package com.checkmarx.sca.suggestion;

import com.checkmarx.sca.AppInjector;
import com.checkmarx.sca.PackageManager;
import com.checkmarx.sca.communication.AccessControlClient;
import com.checkmarx.sca.communication.ScaHttpClient;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.models.ArtifactId;
import com.checkmarx.sca.scan.ArtifactIdBuilder;
import com.checkmarx.sca.scan.ArtifactRisksFiller;
import com.checkmarx.sca.scan.LicenseAllowanceChecker;
import com.checkmarx.sca.scan.SecurityThresholdChecker;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.artifactory.repo.RepositoryConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;


public class PrivatePackageSuggestionHandlerTests {
    private Injector _injector;

    private ScaHttpClient _client;

    @BeforeEach
    public void beforeEach() throws ExecutionException, InterruptedException {
        _injector = createAppInjectorForTests();
    }

    @Test
    @DisplayName("Checks if suggestions for private packages are being performed")
    public void checkSuggestionsPerformedOnLocalPackagesAtConfigurationActivation() throws ExecutionException, InterruptedException {
        var privatePackagesSuggestionHandler = _injector.getInstance(PrivatePackageSuggestionHandler.class);
        var client = _injector.getInstance(ScaHttpClient.class);
        var artifactBuilder = _injector.getInstance(ArtifactIdBuilder.class);

        var repoPath = Mockito.mock(RepoPath.class);
        when(repoPath.getRepoKey()).thenReturn("");
        when(repoPath.getPath()).thenReturn("");

        var localRepos = new ArrayList<RepoPath>();
        localRepos.add(repoPath);

        privatePackagesSuggestionHandler.suggestPrivatePackage(repoPath, localRepos);
        var expectedCount = 1;
        PrivatePackageSuggested(client, expectedCount);
        verify(_client, times(1)).suggestPrivatePackage(isA(ArtifactId.class));
        verify(artifactBuilder, atLeast(1)).getArtifactId(isA(FileLayoutInfo.class), isA(RepoPath.class), isA(PackageManager.class));
    }

    private static void PrivatePackageSuggested(ScaHttpClient client, int expectedCount) throws ExecutionException, InterruptedException {
        Mockito.verify(client, expectedCount <= 0? never(): expectedCount == Integer.MAX_VALUE? atLeast(1): times(expectedCount)).suggestPrivatePackage(isA(ArtifactId.class));
    }

    private Injector createAppInjectorForTests() throws ExecutionException, InterruptedException {
        var logger = Mockito.mock(Logger.class);
        var artifactRisksFiller = mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = mock(SecurityThresholdChecker.class);
        var licenseAllowanceChecker = mock(LicenseAllowanceChecker.class);

        var properties = new Properties();
        properties.setProperty("sca.api.url", "http://localhost:8080/");
        properties.setProperty("sca.authentication.url", "http://localhost:8080/");

        var configuration = new PluginConfiguration(properties, logger);
        var accessControlClient = new AccessControlClient(configuration, logger);

        var repositories = Mockito.mock(Repositories.class);
        Mockito.when(repositories.getLayoutInfo(isA(RepoPath.class))).thenReturn(Mockito.mock(FileLayoutInfo.class));
        Mockito.when(repositories.getRepositoryConfiguration(isA(String.class))).thenReturn(Mockito.mock(RepositoryConfiguration.class));
        Mockito.when(repositories.getLayoutInfo(isA(RepoPath.class))).thenReturn(Mockito.mock(FileLayoutInfo.class));
        var suggestionHandler = new PrivatePackageSuggestionHandler(repositories, true);

        var appInjector = new AppInjector(logger, accessControlClient, artifactRisksFiller, configuration, securityThresholdChecker, licenseAllowanceChecker, suggestionHandler);

        var artifactId = Mockito.mock(ArtifactId.class);
        Mockito.when(artifactId.isInvalid()).thenReturn(false);

        var artifactBuilder = Mockito.mock(ArtifactIdBuilder.class);
        Mockito.when(artifactBuilder.getArtifactId(isA(FileLayoutInfo.class), isA(RepoPath.class), isA(PackageManager.class))).thenReturn(artifactId);

        _client = Mockito.mock(ScaHttpClient.class);

        Mockito.when(_client.suggestPrivatePackage(isA(ArtifactId.class))).thenReturn(true);

        var suggestionInjector = new AbstractModule() {
            @Override public void configure(){
                bind(ArtifactIdBuilder.class).toInstance(artifactBuilder);
                bind(ScaHttpClient.class).toInstance(_client);
            }
        };

        return Guice.createInjector(appInjector, suggestionInjector);
    }
}
