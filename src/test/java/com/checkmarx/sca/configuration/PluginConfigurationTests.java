package com.checkmarx.sca.configuration;

import com.checkmarx.sca.TestsInjector;
import com.checkmarx.sca.scan.ArtifactRisksFiller;
import com.checkmarx.sca.scan.SecurityThresholdChecker;
import com.google.inject.Guice;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.Properties;

@DisplayName("PluginConfiguration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PluginConfigurationTests {

    private Properties properties;
    private Logger _logger;

    @BeforeEach
    public void beforeEach() {
        properties = new Properties();
        properties.setProperty("sca.api.url", "1");
        properties.setProperty("sca.test", "2");
        properties.setProperty("sca.3", "3");

        _logger = Mockito.mock(Logger.class);
    }

    @DisplayName("Get Property Entries With Success")
    @Test
    public void getPropertyEntriesWithSuccess() {
        var configuration = new PluginConfiguration(properties, _logger);

        var entries = configuration.getPropertyEntries();

        Assertions.assertEquals(3, entries.size());
    }

    @DisplayName("Get Property With Success")
    @Test
    public void getPropertyWithSuccess() {
        var configuration = new PluginConfiguration(properties, _logger);

        var value = configuration.getProperty(ConfigurationEntry.API_URL);

        Assertions.assertEquals("1", value);
    }

    @DisplayName("Failed to get property returned null")
    @Test
    public void failedToGetPropertyReturnedNull() {
        var configuration = new PluginConfiguration(properties, _logger);

        var value = configuration.getProperty(ConfigurationEntry.AUTHENTICATION_URL);

        Assertions.assertNull(value);
    }

    @DisplayName("Get property or default - returned default")
    @Test
    public void getPropertyOrDefaultReturnedDefault() {
        var configuration = new PluginConfiguration(properties, _logger);

        var value = configuration.getPropertyOrDefault(ConfigurationEntry.AUTHENTICATION_URL);

        Assertions.assertEquals(ConfigurationEntry.AUTHENTICATION_URL.defaultValue(), value);
    }

    @DisplayName("Get property or default - returned property")
    @Test
    public void getPropertyOrDefaultReturnedProperty() {
        var configuration = new PluginConfiguration(properties, _logger);

        var value = configuration.getProperty(ConfigurationEntry.API_URL);

        Assertions.assertEquals("1", value);
    }

    @DisplayName("Validate if expiration time is valid - Is valid")
    @Test
    public void validateExpirationConfigIsValid() {

        var artifactFiller = Mockito.mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = Mockito.mock(SecurityThresholdChecker.class);

        properties.setProperty(ConfigurationEntry.DATA_EXPIRATION_TIME.propertyKey(), "1805");
        var configuration = new PluginConfiguration(properties, _logger);

        var appInjector = new TestsInjector(_logger, configuration, artifactFiller, securityThresholdChecker);
        var injector = Guice.createInjector(appInjector);

        configuration = injector.getInstance(PluginConfiguration.class);

        configuration.validate();

        var value = configuration.getProperty(ConfigurationEntry.DATA_EXPIRATION_TIME);

        Assertions.assertEquals("1805", value);
    }

    @DisplayName("Validate if expiration time is valid - Not valid")
    @Test
    public void validateExpirationConfigNotValid() {

        var artifactFiller = Mockito.mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = Mockito.mock(SecurityThresholdChecker.class);

        properties.setProperty(ConfigurationEntry.DATA_EXPIRATION_TIME.propertyKey(), "asdsa");
        var configuration = new PluginConfiguration(properties, _logger);

        var appInjector = new TestsInjector(_logger, configuration, artifactFiller, securityThresholdChecker);
        var injector = Guice.createInjector(appInjector);

        configuration = injector.getInstance(PluginConfiguration.class);

        configuration.validate();

        var value = configuration.getProperty(ConfigurationEntry.DATA_EXPIRATION_TIME);

        Assertions.assertEquals(ConfigurationEntry.DATA_EXPIRATION_TIME.defaultValue(), value);
    }

    @DisplayName("Validate if expiration time is valid - Changed to minimum value")
    @Test
    public void validateExpirationConfigChangedToMinimumValue() {

        var artifactFiller = Mockito.mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = Mockito.mock(SecurityThresholdChecker.class);

        properties.setProperty(ConfigurationEntry.DATA_EXPIRATION_TIME.propertyKey(), "1700");
        var configuration = new PluginConfiguration(properties, _logger);

        var appInjector = new TestsInjector(_logger, configuration, artifactFiller, securityThresholdChecker);
        var injector = Guice.createInjector(appInjector);

        configuration = injector.getInstance(PluginConfiguration.class);

        configuration.validate();

        var value = configuration.getProperty(ConfigurationEntry.DATA_EXPIRATION_TIME);

        Assertions.assertEquals("1800", value);
    }

    @DisplayName("Validate if security risk threshold is valid - Is valid")
    @Test
    public void validateSecurityRiskThresholdConfigIsValid() {

        var artifactFiller = Mockito.mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = Mockito.mock(SecurityThresholdChecker.class);

        properties.setProperty(ConfigurationEntry.SECURITY_RISK_THRESHOLD.propertyKey(), "medium");
        var configuration = new PluginConfiguration(properties, _logger);

        var appInjector = new TestsInjector(_logger, configuration, artifactFiller, securityThresholdChecker);
        var injector = Guice.createInjector(appInjector);

        configuration = injector.getInstance(PluginConfiguration.class);

        configuration.validate();

        var value = configuration.getProperty(ConfigurationEntry.SECURITY_RISK_THRESHOLD);

        Assertions.assertEquals("medium", value);
    }

    @DisplayName("Validate if security risk threshold is valid - Not valid")
    @Test
    public void validateSecurityRiskThresholdConfigNotValid() {

        var artifactFiller = Mockito.mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = Mockito.mock(SecurityThresholdChecker.class);

        properties.setProperty(ConfigurationEntry.SECURITY_RISK_THRESHOLD.propertyKey(), "invalid");

        var appInjector = new TestsInjector(_logger, new PluginConfiguration(properties, _logger), artifactFiller, securityThresholdChecker);

        var injector = Guice.createInjector(appInjector);

        final var configuration = injector.getInstance(PluginConfiguration.class);

        var exception = Assertions.assertThrows(IllegalArgumentException.class, () -> configuration.validate());

        Assertions.assertTrue(exception.getMessage().contains(SecurityRiskThreshold.class.getName()));
    }
}
