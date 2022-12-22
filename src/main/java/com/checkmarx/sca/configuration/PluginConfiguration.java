package com.checkmarx.sca.configuration;

import com.checkmarx.sca.communication.models.AccessControlCredentials;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.*;

import static java.lang.String.format;

public class PluginConfiguration {
    private final Logger logger;
    private final Properties properties;

    private boolean hasAuthConfiguration;

    public PluginConfiguration(@Nonnull Properties properties, @Nonnull Logger logger) {
        this.hasAuthConfiguration = false;
        this.properties = properties;
        this.logger = logger;
    }

    public Set<Map.Entry<Object, Object>> getPropertyEntries() {
        return new HashSet<>(properties.entrySet());
    }

    public String getProperty(IConfigurationEntry config) {
        return properties.getProperty(config.propertyKey());
    }

    public String getPropertyOrDefault(IConfigurationEntry config) {
        return properties.getProperty(config.propertyKey(), config.defaultValue());
    }

    public boolean hasAuthConfiguration() {
        return this.hasAuthConfiguration;
    }

    public AccessControlCredentials getAccessControlCredentials() {
        try {
            var account = this.getPropertyOrDefault(ConfigurationEntry.ACCOUNT);
            var username = this.getPropertyOrDefault(ConfigurationEntry.USERNAME);
            var password = this.getPropertyOrDefault(ConfigurationEntry.PASSWORD);

            var envValue = System.getenv(password);
            if (envValue != null) {
                password = envValue;
            }

            return new AccessControlCredentials(username, password, account);
        } catch (Exception ex) {
            logger.error("Failed to get access control credentials.");
            logger.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    public void validate() {
        validateAuthConfig();
        validateExpirationConfig();
        validateSeverityThresholdConfig();
        validateLicensesAllowedConfig();
    }

    private void validateExpirationConfig() {
        var expirationTime = getProperty(ConfigurationEntry.DATA_EXPIRATION_TIME);

        if (expirationTime != null) {
            try {
                var definedValue = Integer.parseInt(expirationTime);

                int minimumExpirationTime = 1800;
                if (definedValue < minimumExpirationTime) {
                    properties.setProperty(ConfigurationEntry.DATA_EXPIRATION_TIME.propertyKey(), String.valueOf(minimumExpirationTime));
                    this.logger.warn("The configuration value defined for the property 'sca.data.expiration-time' is lower than the minimum value allowed. The minimum value will be used.");
                }
            } catch (Exception ex) {
                this.logger.warn(format("Error converting the 'sca.data.expiration-time' configuration value, the default value will be used. Exception Message: %s.", ex.getMessage()));
                properties.setProperty(ConfigurationEntry.DATA_EXPIRATION_TIME.propertyKey(), ConfigurationEntry.DATA_EXPIRATION_TIME.defaultValue());
            }
        }
    }

    private void validateSeverityThresholdConfig() {
        var threshold = getProperty(ConfigurationEntry.SECURITY_RISK_THRESHOLD);

        if (threshold != null) {
            try {
                SecurityRiskThreshold.valueOf(threshold.trim().toUpperCase());
            } catch (Exception ex) {
                this.logger.error(format("Error converting the 'sca.security.risk.threshold' configuration value, we will use the default value (LOW). Exception Message: %s.", ex.getMessage()));
                throw ex;
            }
        }
    }

    private void validateLicensesAllowedConfig() {
        var allowance = getProperty(ConfigurationEntry.LICENSES_ALLOWED);

        if (allowance != null) {
            try {
                var licenses = allowance.split(",");
                var validLicenses = Arrays.stream(licenses).filter(license-> !license.isEmpty()).distinct();
            } catch (Exception ex) {
                this.logger.error(format("Error converting '%s' configuration value, no license restrictions applied. Exception Message: %s.",
                        ConfigurationEntry.LICENSES_ALLOWED.propertyKey(), ex.getMessage()));
                throw ex;
            }
        }
    }

    private void validateAuthConfig() {
        var account = this.getPropertyOrDefault(ConfigurationEntry.ACCOUNT);
        var username = this.getPropertyOrDefault(ConfigurationEntry.USERNAME);
        var password = this.getPropertyOrDefault(ConfigurationEntry.PASSWORD);

        if (Objects.equals(account, null)
                && Objects.equals(username, null)
                && Objects.equals(password, null)) {
            return;
        }

        var missingFields = new ArrayList<String>();
        if (Objects.equals(account, null)) {
            missingFields.add(ConfigurationEntry.ACCOUNT.propertyKey());
        }

        if (Objects.equals(username, null)) {
            missingFields.add(ConfigurationEntry.USERNAME.propertyKey());
        }

        if (Objects.equals(password, null)) {
            missingFields.add(ConfigurationEntry.PASSWORD.propertyKey());
        }

        if (missingFields.isEmpty()) {
            this.hasAuthConfiguration = true;
            return;
        }

        var message = format("A mandatory authentication configuration is missing. (Missing configurations: %s)", String.join(", ", missingFields));
        this.logger.error(message);
        this.logger.info("Working without authentication.");
    }
}
