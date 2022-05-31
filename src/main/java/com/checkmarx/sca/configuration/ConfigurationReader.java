package com.checkmarx.sca.configuration;

import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static java.lang.String.format;

public final class ConfigurationReader {
    private static final String CONFIGURATIONS_FILE = "cxsca-security-plugin.properties";

    public static PluginConfiguration loadConfiguration(@Nonnull File pluginsDirectory, @Nonnull Logger logger) throws IOException {
        if (!pluginsDirectory.exists()) {
            throw new IOException(format("Directory '%s' not found", pluginsDirectory.getAbsolutePath()));
        }

        File propertyFile = new File(pluginsDirectory, CONFIGURATIONS_FILE);
        if (!propertyFile.exists()) {
            throw new IOException(format("File '%s' not found", propertyFile.getAbsolutePath()));
        }

        Properties configuration = new Properties();
        try (FileInputStream fis = new FileInputStream(propertyFile)) {
            configuration.load(fis);
        }

        return new PluginConfiguration(configuration, logger);
    }
}
