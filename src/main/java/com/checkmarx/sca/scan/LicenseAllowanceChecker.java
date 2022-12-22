package com.checkmarx.sca.scan;

import com.checkmarx.sca.configuration.ConfigurationEntry;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.google.inject.Inject;
import org.artifactory.exception.CancelException;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

import static com.checkmarx.sca.PropertiesConstants.*;
import static java.lang.String.format;

public class LicenseAllowanceChecker {
    @Inject
    private Logger _logger;

    @Inject
    private PluginConfiguration _configuration;

    private final Repositories _repositories;

    public LicenseAllowanceChecker(@Nonnull Repositories repositories) {
        _repositories = repositories;
    }

    public void checkLicenseAllowance(@Nonnull RepoPath repoPath, @Nonnull ArrayList<RepoPath> nonVirtualRepoPaths) throws CancelException {

        if (nonVirtualRepoPaths.size() > 1) {
            _logger.warn(format("More than one RepoPath found for the artifact: %s.", repoPath.getName()));
        }

        for (var path : nonVirtualRepoPaths) {

            var ignoreThreshold = getIgnoreProperty(path);

            if ("true".equalsIgnoreCase(ignoreThreshold)) {
                _logger.warn(format("Ignoring the License allowance. Artifact Property \"%s\" is \"true\". Artifact Name: %s", IGNORE_LICENSE, repoPath.getName()));
                return;
            }
        }

        validateLicenseAllowanceFulfillment(nonVirtualRepoPaths.get(0));
    }

    private String getIgnoreProperty(RepoPath path) {
        String ignoreLicense = "false";
        var properties = _repositories.getProperties(path).entries();
        for (var property : properties)
        {
            if (IGNORE_LICENSE.equalsIgnoreCase(property.getKey())) {
                ignoreLicense = property.getValue();
                break;
            }
        }

        return ignoreLicense;
    }

    private void validateLicenseAllowanceFulfillment(RepoPath repoPath) throws CancelException {

        var licenseAllowanceList = getLicenseAllowanceList();
        _logger.debug(format("License allowance configured: [%s]", String.join(", ", licenseAllowanceList)));

        if(licenseAllowanceList.size()== 0) return;

        if(licenseAllowanceList.size() == 1 && licenseAllowanceList.toArray()[0].toString().equalsIgnoreCase("none")){
            throw new CancelException(getCancelExceptionMessage(repoPath), 403);
        }

        var licenses = List.of(_repositories.getProperty(repoPath, LICENSE_NAMES).split(","));

        if (licenseAllowanceList.stream().noneMatch(licenses::contains)) {
            throw new CancelException(getCancelExceptionMessage(repoPath), 403);
        }
    }

    private Set<String> getLicenseAllowanceList() {
        var allowance = _configuration.getPropertyOrDefault(ConfigurationEntry.LICENSES_ALLOWED);

        if (allowance != null) {
            try {
                var licenses = allowance.split(",");
                return Arrays.stream(licenses).filter(name -> !name.isBlank())
                        .map(String::trim).collect(Collectors.toSet());
            } catch (Exception ex) {
                _logger.warn(format("License allowance not configured: %s", allowance));
                throw ex;
            }
        }
        return new HashSet<>();
    }

    private String getCancelExceptionMessage(RepoPath repoPath) {
        return format("License allowance not compliant for the artifact: %s", repoPath.getName());
    }
}
