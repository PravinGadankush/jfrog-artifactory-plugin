package com.checkmarx.sca;

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

    private void MockGetProperties(Repositories repositories, RepoPath repoPath, String value) {
        var propertiesMap = new HashMap<String, String>();
        propertiesMap.put(IGNORE_THRESHOLD, value);

        var properties = Mockito.mock(org.artifactory.md.Properties.class);

        when(properties.entries()).thenReturn(propertiesMap.entrySet());
        when(repositories.getProperties(repoPath)).thenReturn(properties);
    }
}
