package com.checkmarx.sca;

import com.checkmarx.sca.communication.AccessControlClient;
import com.checkmarx.sca.communication.ScaHttpClient;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.scan.ArtifactRisksFiller;
import com.checkmarx.sca.scan.SecurityThresholdChecker;
import com.google.inject.AbstractModule;
import org.slf4j.Logger;

import javax.annotation.Nonnull;

public class TestsInjector extends AbstractModule {
    private final Logger _logger;
    private final PluginConfiguration _configuration;
    private final ArtifactRisksFiller _artifactRisksFiller;
    private final SecurityThresholdChecker _securityThresholdChecker;

    private AccessControlClient _accessControlClient;
    private ScaHttpClient _scaHttpClient;

    public TestsInjector(@Nonnull Logger logger,
                         @Nonnull PluginConfiguration configuration,
                         @Nonnull ArtifactRisksFiller artifactRisksFiller,
                         @Nonnull SecurityThresholdChecker securityThresholdChecker) {
        _logger = logger;
        _configuration = configuration;
        _artifactRisksFiller = artifactRisksFiller;
        _securityThresholdChecker = securityThresholdChecker;
    }

    public void setAccessControlClient(AccessControlClient accessControlClient){
        _accessControlClient = accessControlClient;
    }

    public void setScaHttpClient(ScaHttpClient scaHttpClient){
        _scaHttpClient = scaHttpClient;
    }

    @Override
    protected void configure() {
        bind(Logger.class).toInstance(_logger);
        bind(PluginConfiguration.class).toInstance(_configuration);
        bind(ArtifactRisksFiller.class).toInstance(_artifactRisksFiller);
        bind(SecurityThresholdChecker.class).toInstance(_securityThresholdChecker);

        if(_accessControlClient != null){
            bind(AccessControlClient.class).toInstance(_accessControlClient);
        }

        if(_scaHttpClient != null){
            bind(ScaHttpClient.class).toInstance(_scaHttpClient);
        }
    }
}
