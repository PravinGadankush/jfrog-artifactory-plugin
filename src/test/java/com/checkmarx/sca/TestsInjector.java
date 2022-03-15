package com.checkmarx.sca;

import com.checkmarx.sca.communication.AccessControlClient;
import com.checkmarx.sca.communication.ScaHttpClient;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.scan.ArtifactChecker;
import com.google.inject.AbstractModule;
import org.artifactory.repo.Repositories;
import org.slf4j.Logger;

import javax.annotation.Nonnull;

public class TestsInjector extends AbstractModule {
    private final Logger _logger;
    private final ArtifactChecker _artifactChecker;
    private final PluginConfiguration _configuration;

    private AccessControlClient _accessControlClient;
    private ScaHttpClient _scaHttpClient;

    public TestsInjector(@Nonnull Logger logger,
                         @Nonnull ArtifactChecker artifactChecker,
                         @Nonnull PluginConfiguration configuration) {
        _logger = logger;
        _artifactChecker = artifactChecker;
        _configuration = configuration;
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
        bind(ArtifactChecker.class).toInstance(_artifactChecker);
        bind(PluginConfiguration.class).toInstance(_configuration);

        if(_accessControlClient != null){
            bind(AccessControlClient.class).toInstance(_accessControlClient);
        }

        if(_scaHttpClient != null){
            bind(ScaHttpClient.class).toInstance(_scaHttpClient);
        }
    }
}
