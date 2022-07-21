package com.checkmarx.sca;

import com.checkmarx.sca.communication.AccessControlClient;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.scan.ArtifactRisksFiller;
import com.checkmarx.sca.scan.SecurityThresholdChecker;
import com.google.inject.AbstractModule;
import org.slf4j.Logger;

import javax.annotation.Nonnull;

public class AppInjector extends AbstractModule {

    private final Logger _logger;
    private final ArtifactRisksFiller _artifactFiller;
    private final AccessControlClient _accessControlClient;
    private final SecurityThresholdChecker _securityThresholdChecker;

    private final PluginConfiguration _configuration;

    public AppInjector(@Nonnull Logger logger,
                       AccessControlClient accessControlClient,
                       @Nonnull ArtifactRisksFiller artifactFiller,
                       @Nonnull PluginConfiguration configuration,
                       @Nonnull SecurityThresholdChecker securityThresholdChecker) {
        _logger = logger;
        _configuration = configuration;
        _artifactFiller = artifactFiller;
        _accessControlClient = accessControlClient;
        _securityThresholdChecker = securityThresholdChecker;
    }

    @Override
    protected void configure() {
        bind(Logger.class).toInstance(_logger);
        bind(ArtifactRisksFiller.class).toInstance(_artifactFiller);
        bind(PluginConfiguration.class).toInstance(_configuration);
        bind(SecurityThresholdChecker.class).toInstance(_securityThresholdChecker);

        if (_accessControlClient != null) {
            bind(AccessControlClient.class).toInstance(_accessControlClient);
        }
    }
}
