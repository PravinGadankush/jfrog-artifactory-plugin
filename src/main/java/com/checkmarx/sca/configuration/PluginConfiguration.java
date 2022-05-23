package com.checkmarx.sca.configuration;

import com.google.inject.Inject;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static java.lang.String.format;

public class PluginConfiguration {

    private final Logger logger;
    private final Properties properties;

    public PluginConfiguration(@Nonnull Properties properties, @Nonnull Logger logger) {
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

    public void validate() {
        validateExpirationConfig();
        validateSeverityThresholdConfig();
    }

    private void validateExpirationConfig(){
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

    private void validateSeverityThresholdConfig(){
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
}
