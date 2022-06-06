package com.checkmarx.sca.scan;

import com.checkmarx.sca.PackageManager;
import com.checkmarx.sca.models.ArtifactId;
import com.google.inject.Inject;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class ArtifactIdBuilder {
    @Inject
    private Logger _logger;

    @Inject
    private ComposerArtifactIdBuilder _composerArtifactIdBuilder;

    public ArtifactId getArtifactId(@Nonnull FileLayoutInfo fileLayoutInfo, @Nonnull RepoPath repoPath, @Nonnull PackageManager packageManager){
        var revision = fileLayoutInfo.getBaseRevision();
        var name = fileLayoutInfo.getModule();

        var validFileLayouts = List.of(PackageManager.MAVEN.key(), PackageManager.GRADLE.key());

        if (validFileLayouts.contains(packageManager.key())) {
            return getArtifactIdOfValidLayout(fileLayoutInfo, packageManager, name, revision);
        }

        switch (packageManager) {
            case NOTSUPPORTED:
                return new ArtifactId(packageManager.packageType(), name, revision);
            case COMPOSER:
                return _composerArtifactIdBuilder.generateArtifactId(repoPath, packageManager);
            default:
                return tryToUseRegex(repoPath, packageManager);
        }
    }

    private ArtifactId getArtifactIdOfValidLayout(FileLayoutInfo fileLayoutInfo, PackageManager packageManager, String name, String revision) {
        var organization = fileLayoutInfo.getOrganization();
        var fileIntegrationRevision = fileLayoutInfo.getFileIntegrationRevision();

        name = String.format("%s:%s", organization, name);

        if (fileIntegrationRevision != null) {
            revision = String.format("%s-%s", revision, fileIntegrationRevision);
        }

        return new ArtifactId(packageManager.packageType(), name, revision);
    }

    private ArtifactId tryToUseRegex(RepoPath repoPath, PackageManager packageManager) {
        try{
            String regex;
            switch (packageManager) {
                case NPM:
                    regex = "(?<name>.+)\\/-\\/.+-(?<version>\\d+\\.\\d+\\.\\d+.*)\\.tgz";
                    break;
                case PYPI:
                    regex = ".+/(?<name>.+)-(?<version>\\d+(?:\\.[A-Za-z0-9]+)*).*\\.(?:whl|egg|zip|tar\\.gz)";
                    break;
                case NUGET:
                    regex = "(?<name>.*?)\\.(?<version>(?:\\.?[0-9]+){3,}(?:[-a-z]+)?)\\.nupkg";
                    break;
                case BOWER:
                    regex = ".*/(?<name>.+)-v?(?<version>\\d(?:\\.[A-Za-z0-9]+)*).*tar\\.gz";
                    break;
                case IVY:
                case SBT:
                    return parseMavenRepoPath(repoPath, packageManager);
                case GO:
                    var path = repoPath.getPath();

                    path = path.replaceAll("(\\+incompatible)?(\\.mod|\\.info|\\.zip)", "");

                    regex = "(?<name>.*?)\\/@v\\/(?<version>.*)";
                    return parseRepoPath(path, packageManager, regex);
                default:
                    _logger.info(format("Trying to parse RepoPath through regex but packageType is not supported. PackageType: %s, Artifact Name: %s", packageManager.packageType(), repoPath.getName()));
                    _logger.debug(format("Path not supported by regex. Artifact path: %s", repoPath.getPath()));
                    return new ArtifactId(packageManager.packageType(), null, null);
            }

            return parseRepoPath(repoPath.getPath(), packageManager, regex);
        } catch (Exception ex) {
            _logger.error(format("There was a problem trying to use a Regex to parse the artifact path. Artifact path: %s", repoPath.getPath()));
            _logger.debug("Exception", ex);
            return new ArtifactId(packageManager.packageType(), null, null);
        }
    }

    public ArtifactId parseRepoPath(String path, PackageManager packageManager, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(path);

        if (matcher.matches()) {
            var name = matcher.group("name");
            var version = matcher.group("version");
            LogPackageDebug(path, packageManager, name, version);
            return new ArtifactId(packageManager.packageType(), name, version);
        }

        return new ArtifactId(packageManager.packageType(), null, null);
    }

    private ArtifactId parseMavenRepoPath(RepoPath repoPath, PackageManager packageManager){
        var pattern = Pattern.compile("(?<packagePath>.+)/(?<version>\\d+(?:\\.[A-Za-z0-9]+)*).*");
        var matcher = pattern.matcher(repoPath.getPath());

        if(matcher.matches()){
            var packagePath = matcher.group("packagePath");
            var version = matcher.group("version");

            var packagePathArray = packagePath.split("/");
            var organisation = String.join(".", Arrays.copyOfRange(packagePathArray, 0, packagePathArray.length - 1));
            var packageName = packagePathArray[packagePathArray.length - 1];

            var name = format("%s:%s", organisation, packageName);

            LogPackageDebug(repoPath.getPath(), packageManager, name, version);

            return new ArtifactId(packageManager.packageType(), name, version);
       }

        return new ArtifactId(packageManager.packageType(), null, null);
    }

    private void LogPackageDebug(String repoPath, PackageManager packageManager, String name, String version) {
        _logger.debug(format("PackageManager: %s", packageManager.key()));
        _logger.debug(format("RepoPath: %s", repoPath));
        _logger.debug(format("Parsed name: %s", name));
        _logger.debug(format("Parsed version: %s", version));
    }
}
