package com.checkmarx.sca;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.artifactory.exception.CancelException;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static com.checkmarx.sca.PropertiesConstants.HIGH_RISKS_COUNT;
import static com.checkmarx.sca.PropertiesConstants.IGNORE_THRESHOLD;
import static com.checkmarx.sca.suggestion.PrivatePackageSuggestionHandler.SUGGESTED_KEY;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class ScaPluginTest {
    private final String RepoKey = "test-remote";

    private final String ArtifactType = "npm";
    private final String ArtifactName = "lodash";
    private final String ArtifactVersion = "0.2.1";


    @DisplayName("Check artifact with success - non virtual repository")
    @Test
    public void checkArtifactWithSuccess() {

        String path = "src/test/resources/com/checkmarx/sca/configuration";
        File resourcesDir = new File(path);

        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);
        when(fileLayoutInfo.getBaseRevision()).thenReturn(ArtifactVersion);
        when(fileLayoutInfo.getModule()).thenReturn(ArtifactName);
        when(fileLayoutInfo.isValid()).thenReturn(true);

        var repoPath = Mockito.mock(RepoPath.class);
        when(repoPath.getRepoKey()).thenReturn(RepoKey);
        when(repoPath.getName()).thenReturn(format("%s-%s", ArtifactName, ArtifactVersion));
        when(repoPath.getPath()).thenReturn(format("%s/-/%s-%s.tgz", ArtifactName, ArtifactName, ArtifactVersion));

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);
        when(localRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);

        var repositories = Mockito.mock(Repositories.class);
        when(repositories.exists(repoPath)).thenReturn(false);
        when(repositories.getLayoutInfo(repoPath)).thenReturn(fileLayoutInfo);
        when(repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);
        when(repositories.setProperty(isA(RepoPath.class), isA(String.class), isA(String.class))).thenReturn(null);

        when(repositories.getProperty(repoPath, HIGH_RISKS_COUNT)).thenReturn("0");
        MockGetProperties(repositories, repoPath, "false");

        var logger = Mockito.mock(Logger.class);

        try {
            var scaPlugin = new ScaPlugin(logger, resourcesDir, repositories);
            scaPlugin.beforeDownload(repoPath);
        } catch (IOException e) {
            Assertions.fail();
        }

        Mockito.verify(logger, never()).error(isA(String.class));
        Mockito.verify(logger, never()).error(isA(String.class), isA(Exception.class));

        Mockito.verify(logger, never()).warn(isA(String.class));
        Mockito.verify(logger, never()).warn(isA(String.class), isA(Exception.class));
    }

    @DisplayName("Check artifact with success - virtual repository")
    @Test
    public void checkArtifactWithSuccessVirtualRepository() {

        String path = "src/test/resources/com/checkmarx/sca/configuration";
        File resourcesDir = new File(path);

        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);
        when(fileLayoutInfo.getBaseRevision()).thenReturn(ArtifactVersion);
        when(fileLayoutInfo.getModule()).thenReturn(ArtifactName);
        when(fileLayoutInfo.isValid()).thenReturn(true);

        var virtualPath = Mockito.mock(RepoPath.class);
        when(virtualPath.getRepoKey()).thenReturn(RepoKey);
        when(virtualPath.getName()).thenReturn(format("%s-%s", ArtifactName, ArtifactVersion));
        when(virtualPath.getPath()).thenReturn(format("%s/-/%s-%s.tgz", ArtifactName, ArtifactName, ArtifactVersion));

        var repoPath = Mockito.mock(RepoPath.class);
        var virtualRepositoryConfiguration = Mockito.mock(VirtualRepositoryConfiguration.class);
        when(virtualRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);
        when(virtualRepositoryConfiguration.getRepositories()).thenReturn(List.of("xyz", "abc"));

        var repositories = Mockito.mock(Repositories.class);
        when(repositories.exists(virtualPath)).thenReturn(false);
        when(repositories.exists(repoPath)).thenReturn(true);
        when(repositories.getLayoutInfo(virtualPath)).thenReturn(fileLayoutInfo);
        when(repositories.getRepositoryConfiguration(RepoKey)).thenReturn(virtualRepositoryConfiguration);
        when(repositories.setProperty(isA(RepoPath.class), isA(String.class), isA(String.class))).thenReturn(null);

        when(repositories.getProperty(repoPath, HIGH_RISKS_COUNT)).thenReturn("0");
        MockGetProperties(repositories, repoPath, "false");

        var logger = Mockito.mock(Logger.class);

        try(MockedStatic<RepoPathFactory> utilities = Mockito.mockStatic(RepoPathFactory.class)){
            utilities.when(()-> RepoPathFactory.create("xyz", virtualPath.getPath()))
                .thenReturn(repoPath);

            try {
                var scaPlugin = new ScaPlugin(logger, resourcesDir, repositories);
                scaPlugin.beforeDownload(virtualPath);
            } catch (IOException e) {
                Assertions.fail();
            }
        }

        Mockito.verify(logger, never()).error(isA(String.class));
        Mockito.verify(logger, never()).error(isA(String.class), isA(Exception.class));

        Mockito.verify(logger, never()).warn(isA(String.class));
        Mockito.verify(logger, never()).warn(isA(String.class), isA(Exception.class));
    }

    @DisplayName("Check artifact with success - ignoring security risk threshold")
    @Test
    public void checkArtifactWithSuccessIgnoreSecurityRiskThreshold() {

        String path = "src/test/resources/com/checkmarx/sca/configuration";
        File resourcesDir = new File(path);

        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);
        when(fileLayoutInfo.getBaseRevision()).thenReturn(ArtifactVersion);
        when(fileLayoutInfo.getModule()).thenReturn(ArtifactName);
        when(fileLayoutInfo.isValid()).thenReturn(true);

        var repoPath = Mockito.mock(RepoPath.class);
        when(repoPath.getRepoKey()).thenReturn(RepoKey);
        when(repoPath.getName()).thenReturn(format("%s-%s", ArtifactName, ArtifactVersion));
        when(repoPath.getPath()).thenReturn(format("%s/-/%s-%s.tgz", ArtifactName, ArtifactName, ArtifactVersion));

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);
        when(localRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);

        var repositories = Mockito.mock(Repositories.class);
        when(repositories.exists(repoPath)).thenReturn(false);
        when(repositories.getLayoutInfo(repoPath)).thenReturn(fileLayoutInfo);
        when(repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);
        when(repositories.setProperty(isA(RepoPath.class), isA(String.class), isA(String.class))).thenReturn(null);

        MockGetProperties(repositories, repoPath, "true");

        var logger = Mockito.mock(Logger.class);

        try {
            var scaPlugin = new ScaPlugin(logger, resourcesDir, repositories);
            scaPlugin.beforeDownload(repoPath);
        } catch (IOException e) {
            Assertions.fail();
        }

        Mockito.verify(logger, never()).error(isA(String.class));
        Mockito.verify(logger, never()).error(isA(String.class), isA(Exception.class));

        Mockito.verify(logger, times(1)).warn(Mockito.argThat(s -> s.contains(IGNORE_THRESHOLD)));
        Mockito.verify(logger, never()).warn(isA(String.class), isA(Exception.class));
    }

    @DisplayName("Check artifact with success - download blocked security risk threshold")
    @Test
    public void checkArtifactWithSuccessDownloadBlocked() {
        String path = "src/test/resources/com/checkmarx/sca/configuration";
        File resourcesDir = new File(path);

        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);
        when(fileLayoutInfo.getBaseRevision()).thenReturn(ArtifactVersion);
        when(fileLayoutInfo.getModule()).thenReturn(ArtifactName);
        when(fileLayoutInfo.isValid()).thenReturn(true);

        var repoPath = Mockito.mock(RepoPath.class);
        when(repoPath.getRepoKey()).thenReturn(RepoKey);
        when(repoPath.getName()).thenReturn(format("%s-%s", ArtifactName, ArtifactVersion));
        when(repoPath.getPath()).thenReturn(format("%s/-/%s-%s.tgz", ArtifactName, ArtifactName, ArtifactVersion));

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);
        when(localRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);

        var repositories = Mockito.mock(Repositories.class);
        when(repositories.exists(repoPath)).thenReturn(false);
        when(repositories.getLayoutInfo(repoPath)).thenReturn(fileLayoutInfo);
        when(repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);
        when(repositories.setProperty(isA(RepoPath.class), isA(String.class), isA(String.class))).thenReturn(null);
        when(repositories.getProperty(repoPath, HIGH_RISKS_COUNT)).thenReturn("1");

        MockGetProperties(repositories, repoPath, "false");

        var logger = Mockito.mock(Logger.class);

        try {
            var scaPlugin = new ScaPlugin(logger, resourcesDir, repositories);

            var exception = Assertions.assertThrows(CancelException.class, () -> scaPlugin.beforeDownload(repoPath));

            Assertions.assertEquals(exception.getErrorCode(), 403);
        } catch (IOException e) {
            Assertions.fail();
        }

        Mockito.verify(logger, never()).error(isA(String.class));
        Mockito.verify(logger, never()).error(isA(String.class), isA(Exception.class));

        Mockito.verify(logger, never()).warn(isA(String.class));
        Mockito.verify(logger, never()).warn(isA(String.class), isA(Exception.class));
    }


    @DisplayName("Check if upload artifact makes suggestion")
    @Test
    public void uploadArtifactMakeSuggestion() {

        var wireMockServer = new WireMockServer();
        wireMockServer.start();
        wireMockServer.stubFor(
                WireMock.post("/private-dependencies-repository/dependencies")
                        .withRequestBody(containing("[{\"name\":\"lodash\",\"packageManager\":\"npm\",\"version\":\"0.2.1\",\"origin\":\"PrivateArtifactory\"}]"))
                        .willReturn(ok())
        );

        String token = "eyJhbGciOiJSUzI1NiIsC6";
        wireMockServer.stubFor(
                WireMock.post("/identity/connect/token")
                        .willReturn(ok()
                                .withHeader("Content-Type", "application/json; charset=UTF-8")
                                .withBody(format("{\"access_token\":\"%s\",\"expires_in\":3600,\"token_type\":\"Bearer\"}", token)))
        );

        try {
            String path = "src/test/resources/com/checkmarx/sca/configuration-local";
            File resourcesDir = new File(path);

            var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);
            when(fileLayoutInfo.getBaseRevision()).thenReturn(ArtifactVersion);
            when(fileLayoutInfo.getModule()).thenReturn(ArtifactName);
            when(fileLayoutInfo.isValid()).thenReturn(true);

            var repoPath = Mockito.mock(RepoPath.class);
            when(repoPath.getRepoKey()).thenReturn(RepoKey);
            when(repoPath.getName()).thenReturn(format("%s-%s", ArtifactName, ArtifactVersion));
            when(repoPath.getPath()).thenReturn(format("%s/-/%s-%s.tgz", ArtifactName, ArtifactName, ArtifactVersion));

            var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);
            when(localRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);

            var repositories = Mockito.mock(Repositories.class);
            when(repositories.exists(repoPath)).thenReturn(false);
            when(repositories.getLayoutInfo(repoPath)).thenReturn(fileLayoutInfo);
            when(repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);

            var properties = Mockito.mock(org.artifactory.md.Properties.class);
            when(properties.containsKey(SUGGESTED_KEY)).thenReturn(true);

            when(repositories.setProperty(isA(RepoPath.class), isA(String.class), isA(String.class))).thenReturn(properties);

            var logger = Mockito.mock(Logger.class);

            try {
                var scaPlugin = new ScaPlugin(logger, resourcesDir, repositories);
                scaPlugin.beforeUpload(repoPath);
            } catch (IOException e) {
                Assertions.fail();
            }

            Mockito.verify(logger, never()).error(isA(String.class));
            Mockito.verify(logger, never()).error(isA(String.class), isA(Exception.class));

            Mockito.verify(logger, never()).warn(isA(String.class));
            Mockito.verify(logger, never()).warn(isA(String.class), isA(Exception.class));

            wireMockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/identity/connect/token")));
            wireMockServer.verify(exactly(1), postRequestedFor(urlEqualTo("/private-dependencies-repository/dependencies")));
        } finally {
            wireMockServer.stop();
        }
    }

    private void MockGetProperties(Repositories repositories, RepoPath repoPath, String value) {
        var propertiesMap = new HashMap<String, String>();
        propertiesMap.put(IGNORE_THRESHOLD, value);

        var properties = Mockito.mock(org.artifactory.md.Properties.class);

        when(properties.entries()).thenReturn(propertiesMap.entrySet());
        when(repositories.getProperties(repoPath)).thenReturn(properties);
    }
}
