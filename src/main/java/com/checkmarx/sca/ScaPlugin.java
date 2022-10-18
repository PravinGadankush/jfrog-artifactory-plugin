package com.checkmarx.sca;

import com.checkmarx.sca.communication.AccessControlClient;
import com.checkmarx.sca.configuration.ConfigurationReader;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.scan.ArtifactIdBuilder;
import com.checkmarx.sca.scan.ArtifactRisksFiller;
import com.checkmarx.sca.scan.SecurityThresholdChecker;
import com.checkmarx.sca.suggestion.PrivatePackageSuggestionHandler;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.artifactory.exception.CancelException;
import org.artifactory.repo.*;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static java.lang.String.format;

public class ScaPlugin {
    private final Injector _injector;
    private final Logger _logger;
    private final Repositories _repositories;

    public ScaPlugin(@Nonnull Logger logger,
                     @Nonnull File pluginsDirectory,
                     @Nonnull Repositories repositories) throws IOException {
        _logger = logger;
        try {
            var configuration = ConfigurationReader.loadConfiguration(pluginsDirectory, logger);
            configuration.validate();

            var accessControlClient = tryToAuthenticate(configuration, logger);

            _repositories = repositories;
            var risksFiller = new ArtifactRisksFiller(repositories);
            var securityThresholdChecker = new SecurityThresholdChecker(repositories);
            var privatePackageSuggestionHandler = new PrivatePackageSuggestionHandler(repositories, configuration.hasAuthConfiguration());
            var appInjector = new AppInjector(_logger, accessControlClient, risksFiller, configuration, securityThresholdChecker, privatePackageSuggestionHandler);

            _injector = Guice.createInjector(appInjector);
        } catch (Exception ex) {
            _logger.error("Sca plugin could not be initialized!");
            throw ex;
        }
    }

    private AccessControlClient tryToAuthenticate(@Nonnull PluginConfiguration configuration, @Nonnull Logger logger) {
        AccessControlClient accessControlClient = null;
        try {
            if (configuration.hasAuthConfiguration()) {
                accessControlClient = new AccessControlClient(configuration, logger);
                accessControlClient.Authenticate(configuration.getAccessControlCredentials());
            } else {
                _logger.info("Authentication configuration not defined.");
            }
        } catch (Exception ex) {
            _logger.error("Authentication failed. Working without authentication.");
        }

        return accessControlClient;
    }

    public void checkArtifactsAlreadyPresent(RepoPath repoPath) {
        var nonVirtualRepoPaths = getNonVirtualRepoPaths(repoPath);

        addPackageRisks(repoPath, nonVirtualRepoPaths);
    }


    public void checkArtifactsForSuggestionOnPrivatePackages(RepoPath repoPath) {
        var nonVirtualRepoPaths = getNonVirtualRepoPaths(repoPath);

        var suggestion = _injector.getInstance(PrivatePackageSuggestionHandler.class);

        for (var artifact : nonVirtualRepoPaths) {
            suggestion.suggestPrivatePackage(artifact, nonVirtualRepoPaths);
        }
    }

    public void beforeDownload(RepoPath repoPath) {
        var nonVirtualRepoPaths = getNonVirtualRepoPaths(repoPath);

        var riskAddedSuccessfully = addPackageRisks(repoPath, nonVirtualRepoPaths);

        if (riskAddedSuccessfully) {
            checkRiskThreshold(repoPath, nonVirtualRepoPaths);
        }
    }

    public void beforeUpload(RepoPath repoPath) {
        var nonVirtualRepoPaths = getNonVirtualRepoPaths(repoPath);

        var suggestionHandler = _injector.getInstance(PrivatePackageSuggestionHandler.class);

        suggestionHandler.suggestPrivatePackage(repoPath, nonVirtualRepoPaths);
    }

    private ArrayList<RepoPath> getNonVirtualRepoPaths(RepoPath repoPath) {
        var repositoryKey = repoPath.getRepoKey();
        var repoConfiguration = _repositories.getRepositoryConfiguration(repositoryKey);

        var nonVirtualRepoPaths = new ArrayList<RepoPath>();
        if (repoConfiguration instanceof VirtualRepositoryConfiguration) {
            setNonVirtualRepoPathsRepoPathsOfVirtualRepository(nonVirtualRepoPaths, repoConfiguration, repoPath.getPath());
        } else {
            nonVirtualRepoPaths.add(repoPath);
        }

        return nonVirtualRepoPaths;
    }

    private void setNonVirtualRepoPathsRepoPathsOfVirtualRepository(@Nonnull ArrayList<RepoPath> nonVirtualRepoPaths, @Nonnull RepositoryConfiguration repoConfiguration, @Nonnull String artifactPath){
        var virtualConfiguration = (VirtualRepositoryConfiguration) repoConfiguration;
        for (var repo : virtualConfiguration.getRepositories()) {
            var repoPathFromVirtual = RepoPathFactory.create(repo, artifactPath);
            if (_repositories.exists(repoPathFromVirtual)) {
                nonVirtualRepoPaths.add(repoPathFromVirtual);
            }
        }
    }

    private boolean addPackageRisks(@Nonnull RepoPath repoPath, @Nonnull ArrayList<RepoPath> nonVirtualRepoPaths){
        try {
            var path = repoPath.getPath();
            if (path == null) {
                _logger.error("SCA was unable to complete verification. The Path was not provided.");
                return false;
            }

            var artifactChecker = _injector.getInstance(ArtifactRisksFiller.class);

            return artifactChecker.addArtifactRisks(repoPath, nonVirtualRepoPaths);
        } catch (Exception ex) {
            _logger.error(format("SCA was unable to complete verification of: %s.\nException message: %s", repoPath.getName(), ex.getMessage()));
            return false;
        }
    }

    private void checkRiskThreshold(@Nonnull RepoPath repoPath, @Nonnull ArrayList<RepoPath> nonVirtualRepoPaths){
        try {
            var thresholdChecker = _injector.getInstance(SecurityThresholdChecker.class);
            thresholdChecker.checkSecurityRiskThreshold(repoPath, nonVirtualRepoPaths);
        } catch (CancelException ex) {
            _logger.info(format("The download was blocked by configuration. Artifact Name: %s", repoPath.getName()));
            throw ex;
        } catch (Exception ex) {
            _logger.error(format("SCA was unable to complete the security risk threshold verification for the Artifact: %s.\nException: %s", repoPath.getName(), ex));
        }
    }
}
