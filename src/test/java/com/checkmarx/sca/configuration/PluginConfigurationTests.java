package com.checkmarx.sca.configuration;

import com.checkmarx.sca.TestsInjector;
import com.checkmarx.sca.scan.ArtifactChecker;
import com.google.inject.Guice;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.Properties;

@DisplayName("PluginConfiguration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PluginConfigurationTests {

    private Properties properties;

    @BeforeEach
    public void beforeEach() {
        properties = new Properties();
        properties.setProperty("sca.api.url", "1");
        properties.setProperty("sca.test", "2");
        properties.setProperty("sca.3", "3");
    }

    @DisplayName("Get Property Entries With Success")
    @Test
    public void getPropertyEntriesWithSuccess() {
        var configuration = new PluginConfiguration(properties);

        var entries = configuration.getPropertyEntries();

        Assertions.assertEquals(3, entries.size());
    }

    @DisplayName("Get Property With Success")
    @Test
    public void getPropertyWithSuccess() {
        var configuration = new PluginConfiguration(properties);

        var value = configuration.getProperty(ConfigurationEntry.API_URL);

        Assertions.assertEquals("1", value);
    }

    @DisplayName("Failed to get property returned null")
    @Test
    public void failedToGetPropertyReturnedNull() {
        var configuration = new PluginConfiguration(properties);

        var value = configuration.getProperty(ConfigurationEntry.AUTHENTICATION_URL);

        Assertions.assertNull(value);
    }

    @DisplayName("Get property or default - returned default")
    @Test
    public void getPropertyOrDefaultReturnedDefault() {
        var configuration = new PluginConfiguration(properties);

        var value = configuration.getPropertyOrDefault(ConfigurationEntry.AUTHENTICATION_URL);

        Assertions.assertEquals(ConfigurationEntry.AUTHENTICATION_URL.defaultValue(), value);
    }

    @DisplayName("Get property or default - returned property")
    @Test
    public void getPropertyOrDefaultReturnedProperty() {
        var configuration = new PluginConfiguration(properties);

        var value = configuration.getProperty(ConfigurationEntry.API_URL);

        Assertions.assertEquals("1", value);
    }

    @DisplayName("Validate if expiration time is valid - Is valid")
    @Test
    public void validateExpirationConfigIsValid() {

        var logger = Mockito.mock(Logger.class);
        var artifactChecker = Mockito.mock(ArtifactChecker.class);

        properties.setProperty(ConfigurationEntry.DATA_EXPIRATION_TIME.propertyKey(), "12");
        var configuration = new PluginConfiguration(properties);

        var appInjector = new TestsInjector(logger, artifactChecker, configuration);
        var injector = Guice.createInjector(appInjector);

        configuration = injector.getInstance(PluginConfiguration.class);

        configuration.validate();

        var value = configuration.getProperty(ConfigurationEntry.DATA_EXPIRATION_TIME);

        Assertions.assertEquals("12", value);
    }

    @DisplayName("Validate if expiration time is valid - Not valid")
    @Test
    public void validateExpirationConfigNotValid() {

        var logger = Mockito.mock(Logger.class);
        var artifactChecker = Mockito.mock(ArtifactChecker.class);

        properties.setProperty(ConfigurationEntry.DATA_EXPIRATION_TIME.propertyKey(), "asdsa");
        var configuration = new PluginConfiguration(properties);

        var appInjector = new TestsInjector(logger, artifactChecker, configuration);
        var injector = Guice.createInjector(appInjector);

        configuration = injector.getInstance(PluginConfiguration.class);

        configuration.validate();

        var value = configuration.getProperty(ConfigurationEntry.DATA_EXPIRATION_TIME);

        Assertions.assertEquals(ConfigurationEntry.DATA_EXPIRATION_TIME.defaultValue(), value);
    }
}
