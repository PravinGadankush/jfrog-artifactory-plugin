package com.checkmarx.sca.scan;

import com.checkmarx.sca.configuration.ConfigurationEntry;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.configuration.SecurityRiskThreshold;
import com.google.inject.Inject;
import org.artifactory.exception.CancelException;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;

import static com.checkmarx.sca.PropertiesConstants.*;
import static java.lang.String.format;

public class SecurityThresholdChecker {

    @Inject
    private Logger _logger;

    @Inject
    private PluginConfiguration _configuration;

    private final Repositories _repositories;

    public SecurityThresholdChecker(@Nonnull Repositories repositories) {
        _repositories = repositories;
    }

    public void checkSecurityRiskThreshold(@Nonnull RepoPath repoPath, @Nonnull ArrayList<RepoPath> nonVirtualRepoPaths) throws CancelException {

        if (nonVirtualRepoPaths.size() > 1) {
            _logger.warn(format("More than one RepoPath found for the artifact: %s.", repoPath.getName()));
        }

        for (var path : nonVirtualRepoPaths) {

            var ignoreThreshold = getIgnoreProperty(path);

            if ("true".equalsIgnoreCase(ignoreThreshold)) {
                _logger.warn(format("Ignoring the security risk threshold. Artifact Property \"%s\" is \"true\". Artifact Name: %s", IGNORE_THRESHOLD, repoPath.getName()));
                return;
            }
        }

        validateSecurityRiskThresholdFulfillment(nonVirtualRepoPaths.get(0));
    }

    private String getIgnoreProperty(RepoPath path) {
        String ignoreThreshold = "false";
        var properties = _repositories.getProperties(path).entries();
        for (var property : properties)
        {
            if (IGNORE_THRESHOLD.equalsIgnoreCase(property.getKey())) {
                ignoreThreshold = property.getValue();
                break;
            }
        }

        return ignoreThreshold;
    }

    private void validateSecurityRiskThresholdFulfillment(RepoPath repoPath) throws CancelException {

        var securityRiskThreshold = getSecurityRiskThreshold();
        _logger.debug(format("Security risk threshold configured: %s", securityRiskThreshold));

        switch (securityRiskThreshold) {
            case LOW:
                checkIfLowRiskThresholdFulfillment(repoPath);
                break;
            case MEDIUM:
                checkIfMediumRiskThresholdFulfillment(repoPath);
                break;
            case HIGH:
                checkIfHighRiskThresholdFulfillment(repoPath);
                break;
            case CRITICAL:
                checkIfCriticalRiskThresholdFulfillment(repoPath);
                break;
        }
    }

    private SecurityRiskThreshold getSecurityRiskThreshold() {
        var configuration = _configuration.getPropertyOrDefault(ConfigurationEntry.SECURITY_RISK_THRESHOLD);
        return SecurityRiskThreshold.valueOf(configuration.trim().toUpperCase());
    }

    private void checkIfLowRiskThresholdFulfillment(RepoPath repoPath) throws CancelException {
        var vulnerabilities = _repositories.getProperty(repoPath, TOTAL_RISKS_COUNT);

        if (Integer.parseInt(vulnerabilities) > 0) {
            throw new CancelException(getCancelExceptionMessage(repoPath), 403);
        }
    }

    private void checkIfMediumRiskThresholdFulfillment(RepoPath repoPath) throws CancelException {
        var mediumRisk = _repositories.getProperty(repoPath, MEDIUM_RISKS_COUNT);
        var highRisk = _repositories.getProperty(repoPath, HIGH_RISKS_COUNT);
        var criticalRisk = _repositories.getProperty(repoPath, CRITICAL_RISKS_COUNT);

        if (Integer.parseInt(mediumRisk) > 0 || Integer.parseInt(highRisk) > 0 || Integer.parseInt(criticalRisk) > 0) {
            throw new CancelException(getCancelExceptionMessage(repoPath), 403);
        }
    }

    private void checkIfHighRiskThresholdFulfillment(RepoPath repoPath) throws CancelException {
        var highRisk = _repositories.getProperty(repoPath, HIGH_RISKS_COUNT);
        var criticalRisk = _repositories.getProperty(repoPath, CRITICAL_RISKS_COUNT);

        if (Integer.parseInt(highRisk) > 0 || Integer.parseInt(criticalRisk) > 0) {
            throw new CancelException(getCancelExceptionMessage(repoPath), 403);
        }
    }

    private void checkIfCriticalRiskThresholdFulfillment(RepoPath repoPath) throws CancelException {
        var criticalRisk = _repositories.getProperty(repoPath, CRITICAL_RISKS_COUNT);

        if (Integer.parseInt(criticalRisk) > 0) {
            throw new CancelException(getCancelExceptionMessage(repoPath), 403);
        }
    }

    private String getCancelExceptionMessage(RepoPath repoPath) {
        return format("Artifact has risks that do not comply with the security risk threshold. Artifact Name: %s", repoPath.getName());
    }
}
