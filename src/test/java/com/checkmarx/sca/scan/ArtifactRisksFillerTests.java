package com.checkmarx.sca.scan;

import com.checkmarx.sca.PropertiesConstants;
import com.checkmarx.sca.TestsInjector;
import com.checkmarx.sca.communication.ScaHttpClient;
import com.checkmarx.sca.communication.exceptions.UnexpectedResponseCodeException;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.models.ArtifactInfo;
import com.checkmarx.sca.models.PackageRiskAggregation;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.LocalRepositoryConfiguration;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.artifactory.repo.VirtualRepositoryConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class ArtifactRisksFillerTests {

    private final String RepoKey = "test-remote";

    private final String ArtifactType = "npm";
    private final String ArtifactName = "lodash";
    private final String ArtifactVersion = "0.2.1";

    private Logger _logger;
    private Injector _injector;
    private Repositories _repositories;
    private ScaHttpClient _scaHttpClient;

    private RepoPath _mainRepoPath;

    @BeforeEach
    public void beforeEach() {
        _mainRepoPath = Mockito.mock(RepoPath.class);
        when(_mainRepoPath.getRepoKey()).thenReturn(RepoKey);
        when(_mainRepoPath.getPath()).thenReturn(format("%1$s/-/%1$s-%2$s.tgz", ArtifactName, ArtifactVersion));

        _logger = Mockito.mock(Logger.class);
        _repositories = Mockito.mock(Repositories.class);
        _scaHttpClient = Mockito.mock(ScaHttpClient.class);
        var securityThresholdChecker = Mockito.mock(SecurityThresholdChecker.class);

        var _configuration = new PluginConfiguration(new Properties(), _logger);

        var appInjector = new TestsInjector(_logger, _configuration, new ArtifactRisksFiller(_repositories), securityThresholdChecker);
        appInjector.setScaHttpClient(_scaHttpClient);

        _injector = Guice.createInjector(appInjector);
    }

    @DisplayName("Check artifact with success")
    @Test
    public void addArtifactRisksWithSuccess() {

        var fileLayoutInfo = CreateFileLayoutInfoMock();

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);
        when(localRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);

        when(_repositories.exists(_mainRepoPath)).thenReturn(false);
        when(_repositories.getLayoutInfo(_mainRepoPath)).thenReturn(fileLayoutInfo);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);
        when(_repositories.setProperty(isA(RepoPath.class), isA(String.class), isA(String.class))).thenReturn(null);

        MockScaHttpClientMethods();

        var ArtifactRisksFiller = _injector.getInstance(ArtifactRisksFiller.class);

        ArtifactRisksFiller.addArtifactRisks(_mainRepoPath, new ArrayList<>(List.of(_mainRepoPath)));

        withoutWarningsAndErrors();
        Mockito.verify(_repositories, times(7)).setProperty(isA(RepoPath.class), isA(String.class), isA(String.class));
    }

    @DisplayName("Check artifact with success - two repositories repository")
    @Test
    public void addArtifactRisksWithSuccessFromVirtualRepo() throws ExecutionException, InterruptedException {

        var repoPathLocal = Mockito.mock(RepoPath.class);

        var fileLayoutInfo = CreateFileLayoutInfoMock();

        var virtualRepositoryConfiguration = Mockito.mock(VirtualRepositoryConfiguration.class);
        when(virtualRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);

        when(_repositories.exists(repoPathLocal)).thenReturn(true);
        when(_repositories.getLayoutInfo(_mainRepoPath)).thenReturn(fileLayoutInfo);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(virtualRepositoryConfiguration);
        when(_repositories.setProperty(isA(RepoPath.class), isA(String.class), isA(String.class))).thenReturn(null);

        MockScaHttpClientMethods();

        var ArtifactRisksFiller = _injector.getInstance(ArtifactRisksFiller.class);

        var result = ArtifactRisksFiller.addArtifactRisks(_mainRepoPath, new ArrayList<>(List.of(repoPathLocal, repoPathLocal)));

        Assertions.assertTrue(result);
        withoutWarningsAndErrors();
        Mockito.verify(_repositories, times(14)).setProperty(isA(RepoPath.class), isA(String.class), isA(String.class));
        Mockito.verify(_scaHttpClient, times(1)).getArtifactInformation(isA(String.class), isA(String.class), isA(String.class));
        Mockito.verify(_scaHttpClient, times(1)).getRiskAggregationOfArtifact(isA(String.class), isA(String.class), isA(String.class));
    }

    @DisplayName("Check artifact with success - two repositories not ignored by one")
    @Test
    public void addArtifactRisksWithSuccessFromVirtualRepoNotIgnoredByOne() throws ExecutionException, InterruptedException {

        var repoPathLocalOld = Mockito.mock(RepoPath.class);
        var repoPathLocalRecent = Mockito.mock(RepoPath.class);

        var fileLayoutInfo = CreateFileLayoutInfoMock();

        var virtualRepositoryConfiguration = Mockito.mock(VirtualRepositoryConfiguration.class);
        when(virtualRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);

        when(_repositories.exists(repoPathLocalOld)).thenReturn(true);
        when(_repositories.exists(repoPathLocalRecent)).thenReturn(true);
        when(_repositories.getLayoutInfo(_mainRepoPath)).thenReturn(fileLayoutInfo);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(virtualRepositoryConfiguration);

        getAllCxProperties(repoPathLocalOld, Instant.now().minusSeconds(22000).toString());
        getAllCxProperties(repoPathLocalRecent, Instant.now().toString());
        when(_repositories.setProperty(isA(RepoPath.class), isA(String.class), isA(String.class))).thenReturn(null);

        MockScaHttpClientMethods();

        var ArtifactRisksFiller = _injector.getInstance(ArtifactRisksFiller.class);
        var result = ArtifactRisksFiller.addArtifactRisks(_mainRepoPath, new ArrayList<>(List.of(repoPathLocalRecent, repoPathLocalOld)));

        Assertions.assertTrue(result);
        withoutWarningsAndErrors();
        Mockito.verify(_repositories, times(14)).setProperty(isA(RepoPath.class), isA(String.class), isA(String.class));
        Mockito.verify(_scaHttpClient, times(1)).getArtifactInformation(isA(String.class), isA(String.class), isA(String.class));
        Mockito.verify(_scaHttpClient, times(1)).getRiskAggregationOfArtifact(isA(String.class), isA(String.class), isA(String.class));
    }

    @DisplayName("Failed to check artifact - artifact not found")
    @Test
    public void failedToAddArtifactRisksArtifactNotFound() throws ExecutionException, InterruptedException {

        var virtualRepositoryConfiguration = Mockito.mock(VirtualRepositoryConfiguration.class);

        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(virtualRepositoryConfiguration);

        var ArtifactRisksFiller = _injector.getInstance(ArtifactRisksFiller.class);

        var result = ArtifactRisksFiller.addArtifactRisks(_mainRepoPath, new ArrayList<>());

        Assertions.assertFalse(result);
        Mockito.verify(_logger, times(1)).warn(Mockito.argThat(s -> s.contains("Artifact not found in any repository.")));
        Mockito.verify(_scaHttpClient, never()).getArtifactInformation(isA(String.class), isA(String.class), isA(String.class));
        Mockito.verify(_scaHttpClient, never()).getRiskAggregationOfArtifact(isA(String.class), isA(String.class), isA(String.class));
    }

    @DisplayName("Artifact verification ignored")
    @Test
    public void artifactVerificationIgnored() throws ExecutionException, InterruptedException {

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);

        when(_repositories.exists(_mainRepoPath)).thenReturn(true);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);
        getAllCxProperties(_mainRepoPath, Instant.now().toString());

        var ArtifactRisksFiller = _injector.getInstance(ArtifactRisksFiller.class);

        var result = ArtifactRisksFiller.addArtifactRisks(_mainRepoPath, new ArrayList<>(List.of(_mainRepoPath)));

        Assertions.assertTrue(result);
        Mockito.verify(_logger, times(1)).info(Mockito.argThat(s -> s.contains("Scan ignored by cache configuration.")));
        Mockito.verify(_scaHttpClient, never()).getArtifactInformation(isA(String.class), isA(String.class), isA(String.class));
        Mockito.verify(_scaHttpClient, never()).getRiskAggregationOfArtifact(isA(String.class), isA(String.class), isA(String.class));
    }

    @DisplayName("Artifact verification not ignored - missing properties")
    @Test
    public void artifactVerificationNotIgnoredMissingProperties() {

        var fileLayoutInfo = CreateFileLayoutInfoMock();

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);
        when(localRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);

        when(_repositories.exists(_mainRepoPath)).thenReturn(true);
        when(_repositories.getLayoutInfo(_mainRepoPath)).thenReturn(fileLayoutInfo);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);
        when(_repositories.setProperty(isA(RepoPath.class), isA(String.class), isA(String.class))).thenReturn(null);

        var properties = Mockito.mock(org.artifactory.md.Properties.class);
        when(properties.containsKey(PropertiesConstants.TOTAL_RISKS_COUNT)).thenReturn(false);
        when(properties.containsKey(PropertiesConstants.LAST_SCAN)).thenReturn(true);
        when(properties.getFirst(PropertiesConstants.LAST_SCAN)).thenReturn(Instant.now().toString());

        when(_repositories.getProperties(_mainRepoPath)).thenReturn(properties);

        MockScaHttpClientMethods();

        var ArtifactRisksFiller = _injector.getInstance(ArtifactRisksFiller.class);

        var result = ArtifactRisksFiller.addArtifactRisks(_mainRepoPath, new ArrayList<>(List.of(_mainRepoPath)));

        Assertions.assertTrue(result);
        withoutWarningsAndErrors();
        Mockito.verify(_repositories, times(7)).setProperty(isA(RepoPath.class), isA(String.class), isA(String.class));
    }


    @DisplayName("Failed to check artifact - invalid artifact id")
    @Test
    public void failedToAddArtifactRisksInvalidArtifactId() throws ExecutionException, InterruptedException {

        var fileLayoutInfo = CreateFileLayoutInfoMock();

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);
        when(localRepositoryConfiguration.getPackageType()).thenReturn(null);

        when(_repositories.exists(_mainRepoPath)).thenReturn(false);
        when(_repositories.getLayoutInfo(_mainRepoPath)).thenReturn(fileLayoutInfo);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);

        var ArtifactRisksFiller = _injector.getInstance(ArtifactRisksFiller.class);

        var result = ArtifactRisksFiller.addArtifactRisks(_mainRepoPath, new ArrayList<>(List.of(_mainRepoPath)));

        Assertions.assertFalse(result);
        Mockito.verify(_logger, times(1)).error(Mockito.argThat(s -> s.contains("The artifact id was not built correctly.")));
        Mockito.verify(_scaHttpClient, never()).getArtifactInformation(isA(String.class), isA(String.class), isA(String.class));
        Mockito.verify(_scaHttpClient, never()).getRiskAggregationOfArtifact(isA(String.class), isA(String.class), isA(String.class));
    }

    @DisplayName("Failed to check artifact - failed to get artifact info")
    @Test
    public void failedToAddArtifactRisksFailedToGetArtifactInfo() throws ExecutionException, InterruptedException {

        var fileLayoutInfo = CreateFileLayoutInfoMock();

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);
        when(localRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);

        when(_repositories.exists(_mainRepoPath)).thenReturn(false);
        when(_repositories.getLayoutInfo(_mainRepoPath)).thenReturn(fileLayoutInfo);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);

        var exception = new InterruptedException("Test Exception");
        when(_scaHttpClient.getArtifactInformation(ArtifactType, ArtifactName, ArtifactVersion))
                .thenThrow(exception);

        var ArtifactRisksFiller = _injector.getInstance(ArtifactRisksFiller.class);

        var result = ArtifactRisksFiller.addArtifactRisks(_mainRepoPath, new ArrayList<>(List.of(_mainRepoPath)));

        Assertions.assertFalse(result);
        Mockito.verify(_logger, times(1)).error(Mockito.argThat(s -> s.contains(exception.getMessage())));
        Mockito.verify(_repositories, times(0)).setProperty(isA(RepoPath.class), isA(String.class), isA(String.class));
    }

    @DisplayName("Failed to check artifact - failed to get artifact info artifact not found")
    @Test
    public void failedToAddArtifactRisksFailedToGetArtifactInfoArtifactNotFound() throws ExecutionException, InterruptedException {

        var fileLayoutInfo = CreateFileLayoutInfoMock();

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);
        when(localRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);

        when(_repositories.exists(_mainRepoPath)).thenReturn(false);
        when(_repositories.getLayoutInfo(_mainRepoPath)).thenReturn(fileLayoutInfo);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);

        var exception = new UnexpectedResponseCodeException(404);
        when(_scaHttpClient.getArtifactInformation(ArtifactType, ArtifactName, ArtifactVersion))
                .thenThrow(exception);

        var ArtifactRisksFiller = _injector.getInstance(ArtifactRisksFiller.class);

        var result = ArtifactRisksFiller.addArtifactRisks(_mainRepoPath, new ArrayList<>(List.of(_mainRepoPath)));

        Assertions.assertFalse(result);
        Mockito.verify(_logger, times(1)).error(Mockito.argThat(s -> s.contains(exception.getMessage())));
        Mockito.verify(_repositories, never()).setProperty(isA(RepoPath.class), isA(String.class), isA(String.class));
    }

    @DisplayName("Failed to check artifact - failed to get artifact aggregation risk")
    @Test
    public void failedToAddArtifactRisksFailedToGetAggregationRisk() throws ExecutionException, InterruptedException {

        var fileLayoutInfo = CreateFileLayoutInfoMock();

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);
        when(localRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);

        when(_repositories.exists(_mainRepoPath)).thenReturn(false);
        when(_repositories.getLayoutInfo(_mainRepoPath)).thenReturn(fileLayoutInfo);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);

        var artifactInfo = CreateArtifactInfo();
        when(_scaHttpClient.getArtifactInformation(ArtifactType, ArtifactName, ArtifactVersion))
                .thenReturn(artifactInfo);

        var exception = new UnexpectedResponseCodeException(404);
        when(_scaHttpClient.getRiskAggregationOfArtifact(artifactInfo.getPackageType(), artifactInfo.getName(), artifactInfo.getVersion()))
                .thenThrow(exception);

        var ArtifactRisksFiller = _injector.getInstance(ArtifactRisksFiller.class);

        var result = ArtifactRisksFiller.addArtifactRisks(_mainRepoPath, new ArrayList<>(List.of(_mainRepoPath)));

        Assertions.assertFalse(result);
        Mockito.verify(_logger, times(1)).error(Mockito.argThat(s -> s.contains(exception.getMessage())));
        Mockito.verify(_repositories, times(0)).setProperty(isA(RepoPath.class), isA(String.class), isA(String.class));
    }

    private FileLayoutInfo CreateFileLayoutInfoMock(){
        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);
        when(fileLayoutInfo.isValid()).thenReturn(false);
        return fileLayoutInfo;
    }

    private void MockScaHttpClientMethods() {
        try {
            var artifactInfo = CreateArtifactInfo();
            when(_scaHttpClient.getArtifactInformation(ArtifactType, ArtifactName, ArtifactVersion))
                    .thenReturn(artifactInfo);

            var vulnerabilities = CreatePackageRiskAggregation();
            when(_scaHttpClient.getRiskAggregationOfArtifact(artifactInfo.getPackageType(), artifactInfo.getName(), artifactInfo.getVersion()))
                    .thenReturn(vulnerabilities);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArtifactInfo CreateArtifactInfo(){
        return new Gson().fromJson("{\"id\":{\"identifier\":\"Npm-lodash-0.2.1\"},\"name\":\"lodash\",\"version\":\"0.2.1\",\"type\":\"Npm\",\"releaseDate\":\"2012-05-24T21:53:08\",\"description\":\"A drop-in replacement for Underscore.js that delivers performance improvements, bug fixes, and additional features.\",\"repositoryUrl\":\"git://github.com/bestiejs/lodash.git\",\"binaryUrl\":\"https://registry.npmjs.org/lodash/-/lodash-0.2.1.tgz\",\"projectUrl\":\"\",\"bugsUrl\":null,\"sourceUrl\":\"\",\"projectHomePage\":\"http://lodash.com\",\"homePage\":\"\",\"license\":\"\",\"summary\":\"\",\"url\":\"\",\"owner\":\"\"}", ArtifactInfo.class);
    }

    private PackageRiskAggregation CreatePackageRiskAggregation(){
        Type listType = new TypeToken<PackageRiskAggregation>(){}.getType();

        return new Gson().fromJson("{\"packageVulnerabilitiesAggregation\":{\"vulnerabilitiesCount\":159,\"maxRiskSeverity\":\"High\",\"maxRiskScore\":9.8,\"highRiskCount\":151,\"mediumRiskCount\":8,\"lowRiskCount\":0}}", listType);
    }

    private void withoutWarningsAndErrors(){
        Mockito.verify(_logger, times(2)).info(isA(String.class));
        Mockito.verify(_logger, never()).info(Mockito.anyString(), isA(Exception.class));

        Mockito.verify(_logger, never()).error(isA(String.class));
        Mockito.verify(_logger, never()).error(isA(String.class), isA(Exception.class));

        Mockito.verify(_logger, never()).warn(isA(String.class));
        Mockito.verify(_logger, never()).warn(isA(String.class), isA(Exception.class));
    }

    private void getAllCxProperties(RepoPath repoPath, String scanDate) {
        var properties = Mockito.mock(org.artifactory.md.Properties.class);

        when(properties.containsKey(PropertiesConstants.TOTAL_RISKS_COUNT)).thenReturn(true);
        when(properties.containsKey(PropertiesConstants.LOW_RISKS_COUNT)).thenReturn(true);
        when(properties.containsKey(PropertiesConstants.MEDIUM_RISKS_COUNT)).thenReturn(true);
        when(properties.containsKey(PropertiesConstants.HIGH_RISKS_COUNT)).thenReturn(true);
        when(properties.containsKey(PropertiesConstants.RISK_SCORE)).thenReturn(true);
        when(properties.containsKey(PropertiesConstants.RISK_LEVEL)).thenReturn(true);
        when(properties.containsKey(PropertiesConstants.LAST_SCAN)).thenReturn(true);
        when(properties.getFirst(PropertiesConstants.LAST_SCAN)).thenReturn(scanDate);

        when(_repositories.getProperties(repoPath)).thenReturn(properties);
    }
}
