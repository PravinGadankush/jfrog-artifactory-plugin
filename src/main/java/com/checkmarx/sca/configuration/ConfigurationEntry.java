package com.checkmarx.sca.configuration;

public enum ConfigurationEntry implements IConfigurationEntry {

    API_URL("sca.api.url", "https://api-sca.checkmarx.net"),
    AUTHENTICATION_URL("sca.authentication.url", "https://platform.checkmarx.net/"),
    DATA_EXPIRATION_TIME("sca.data.expiration-time", "21600"),
    SECURITY_RISK_THRESHOLD("sca.security.risk.threshold", "None"),
    PACKAGIST_REPOSITORY("packagist.repository", "https://packagist.org");

    private final String propertyKey;
    private final String defaultValue;

    ConfigurationEntry(String propertyKey, String defaultValue) {
        this.propertyKey = propertyKey;
        this.defaultValue = defaultValue;
    }

    @Override
    public String propertyKey() {
        return propertyKey;
    }

    @Override
    public String defaultValue() {
        return defaultValue;
    }
}
