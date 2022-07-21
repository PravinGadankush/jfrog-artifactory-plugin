package com.checkmarx.sca.configuration;

import com.checkmarx.sca.TestsInjector;
import com.checkmarx.sca.scan.ArtifactRisksFiller;
import com.checkmarx.sca.scan.SecurityThresholdChecker;
import com.google.inject.Guice;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;

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

        var exception = Assertions.assertThrows(IllegalArgumentException.class, configuration::validate);

        Assertions.assertTrue(exception.getMessage().contains(SecurityRiskThreshold.class.getName()));
    }

    @DisplayName("Validate if authentication configuration is valid - Is valid")
    @Test
    public void validateAuthConfigIsValid() {

        var artifactFiller = Mockito.mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = Mockito.mock(SecurityThresholdChecker.class);

        properties.setProperty(ConfigurationEntry.ACCOUNT.propertyKey(), "account");
        properties.setProperty(ConfigurationEntry.USERNAME.propertyKey(), "username");
        properties.setProperty(ConfigurationEntry.PASSWORD.propertyKey(), "password");

        var configuration = new PluginConfiguration(properties, _logger);

        var appInjector = new TestsInjector(_logger, configuration, artifactFiller, securityThresholdChecker);
        var injector = Guice.createInjector(appInjector);

        configuration = injector.getInstance(PluginConfiguration.class);

        configuration.validate();

        var account = configuration.getProperty(ConfigurationEntry.ACCOUNT);
        var username = configuration.getProperty(ConfigurationEntry.USERNAME);
        var password = configuration.getProperty(ConfigurationEntry.PASSWORD);

        Assertions.assertEquals("account", account);
        Assertions.assertEquals("username", username);
        Assertions.assertEquals("password", password);
        Assertions.assertTrue(configuration.hasAuthConfiguration());
    }

    @DisplayName("Validate if authentication configuration is not defined - Is not defined")
    @Test
    public void validateAuthConfigIsNotDefined() {

        var artifactFiller = Mockito.mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = Mockito.mock(SecurityThresholdChecker.class);

        var configuration = new PluginConfiguration(properties, _logger);

        var appInjector = new TestsInjector(_logger, configuration, artifactFiller, securityThresholdChecker);
        var injector = Guice.createInjector(appInjector);

        configuration = injector.getInstance(PluginConfiguration.class);

        configuration.validate();

        Mockito.verify(_logger, times(0)).error(isA(String.class));
        Mockito.verify(_logger, times(0)).info(isA(String.class));

        Assertions.assertFalse(configuration.hasAuthConfiguration());
    }

    @DisplayName("Validate if authentication configuration is invalid - Is invalid")
    @Test
    public void validateAuthConfigIsInvalid() {

        var artifactFiller = Mockito.mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = Mockito.mock(SecurityThresholdChecker.class);

        properties.setProperty(ConfigurationEntry.ACCOUNT.propertyKey(), "account");
        properties.setProperty(ConfigurationEntry.USERNAME.propertyKey(), "username");

        var configuration = new PluginConfiguration(properties, _logger);

        var appInjector = new TestsInjector(_logger, configuration, artifactFiller, securityThresholdChecker);
        var injector = Guice.createInjector(appInjector);

        configuration = injector.getInstance(PluginConfiguration.class);

        configuration.validate();

        Assertions.assertFalse(configuration.hasAuthConfiguration());
        Mockito.verify(_logger, times(1)).error(isA(String.class));
        Mockito.verify(_logger, times(1)).info(isA(String.class));
    }

    @DisplayName("Validate if able to get access control credentials - succeed")
    @Test
    public void validateGetAccessControlCredentials() {

        var artifactFiller = Mockito.mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = Mockito.mock(SecurityThresholdChecker.class);

        properties.setProperty(ConfigurationEntry.ACCOUNT.propertyKey(), "account");
        properties.setProperty(ConfigurationEntry.USERNAME.propertyKey(), "username");
        properties.setProperty(ConfigurationEntry.PASSWORD.propertyKey(), "password");

        var configuration = new PluginConfiguration(properties, _logger);

        var appInjector = new TestsInjector(_logger, configuration, artifactFiller, securityThresholdChecker);
        var injector = Guice.createInjector(appInjector);

        configuration = injector.getInstance(PluginConfiguration.class);

        var accessControlCredentials = configuration.getAccessControlCredentials();

        Assertions.assertEquals("account", accessControlCredentials.getTenant());
        Assertions.assertEquals("username", accessControlCredentials.getUsername());
        Assertions.assertEquals("password", accessControlCredentials.getPassword());
    }

    @DisplayName("Validate if able to get access control credentials - failed")
    @Test
    public void failedToGetAccessControlCredentials() {

        var artifactFiller = Mockito.mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = Mockito.mock(SecurityThresholdChecker.class);

        properties.setProperty(ConfigurationEntry.ACCOUNT.propertyKey(), "account");
        properties.setProperty(ConfigurationEntry.USERNAME.propertyKey(), "username");

        var configuration = new PluginConfiguration(properties, _logger);

        var appInjector = new TestsInjector(_logger, configuration, artifactFiller, securityThresholdChecker);
        var injector = Guice.createInjector(appInjector);

        configuration = injector.getInstance(PluginConfiguration.class);

        Assertions.assertThrows(NullPointerException.class, configuration::getAccessControlCredentials);

        Mockito.verify(_logger, times(1)).error(isA(String.class));
        Mockito.verify(_logger, times(1)).error(any(), isA(Exception.class));
    }
}
