package com.checkmarx.sca.scan;

import com.checkmarx.sca.PropertiesConstants;
import com.checkmarx.sca.TestsInjector;
import com.checkmarx.sca.communication.ScaHttpClient;
import com.checkmarx.sca.communication.exceptions.UnexpectedResponseCodeException;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.models.ArtifactId;
import com.checkmarx.sca.models.ArtifactInfo;
import com.checkmarx.sca.models.Vulnerability;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.*;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
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
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class ArtifactCheckerTests {

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
        when(_mainRepoPath.getName()).thenReturn(format("%s-%s", ArtifactName, ArtifactVersion));

        _logger = Mockito.mock(Logger.class);
        _repositories = Mockito.mock(Repositories.class);
        _scaHttpClient = Mockito.mock(ScaHttpClient.class);
        var _configuration = new PluginConfiguration(new Properties());

        var appInjector = new TestsInjector(_logger, new ArtifactChecker(_repositories), _configuration);
        appInjector.setScaHttpClient(_scaHttpClient);

        _injector = Guice.createInjector(appInjector);
    }

    @DisplayName("Check artifact with success")
    @Test
    public void checkArtifactWithSuccess() throws ExecutionException {

        var fileLayoutInfo = CreateFileLayoutInfoMock();

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);
        when(localRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);

        when(_repositories.exists(_mainRepoPath)).thenReturn(false);
        when(_repositories.getLayoutInfo(_mainRepoPath)).thenReturn(fileLayoutInfo);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);
        when(_repositories.setProperty(isA(RepoPath.class), isA(String.class), isA(String.class))).thenReturn(null);

        MockScaHttpClientMethods();

        var artifactChecker = _injector.getInstance(ArtifactChecker.class);

        artifactChecker.checkArtifact(_mainRepoPath);

        loggerNeverCalled();
        Mockito.verify(_repositories, times(8)).setProperty(isA(RepoPath.class), isA(String.class), isA(String.class));
    }

    @DisplayName("Check artifact with success - virtual repository")
    @Test
    public void checkArtifactWithSuccessFromVirtualRepo() throws ExecutionException, InterruptedException {

        var repoPathLocal = Mockito.mock(RepoPath.class);

        var fileLayoutInfo = CreateFileLayoutInfoMock();

        var virtualRepositoryConfiguration = Mockito.mock(VirtualRepositoryConfiguration.class);
        when(virtualRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);
        when(virtualRepositoryConfiguration.getRepositories()).thenReturn(List.of("xyz", "abc"));

        when(_repositories.exists(repoPathLocal)).thenReturn(true);
        when(_repositories.getLayoutInfo(_mainRepoPath)).thenReturn(fileLayoutInfo);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(virtualRepositoryConfiguration);
        when(_repositories.setProperty(isA(RepoPath.class), isA(String.class), isA(String.class))).thenReturn(null);

        MockScaHttpClientMethods();

        var artifactChecker = _injector.getInstance(ArtifactChecker.class);

        try (MockedStatic<RepoPathFactory> utilities = Mockito.mockStatic(RepoPathFactory.class)) {
            utilities.when(() -> RepoPathFactory.create("xyz", _mainRepoPath.getName()))
                    .thenReturn(repoPathLocal);
            utilities.when(() -> RepoPathFactory.create("abc", _mainRepoPath.getName()))
                    .thenReturn(repoPathLocal);

            artifactChecker.checkArtifact(_mainRepoPath);
        }

        loggerNeverCalled();
        Mockito.verify(_repositories, times(16)).setProperty(isA(RepoPath.class), isA(String.class), isA(String.class));
        Mockito.verify(_scaHttpClient, times(1)).getArtifactInformation(isA(String.class), isA(String.class), isA(String.class));
        Mockito.verify(_scaHttpClient, times(1)).getVulnerabilitiesForArtifact(isA(ArtifactId.class));
    }

    @DisplayName("Check artifact with success - virtual repository not ignored by one")
    @Test
    public void checkArtifactWithSuccessFromVirtualRepoNotIgnoredByOne() throws ExecutionException, InterruptedException {

        var repoPathLocalOld = Mockito.mock(RepoPath.class);
        var repoPathLocalRecent = Mockito.mock(RepoPath.class);

        var fileLayoutInfo = CreateFileLayoutInfoMock();

        var virtualRepositoryConfiguration = Mockito.mock(VirtualRepositoryConfiguration.class);
        when(virtualRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);
        when(virtualRepositoryConfiguration.getRepositories()).thenReturn(List.of("xyz", "abc"));

        when(_repositories.exists(repoPathLocalOld)).thenReturn(true);
        when(_repositories.exists(repoPathLocalRecent)).thenReturn(true);
        when(_repositories.getLayoutInfo(_mainRepoPath)).thenReturn(fileLayoutInfo);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(virtualRepositoryConfiguration);
        when(_repositories.getProperty(repoPathLocalOld, PropertiesConstants.SCAN_DATE)).thenReturn(Instant.now().minusSeconds(22000).toString());
        when(_repositories.getProperty(repoPathLocalRecent, PropertiesConstants.SCAN_DATE)).thenReturn(Instant.now().toString());
        when(_repositories.setProperty(isA(RepoPath.class), isA(String.class), isA(String.class))).thenReturn(null);

        MockScaHttpClientMethods();

        var artifactChecker = _injector.getInstance(ArtifactChecker.class);

        try (MockedStatic<RepoPathFactory> utilities = Mockito.mockStatic(RepoPathFactory.class)) {
            utilities.when(() -> RepoPathFactory.create("xyz", _mainRepoPath.getName()))
                    .thenReturn(repoPathLocalRecent);
            utilities.when(() -> RepoPathFactory.create("abc", _mainRepoPath.getName()))
                    .thenReturn(repoPathLocalOld);

            artifactChecker.checkArtifact(_mainRepoPath);
        }

        loggerNeverCalled();
        Mockito.verify(_repositories, times(16)).setProperty(isA(RepoPath.class), isA(String.class), isA(String.class));
        Mockito.verify(_scaHttpClient, times(1)).getArtifactInformation(isA(String.class), isA(String.class), isA(String.class));
        Mockito.verify(_scaHttpClient, times(1)).getVulnerabilitiesForArtifact(isA(ArtifactId.class));
    }

    @DisplayName("Failed to check artifact - artifact not found")
    @Test
    public void failedToCheckArtifactArtifactNotFound() throws ExecutionException, InterruptedException {

        var repoPathLocal = Mockito.mock(RepoPath.class);

        var virtualRepositoryConfiguration = Mockito.mock(VirtualRepositoryConfiguration.class);
        when(virtualRepositoryConfiguration.getRepositories()).thenReturn(List.of("xyz", "abc"));

        when(_repositories.exists(repoPathLocal)).thenReturn(false);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(virtualRepositoryConfiguration);

        var artifactChecker = _injector.getInstance(ArtifactChecker.class);

        try (MockedStatic<RepoPathFactory> utilities = Mockito.mockStatic(RepoPathFactory.class)) {
            utilities.when(() -> RepoPathFactory.create("xyz", _mainRepoPath.getName()))
                    .thenReturn(repoPathLocal);
            utilities.when(() -> RepoPathFactory.create("abc", _mainRepoPath.getName()))
                    .thenReturn(repoPathLocal);

            artifactChecker.checkArtifact(_mainRepoPath);
        }

        Mockito.verify(_logger, times(1)).warn(Mockito.argThat(s -> s.contains("Artifact not found in any repository.")));
        Mockito.verify(_scaHttpClient, never()).getArtifactInformation(isA(String.class), isA(String.class), isA(String.class));
        Mockito.verify(_scaHttpClient, never()).getVulnerabilitiesForArtifact(isA(ArtifactId.class));
    }

    @DisplayName("Artifact verification ignored")
    @Test
    public void artifactVerificationIgnored() throws ExecutionException, InterruptedException {

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);

        when(_repositories.exists(_mainRepoPath)).thenReturn(true);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);
        when(_repositories.getProperty(_mainRepoPath, PropertiesConstants.SCAN_DATE)).thenReturn(Instant.now().toString());

        var artifactChecker = _injector.getInstance(ArtifactChecker.class);

        artifactChecker.checkArtifact(_mainRepoPath);

        Mockito.verify(_logger, times(1)).info(Mockito.argThat(s -> s.contains("Scan ignored by cache configuration.")));
        Mockito.verify(_scaHttpClient, never()).getArtifactInformation(isA(String.class), isA(String.class), isA(String.class));
        Mockito.verify(_scaHttpClient, never()).getVulnerabilitiesForArtifact(isA(ArtifactId.class));
    }

    @DisplayName("Failed to check artifact - invalid artifact id")
    @Test
    public void failedToCheckArtifactInvalidArtifactId() throws ExecutionException, InterruptedException {

        var fileLayoutInfo = CreateFileLayoutInfoMock();

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);
        when(localRepositoryConfiguration.getPackageType()).thenReturn(null);

        when(_repositories.exists(_mainRepoPath)).thenReturn(false);
        when(_repositories.getLayoutInfo(_mainRepoPath)).thenReturn(fileLayoutInfo);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);

        var artifactChecker = _injector.getInstance(ArtifactChecker.class);

        artifactChecker.checkArtifact(_mainRepoPath);

        Mockito.verify(_logger, times(1)).error(Mockito.argThat(s -> s.contains("The artifact id was not built correctly.")));
        Mockito.verify(_scaHttpClient, never()).getArtifactInformation(isA(String.class), isA(String.class), isA(String.class));
        Mockito.verify(_scaHttpClient, never()).getVulnerabilitiesForArtifact(isA(ArtifactId.class));
    }

    @DisplayName("Failed to check artifact - failed to get artifact info")
    @Test
    public void failedToCheckArtifactFailedToGetArtifactInfo() throws ExecutionException, InterruptedException {

        var fileLayoutInfo = CreateFileLayoutInfoMock();

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);
        when(localRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);

        when(_repositories.exists(_mainRepoPath)).thenReturn(false);
        when(_repositories.getLayoutInfo(_mainRepoPath)).thenReturn(fileLayoutInfo);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);

        var exception = new InterruptedException("Test Exception");
        when(_scaHttpClient.getArtifactInformation(ArtifactType, ArtifactName, ArtifactVersion))
                .thenThrow(exception);

        var artifactChecker = _injector.getInstance(ArtifactChecker.class);

        artifactChecker.checkArtifact(_mainRepoPath);

        Mockito.verify(_logger, times(1)).error(Mockito.argThat(s -> s.contains(exception.getMessage())));
        Mockito.verify(_repositories, times(0)).setProperty(isA(RepoPath.class), isA(String.class), isA(String.class));
    }

    @DisplayName("Failed to check artifact - failed to get artifact info artifact not found")
    @Test
    public void failedToCheckArtifactFailedToGetArtifactInfoArtifactNotFound() throws ExecutionException, InterruptedException {

        var fileLayoutInfo = CreateFileLayoutInfoMock();

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);
        when(localRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);

        when(_repositories.exists(_mainRepoPath)).thenReturn(false);
        when(_repositories.getLayoutInfo(_mainRepoPath)).thenReturn(fileLayoutInfo);
        when(_repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);

        var exception = new UnexpectedResponseCodeException(404);
        when(_scaHttpClient.getArtifactInformation(ArtifactType, ArtifactName, ArtifactVersion))
                .thenThrow(exception);

        var artifactChecker = _injector.getInstance(ArtifactChecker.class);

        artifactChecker.checkArtifact(_mainRepoPath);

        Mockito.verify(_logger, times(1)).error(Mockito.argThat(s -> s.contains(exception.getMessage())));
        Mockito.verify(_repositories, never()).setProperty(isA(RepoPath.class), isA(String.class), isA(String.class));
    }

    @DisplayName("Failed to check artifact - failed to get vulnerabilities")
    @Test
    public void failedToCheckArtifactFailedToGetVulnerabilities() throws ExecutionException, InterruptedException {

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
        when(_scaHttpClient.getVulnerabilitiesForArtifact(artifactInfo.getId()))
                .thenThrow(exception);

        var artifactChecker = _injector.getInstance(ArtifactChecker.class);

        artifactChecker.checkArtifact(_mainRepoPath);

        Mockito.verify(_logger, times(1)).error(Mockito.argThat(s -> s.contains(exception.getMessage())));
        Mockito.verify(_repositories, times(1)).setProperty(isA(RepoPath.class), isA(String.class), isA(String.class));
    }

    private FileLayoutInfo CreateFileLayoutInfoMock(){
        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);
        when(fileLayoutInfo.getBaseRevision()).thenReturn(ArtifactVersion);
        when(fileLayoutInfo.getModule()).thenReturn(ArtifactName);
        return fileLayoutInfo;
    }

    private void MockScaHttpClientMethods() {
        try {
            var artifactInfo = CreateArtifactInfo();
            when(_scaHttpClient.getArtifactInformation(ArtifactType, ArtifactName, ArtifactVersion))
                    .thenReturn(artifactInfo);

            var vulnerabilities = CreateVulnerabilitiesList();
            when(_scaHttpClient.getVulnerabilitiesForArtifact(artifactInfo.getId()))
                    .thenReturn(vulnerabilities);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArtifactInfo CreateArtifactInfo(){
        return new Gson().fromJson("{\"id\":{\"identifier\":\"Npm-lodash-0.2.1\"},\"name\":\"lodash\",\"version\":\"0.2.1\",\"type\":\"Npm\",\"releaseDate\":\"2012-05-24T21:53:08\",\"description\":\"A drop-in replacement for Underscore.js that delivers performance improvements, bug fixes, and additional features.\",\"repositoryUrl\":\"git://github.com/bestiejs/lodash.git\",\"binaryUrl\":\"https://registry.npmjs.org/lodash/-/lodash-0.2.1.tgz\",\"projectUrl\":\"\",\"bugsUrl\":null,\"sourceUrl\":\"\",\"projectHomePage\":\"http://lodash.com\",\"homePage\":\"\",\"license\":\"\",\"summary\":\"\",\"url\":\"\",\"owner\":\"\"}", ArtifactInfo.class);
    }

    private ArrayList<Vulnerability> CreateVulnerabilitiesList(){
        Type listType = new TypeToken<ArrayList<Vulnerability>>(){}.getType();
        return new Gson().fromJson("[{\"id\":\"CVE-2020-28500\",\"cwe\":\"CWE-400\",\"description\":\"Lodash before 4.17.21 is vulnerable to Regular Expression Denial of Service (ReDoS) via the toNumber, trim and trimEnd functions.\",\"vulnerabilityType\":\"Regular\",\"publishDate\":\"2021-02-15T11:15:00\",\"score\":5.3,\"severity\":\"Medium\",\"created\":\"2021-03-04T08:29:31\",\"cveName\":\"CVE-2020-28500\",\"updateTime\":\"2022-01-07T06:04:48\"}]", listType);
    }

    private void loggerNeverCalled(){
        Mockito.verify(_logger, never()).info(isA(String.class));
        Mockito.verify(_logger, never()).info(Mockito.anyString(), isA(Exception.class));

        Mockito.verify(_logger, never()).error(isA(String.class));
        Mockito.verify(_logger, never()).error(isA(String.class), isA(Exception.class));

        Mockito.verify(_logger, never()).warn(isA(String.class));
        Mockito.verify(_logger, never()).warn(isA(String.class), isA(Exception.class));
    }
}
