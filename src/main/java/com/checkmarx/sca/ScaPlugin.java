package com.checkmarx.sca;

import com.checkmarx.sca.communication.AccessControlClient;
import com.checkmarx.sca.communication.models.AccessControlCredentials;
import com.checkmarx.sca.configuration.ConfigurationReader;
import com.checkmarx.sca.scan.ArtifactChecker;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;

import static java.lang.String.format;

public class ScaPlugin
{
    private final Injector _injector;
    private final Logger _logger;

    public ScaPlugin(@Nonnull Logger logger,
                     @Nonnull File pluginsDirectory,
                     @Nonnull Repositories repositories) throws IOException {
        _logger = logger;
        try {
            var configuration = ConfigurationReader.loadConfiguration(pluginsDirectory);
            configuration.validate();

            var accessControlClient = new AccessControlClient(configuration);
            accessControlClient.Authenticate(new AccessControlCredentials("user", "pass", "tenant"));

            var artifactChecker = new ArtifactChecker(repositories);
            var appInjector = new AppInjector(_logger, artifactChecker, configuration, accessControlClient);

            _injector = Guice.createInjector(appInjector);
        } catch (Exception ex) {
            _logger.error("Sca plugin could not be initialized!");
            throw ex;
        }
    }

    public void beforeDownload(RepoPath repoPath) {
        try {
            var path = repoPath.getPath();
            if (path == null) {
                _logger.error("SCA was unable to complete verification. The Path was not provided.");
                return;
            }

            var artifactChecker = _injector.getInstance(ArtifactChecker.class);
            artifactChecker.checkArtifact(repoPath);
        } catch (Exception ex) {
            _logger.error(format("SCA was unable to complete verification of: %s.\nException message: %s", repoPath.getName(), ex.getMessage()));
        }
    }
}
