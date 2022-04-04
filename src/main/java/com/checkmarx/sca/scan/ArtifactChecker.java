package com.checkmarx.sca.scan;

import com.checkmarx.sca.IPackageManager;
import com.checkmarx.sca.PackageManager;
import com.checkmarx.sca.PropertiesConstants;
import com.checkmarx.sca.communication.ScaHttpClient;
import com.checkmarx.sca.communication.exceptions.UnexpectedResponseCodeException;
import com.checkmarx.sca.configuration.ConfigurationEntry;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.models.ArtifactId;
import com.checkmarx.sca.models.ArtifactInfo;
import com.checkmarx.sca.models.Vulnerability;
import com.google.inject.Inject;
import org.artifactory.repo.*;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.ArrayList;

import static java.lang.String.format;

public class ArtifactChecker {

    @Inject
    private Logger _logger;

    @Inject
    private ScaHttpClient _scaHttpClient;

    @Inject
    private ArtifactIdBuilder _artifactIdBuilder;

    @Inject
    private PluginConfiguration _configuration;

    private final Repositories _repositories;

    public ArtifactChecker(@Nonnull Repositories repositories) {
        _repositories = repositories;
    }

    public void checkArtifact(@Nonnull RepoPath repoPath) {
        var repositoryKey = repoPath.getRepoKey();
        var repoConfiguration = _repositories.getRepositoryConfiguration(repositoryKey);

        var physicalRepoPaths = new ArrayList<RepoPath>();
        if (repoConfiguration instanceof VirtualRepositoryConfiguration) {
            setPhysicalRepoPathsOfVirtualRepository(physicalRepoPaths, repoConfiguration, repoPath.getPath());
        } else {
            physicalRepoPaths.add(repoPath);
        }

        if (physicalRepoPaths.size() == 0) {
            _logger.warn(format("Artifact not found in any repository. Artifact name: %s.", repoPath.getName()));
            return;
        }

        if (scanIsNotNeeded(physicalRepoPaths)) {
            _logger.info(format("Scan ignored by cache configuration. Artifact name: %s", repoPath.getName()));
            return;
        }

        ArtifactId artifactId;
        try {
            var packageType = repoConfiguration.getPackageType();
            var packageManager = PackageManager.GetPackageType(packageType);

            if (FileShouldBeIgnored(repoPath, packageManager)) {
                _logger.debug(format("Not an artifact should be ignored. File Name: %s", repoPath.getName()));
                return;
            }

            var fileLayoutInfo = _repositories.getLayoutInfo(repoPath);
            artifactId = _artifactIdBuilder.getArtifactId(fileLayoutInfo, repoPath, packageManager);

            if (artifactId.isInvalid()) {
                _logger.error(format("The artifact id was not built correctly. PackageType: %s, Name: %s, Version: %s", artifactId.PackageType, artifactId.Name, artifactId.Version));
                return;
            }
        } catch (Exception ex) {
            _logger.error(format("Exception Message: %s. Artifact Name: %s.", ex.getMessage(), repoPath.getName()), ex);
            return;
        }

        _logger.info(format("Started artifact verification. Artifact name: %s", repoPath.getName()));

        setProperties(physicalRepoPaths, artifactId);

        _logger.info(format("Ended the artifact verification. Artifact name: %s", repoPath.getName()));
    }

    private void setPhysicalRepoPathsOfVirtualRepository(ArrayList<RepoPath> physicalRepoPaths, RepositoryConfiguration repoConfiguration, String artifactPath){
        var virtualConfiguration = (VirtualRepositoryConfiguration) repoConfiguration;
        for (var repo : virtualConfiguration.getRepositories()) {
            var repoPathFromVirtual = RepoPathFactory.create(repo, artifactPath);
            if (_repositories.exists(repoPathFromVirtual)) {
                physicalRepoPaths.add(repoPathFromVirtual);
            }
        }
    }


    private boolean scanIsNotNeeded(@Nonnull ArrayList<RepoPath> repoPaths) {
        var expirationTime = getExpirationTime();

        for (var repoPath : repoPaths) {
            if (!scanIsNotNeeded(repoPath, expirationTime)) {
                return false;
            }
        }

        return true;
    }

    private boolean scanIsNotNeeded(@Nonnull RepoPath repoPath, int expirationTime) {
        try {
            if (!_repositories.exists(repoPath)) {
                return false;
            }

            var scanDate = _repositories.getProperty(repoPath, PropertiesConstants.SCAN_DATE);
            if (scanDate == null || scanDate.trim().isEmpty()) {
                return false;
            }

            var instantDate = Instant.parse(scanDate);
            instantDate = instantDate.plusSeconds(expirationTime);

            return Instant.now().compareTo(instantDate) < 0;
        } catch (Exception ex) {
            _logger.error(format("Unexpected error when checking the last scan date for the artifact: %s", repoPath.getName()), ex);
            return false;
        }
    }

    private int getExpirationTime() {
        var configurationTime = _configuration.getPropertyOrDefault(ConfigurationEntry.DATA_EXPIRATION_TIME);

        try {
            return Integer.parseInt(configurationTime);
        } catch (Exception ex) {
            _logger.warn(format("Error converting the 'sca.data.expiration-time' configuration value, we will use the default value. Exception Message: %s.", ex.getMessage()));
        }

        return Integer.parseInt(ConfigurationEntry.DATA_EXPIRATION_TIME.defaultValue());
    }

