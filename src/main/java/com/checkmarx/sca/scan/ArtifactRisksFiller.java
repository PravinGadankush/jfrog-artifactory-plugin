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
import com.checkmarx.sca.models.PackageRiskAggregation;
import com.google.inject.Inject;
import org.artifactory.md.Properties;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.ArrayList;

import static java.lang.String.format;

public class ArtifactRisksFiller {

    @Inject
    private Logger _logger;

    @Inject
    private ScaHttpClient _scaHttpClient;

    @Inject
    private ArtifactIdBuilder _artifactIdBuilder;

    @Inject
    private PluginConfiguration _configuration;

    private final Repositories _repositories;

    public ArtifactRisksFiller(@Nonnull Repositories repositories) {
        _repositories = repositories;
    }

    public boolean addArtifactRisks(@Nonnull RepoPath repoPath, @Nonnull ArrayList<RepoPath> nonVirtualRepoPaths) {
        var repositoryKey = repoPath.getRepoKey();
        var repoConfiguration = _repositories.getRepositoryConfiguration(repositoryKey);

        if (nonVirtualRepoPaths.size() == 0) {
            _logger.warn(format("Artifact not found in any repository. Artifact name: %s.", repoPath.getName()));
            return false;
        }

        if (scanIsNotNeeded(nonVirtualRepoPaths)) {
            _logger.info(format("Scan ignored by cache configuration. Artifact name: %s", repoPath.getName()));
            return true;
        }

        ArtifactId artifactId;
        try {
            var packageType = repoConfiguration.getPackageType();
            var packageManager = PackageManager.GetPackageType(packageType);

            if (FileShouldBeIgnored(repoPath, packageManager)) {
                _logger.debug(format("Not an artifact should be ignored. File Name: %s", repoPath.getName()));
                return false;
            }

            var fileLayoutInfo = _repositories.getLayoutInfo(repoPath);
            artifactId = _artifactIdBuilder.getArtifactId(fileLayoutInfo, repoPath, packageManager);

            if (artifactId.isInvalid()) {
                _logger.error(format("The artifact id was not built correctly. PackageType: %s, Name: %s, Version: %s", artifactId.PackageType, artifactId.Name, artifactId.Version));
                return false;
            }
        } catch (Exception ex) {
            _logger.error(format("Exception Message: %s. Artifact Name: %s.", ex.getMessage(), repoPath.getName()), ex);
            return false;
        }

        _logger.info(format("Started artifact verification. Artifact name: %s", repoPath.getPath()));

        var packageRiskAggregation = scanArtifact(artifactId);

        var risksAddedSuccessfully = false;
        if (packageRiskAggregation != null) {
            addArtifactRiskInfo(nonVirtualRepoPaths, packageRiskAggregation);
            risksAddedSuccessfully = true;
        }

        _logger.info(format("Ended the artifact verification. Artifact name: %s", repoPath.getPath()));

        return risksAddedSuccessfully;
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

            var properties = _repositories.getProperties(repoPath);
            if (properties == null || !allPropertiesDefined(properties)) {
                _logger.debug(format("There are missing properties, the scan will be performed. Artifact: %s", repoPath.getName()));
                return false;
            }

            var scanDate = properties.getFirst(PropertiesConstants.LAST_SCAN);
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

    private boolean allPropertiesDefined(Properties properties) {
        return properties.containsKey(PropertiesConstants.TOTAL_RISKS_COUNT)
                && properties.containsKey(PropertiesConstants.LOW_RISKS_COUNT)
                && properties.containsKey(PropertiesConstants.MEDIUM_RISKS_COUNT)
                && properties.containsKey(PropertiesConstants.HIGH_RISKS_COUNT)
                && properties.containsKey(PropertiesConstants.RISK_SCORE)
                && properties.containsKey(PropertiesConstants.RISK_LEVEL)
                && properties.containsKey(PropertiesConstants.LAST_SCAN);
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
        var notGoPackage = packageManager == PackageManager.GO && !repoPath.getPath().endsWith(".zip");

        var jsonFile = repoPath.getPath().endsWith(".json");
        var htmlFile = repoPath.getPath().endsWith(".html");
        return notNugetPackage || notGoPackage || jsonFile || htmlFile;
    }

    private PackageRiskAggregation scanArtifact(@Nonnull ArtifactId artifactId) {
        ArtifactInfo artifactInfo;
        try {
            artifactInfo = _scaHttpClient.getArtifactInformation(artifactId.PackageType, artifactId.Name, artifactId.Version);
            _logger.debug(format("For CxSCA the artifact is identified by %s.", artifactInfo.getId().getIdentifier()));
        } catch (Exception ex) {

            if (ex instanceof UnexpectedResponseCodeException && ((UnexpectedResponseCodeException)ex).StatusCode == 404) {
                _logger.error(format("Artifact not found, artifact name: %s. Exception Message: %s.", artifactId.Name, ex.getMessage()));
                return null;
            }

            _logger.error(format("Failed to get artifact information. Exception Message: %s. Artifact Name: %s.", ex.getMessage(), artifactId.Name));
            return null;
        }

        try {
            return _scaHttpClient.getRiskAggregationOfArtifact(artifactInfo.getPackageType(), artifactInfo.getName(), artifactInfo.getVersion());
        } catch (Exception ex) {
            _logger.error(format("Failed to get risk aggregation of artifact. Exception Message: %s. Artifact Name: %s.", ex.getMessage(), artifactId.Name));
            return null;
        }
    }

    private void addArtifactRiskInfo(@Nonnull ArrayList<RepoPath> repoPaths, @Nonnull PackageRiskAggregation packageRiskAggregation) {
        for (var repoPath : repoPaths) {
            try{
                addArtifactRisksInfo(repoPath, packageRiskAggregation);
            } catch (Exception ex) {
                _logger.error(format("Failed to add risks information to the properties. Exception Message: %s. Artifact Name: %s.", ex.getMessage(), repoPath.getName()));
            }
        }
    }

    private void addArtifactRisksInfo(RepoPath repoPath, PackageRiskAggregation packageRiskAggregation) {

        var vulnerabilitiesAggregation = packageRiskAggregation.getVulnerabilitiesAggregation();

        _repositories.setProperty(repoPath, PropertiesConstants.TOTAL_RISKS_COUNT, String.valueOf(vulnerabilitiesAggregation.getVulnerabilitiesCount()));
        _repositories.setProperty(repoPath, PropertiesConstants.LOW_RISKS_COUNT, String.valueOf(vulnerabilitiesAggregation.getLowRiskCount()));
        _repositories.setProperty(repoPath, PropertiesConstants.MEDIUM_RISKS_COUNT, String.valueOf(vulnerabilitiesAggregation.getMediumRiskCount()));
        _repositories.setProperty(repoPath, PropertiesConstants.HIGH_RISKS_COUNT, String.valueOf(vulnerabilitiesAggregation.getHighRiskCount()));
        _repositories.setProperty(repoPath, PropertiesConstants.RISK_SCORE, String.valueOf(vulnerabilitiesAggregation.getMaxRiskScore()));
        _repositories.setProperty(repoPath, PropertiesConstants.RISK_LEVEL, vulnerabilitiesAggregation.getMaxRiskSeverity());
        _repositories.setProperty(repoPath, PropertiesConstants.LAST_SCAN, Instant.now().toString());
    }
}
