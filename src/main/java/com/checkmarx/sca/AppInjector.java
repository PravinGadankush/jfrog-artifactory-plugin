package com.checkmarx.sca;

import com.checkmarx.sca.communication.AccessControlClient;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.scan.ArtifactChecker;
import com.google.inject.AbstractModule;
import org.slf4j.Logger;

import javax.annotation.Nonnull;

public class AppInjector extends AbstractModule {

    private final Logger _logger;
    private final ArtifactChecker _artifactChecker;
    private final AccessControlClient _accessControlClient;

    private final PluginConfiguration _configuration;

    public AppInjector(@Nonnull Logger logger,
                       @Nonnull ArtifactChecker artifactChecker,
                       @Nonnull PluginConfiguration configuration) {
        _logger = logger;
        _configuration = configuration;
        _artifactChecker = artifactChecker;
        _accessControlClient = null;
    }

    public AppInjector(@Nonnull Logger logger,
                       @Nonnull ArtifactChecker artifactChecker,
                       @Nonnull PluginConfiguration configuration,
                       @Nonnull AccessControlClient accessControlClient) {
        _logger = logger;
        _configuration = configuration;
        _artifactChecker = artifactChecker;
        _accessControlClient = accessControlClient;
    }

    @Override
    protected void configure() {
        bind(Logger.class).toInstance(_logger);
        bind(ArtifactChecker.class).toInstance(_artifactChecker);
        bind(PluginConfiguration.class).toInstance(_configuration);

        if (_accessControlClient != null) {
            bind(AccessControlClient.class).toInstance(_accessControlClient);
        }
    }
}
