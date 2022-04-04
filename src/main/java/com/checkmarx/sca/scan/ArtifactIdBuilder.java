package com.checkmarx.sca.scan;

import com.checkmarx.sca.IPackageManager;
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

    public ArtifactId getArtifactId(@Nonnull FileLayoutInfo fileLayoutInfo, @Nonnull RepoPath repoPath, @Nonnull PackageManager packageManager){
        var revision = fileLayoutInfo.getBaseRevision();
        var name = fileLayoutInfo.getModule();

        if (packageManager == PackageManager.NOTSUPPORTED) {
            return new ArtifactId(packageManager.packageType(), name, revision);
        }

        if (fileLayoutInfo.isValid()) {
            return getArtifactIdOfValidLayout(fileLayoutInfo, packageManager, name, revision);
        }

        return tryToUseRegex(repoPath, packageManager);
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
            switch (packageManager) {
                case PYPI:
                    return pythonParse(repoPath, packageManager);
                case NUGET:
                    return nugetParse(repoPath, packageManager);
                default:
                    _logger.info(format("Trying to parse RepoPath through regex but packageType is not supported. PackageType: %s, Artifact Name: %s", packageManager.packageType(), repoPath.getName()));
                    _logger.debug(format("Path not supported by regex. Artifact path: %s", repoPath.getPath()));
            }
        } catch (Exception ex) {
            _logger.error(format("There was a problem trying to use a Regex to parse the artifact path. Artifact path: %s", repoPath.getPath()));
            _logger.debug("Exception", ex);
        }

        return new ArtifactId(packageManager.packageType(), null, null);
    }

    public ArtifactId pythonParse(RepoPath repoPath, PackageManager packageManager) {
        Pattern pattern = Pattern.compile(".+/(?<name>.+)-(?<version>\\d+(?:\\.[A-Za-z0-9]+)*).*\\.(?:whl|egg|zip|tar\\.gz)");
        Matcher matcher = pattern.matcher(repoPath.getPath());

        if (matcher.matches()) {
            return new ArtifactId(packageManager.packageType(), matcher.group("name"), matcher.group("version"));
        }

        return new ArtifactId(packageManager.packageType(), null, null);
    }

    public ArtifactId nugetParse(RepoPath repoPath, IPackageManager packageManager) {
        Pattern pattern = Pattern.compile("(?<name>.*?)\\.(?<version>(?:\\.?[0-9]+){3,}(?:[-a-z]+)?)\\.nupkg");
        Matcher matcher = pattern.matcher(repoPath.getPath());

        if (matcher.matches()) {
            return new ArtifactId(packageManager.packageType(), matcher.group("name"), matcher.group("version"));
        }

        return new ArtifactId(packageManager.packageType(), null, null);
    }
}
