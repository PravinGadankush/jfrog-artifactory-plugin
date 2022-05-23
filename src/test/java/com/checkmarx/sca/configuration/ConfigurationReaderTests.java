package com.checkmarx.sca.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;

@DisplayName("ConfigurationReader")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ConfigurationReaderTests {

    @DisplayName("Load Configuration With Success")
    @Test
    public void loadConfigurationWithSuccess() throws IOException {
        String path = "src/test/resources/com/checkmarx/sca/configuration";
        File resourcesDir = new File(path);

        var logger = Mockito.mock(Logger.class);

        var config = ConfigurationReader.loadConfiguration(resourcesDir, logger);

        Assertions.assertNotNull(config);
    }

    @DisplayName("Failed to load configuration - Directory not found")
    @Test
    public void FailedToLoadConfigurationDirectoryNotFound() {
        var dir = "test-dir";
        File resourcesDir = new File(dir);
        var logger = Mockito.mock(Logger.class);

        var exception = Assertions.assertThrows(IOException.class, () ->  ConfigurationReader.loadConfiguration(resourcesDir, logger));

        Assertions.assertTrue(exception.getMessage().contains("Directory"));
        Assertions.assertTrue(exception.getMessage().contains(dir));
    }

    @DisplayName("Failed to load configuration - File not found")
    @Test
    public void FailedToLoadConfigurationFileNotFound() {
        var path = "src/test/resources/com/checkmarx/sca";
        File resourcesDir = new File(path);
        var logger = Mockito.mock(Logger.class);

        var exception = Assertions.assertThrows(IOException.class, () ->  ConfigurationReader.loadConfiguration(resourcesDir, logger));

        Assertions.assertTrue(exception.getMessage().contains("File"));
    }
}
