package com.checkmarx.sca.scan;

import com.checkmarx.sca.PackageManager;
import com.checkmarx.sca.models.ArtifactId;
import com.google.inject.Inject;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
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

        if (fileLayoutInfo.isValid() && packageManager != PackageManager.NPM) {
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
        if (packageManager.packageType().equalsIgnoreCase(PackageManager.MAVEN.packageType())) {
            var organization = fileLayoutInfo.getOrganization();
            var fileIntegrationRevision = fileLayoutInfo.getFileIntegrationRevision();

            name = String.format("%s:%s", organization, name);

            if (fileIntegrationRevision != null) {
                revision = String.format("%s-%s", revision, fileIntegrationRevision);
            }
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
                default:
                    _logger.info(format("Trying to parse RepoPath through regex but packageType is not supported. PackageType: %s, Artifact Name: %s", packageManager.packageType(), repoPath.getName()));
                    _logger.debug(format("Path not supported by regex. Artifact path: %s", repoPath.getPath()));
                    return new ArtifactId(packageManager.packageType(), null, null);
            }

            return parseRepoPath(repoPath, packageManager, regex);
        } catch (Exception ex) {
            _logger.error(format("There was a problem trying to use a Regex to parse the artifact path. Artifact path: %s", repoPath.getPath()));
            _logger.debug("Exception", ex);
            return new ArtifactId(packageManager.packageType(), null, null);
        }
    }

    public ArtifactId parseRepoPath(RepoPath repoPath, PackageManager packageManager, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(repoPath.getPath());

        if (matcher.matches()) {
            return new ArtifactId(packageManager.packageType(), matcher.group("name"), matcher.group("version"));
        }

        return new ArtifactId(packageManager.packageType(), null, null);
    }
}