    private boolean FileShouldBeIgnored(RepoPath repoPath, IPackageManager packageManager) {
        var notNugetPackage = packageManager == PackageManager.NUGET && !repoPath.getPath().endsWith(".nupkg");
        return notNugetPackage;
    }

    private void setProperties(@Nonnull ArrayList<RepoPath> repoPaths, @Nonnull ArtifactId artifactId) {
        ArtifactInfo artifactInfo;
        try {
            artifactInfo = _scaHttpClient.getArtifactInformation(artifactId.PackageType, artifactId.Name, artifactId.Version);
            addArtifactInfoToProperties(repoPaths, artifactInfo);
        } catch (Exception ex) {

            if (ex instanceof UnexpectedResponseCodeException && ((UnexpectedResponseCodeException)ex).StatusCode == 404) {
                _logger.error(format("Artifact not found, artifact name: %s. Exception Message: %s.", artifactId.Name, ex.getMessage()));
                return;
            }

            _logger.error(format("Failed to get artifact information. Exception Message: %s. Artifact Name: %s.", ex.getMessage(), artifactId.Name));
            return;
        }

        try {
            var vulnerabilities = _scaHttpClient.getVulnerabilitiesForArtifact(artifactInfo.getId());
            addArtifactVulnerabilitiesInfo(repoPaths, vulnerabilities);
        } catch (Exception ex) {
            _logger.error(format("Failed to get vulnerabilities of artifact. Exception Message: %s. Artifact Name: %s.", ex.getMessage(), artifactId.Name));
        }
    }

    private void addArtifactInfoToProperties(@Nonnull ArrayList<RepoPath> repoPaths, @Nonnull ArtifactInfo artifactInfo) {
        for (var repoPath : repoPaths) {
            try{
                _repositories.setProperty(repoPath, PropertiesConstants.ID, artifactInfo.getId().getIdentifier());
            } catch (Exception ex) {
                _logger.error(format("Failed to add artifact info to the properties. Exception Message: %s. Artifact Name: %s.", ex.getMessage(), repoPath.getName()));
            }
        }
    }

    private void addArtifactVulnerabilitiesInfo(@Nonnull ArrayList<RepoPath> repoPaths, @Nonnull ArrayList<Vulnerability> vulnerabilities) {
        for (var repoPath : repoPaths) {
            try{
                addArtifactVulnerabilitiesInfo(repoPath, vulnerabilities);
            } catch (Exception ex) {
                _logger.error(format("Failed to add vulnerabilities info to the properties. Exception Message: %s. Artifact Name: %s.", ex.getMessage(), repoPath.getName()));
            }
        }
    }

    private void addArtifactVulnerabilitiesInfo(RepoPath repoPath, ArrayList<Vulnerability> vulnerabilities) {

        var totalNumberOfVulnerabilities = vulnerabilities.size();
        var totalLowScoreVulnerabilities = 0;
        var totalMediumScoreVulnerabilities = 0;
        var totalHighScoreVulnerabilities = 0;
        var vulnerabilityLevel = "None";
        var vulnerabilityScore = 0.0;

        for (Vulnerability vulnerability : vulnerabilities) {

            switch (vulnerability.getSeverity()) {
                case "Low":
                    if (vulnerabilityLevel == "None") {
                        vulnerabilityLevel = vulnerability.getSeverity();
                    }

                    totalLowScoreVulnerabilities++;
                    break;
                case "Medium":
                    if (vulnerabilityLevel == "None" || vulnerabilityLevel == "Low") {
                        vulnerabilityLevel = vulnerability.getSeverity();
                    }

                    totalMediumScoreVulnerabilities++;
                    break;
                case "High":
                    if (vulnerabilityLevel == "None" || vulnerabilityLevel == "Low" || vulnerabilityLevel == "Medium") {
                        vulnerabilityLevel = vulnerability.getSeverity();
                    }

                    totalHighScoreVulnerabilities++;
                    break;
            }

            if (vulnerability.getScore() > vulnerabilityScore) {
                vulnerabilityScore = vulnerability.getScore();
            }
        }

        _repositories.setProperty(repoPath, PropertiesConstants.VULNERABILITIES_COUNT, String.valueOf(totalNumberOfVulnerabilities));
        _repositories.setProperty(repoPath, PropertiesConstants.LOW_VULNERABILITIES_COUNT, String.valueOf(totalLowScoreVulnerabilities));
        _repositories.setProperty(repoPath, PropertiesConstants.MEDIUM_VULNERABILITIES_COUNT, String.valueOf(totalMediumScoreVulnerabilities));
        _repositories.setProperty(repoPath, PropertiesConstants.HIGH_VULNERABILITIES_COUNT, String.valueOf(totalHighScoreVulnerabilities));
        _repositories.setProperty(repoPath, PropertiesConstants.VULNERABILITY_SCORE, String.valueOf(vulnerabilityScore));
        _repositories.setProperty(repoPath, PropertiesConstants.VULNERABILITY_LEVEL, vulnerabilityLevel);
        _repositories.setProperty(repoPath, PropertiesConstants.SCAN_DATE, Instant.now().toString());
    }
}
