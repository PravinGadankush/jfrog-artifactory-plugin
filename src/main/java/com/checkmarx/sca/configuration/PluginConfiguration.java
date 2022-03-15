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
    @Inject
    private Logger _logger;

    private final Properties properties;

    public PluginConfiguration(@Nonnull Properties properties) {
        this.properties = properties;
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
    }

    private void validateExpirationConfig(){
        var expirationTime = getProperty(ConfigurationEntry.DATA_EXPIRATION_TIME);

        if (expirationTime != null) {
            try {
                Integer.parseInt(expirationTime);
            } catch (Exception ex) {
                _logger.warn(format("Error converting the 'sca.data.expiration-time' configuration value, we will use the default value. Exception Message: %s.", ex.getMessage()));
                properties.setProperty(ConfigurationEntry.DATA_EXPIRATION_TIME.propertyKey(), ConfigurationEntry.DATA_EXPIRATION_TIME.defaultValue());
            }
        }
    }
}
