package com.checkmarx.sca;

import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.LocalRepositoryConfiguration;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import static java.lang.String.format;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

public class ScaPluginTest {
    private final String RepoKey = "test-remote";

    private final String ArtifactType = "npm";
    private final String ArtifactName = "lodash";
    private final String ArtifactVersion = "0.2.1";


    @DisplayName("Check artifact with success")
    @Test
    public void checkArtifactWithSuccess() throws ExecutionException {

        String path = "src/test/resources/com/checkmarx/sca/configuration";
        File resourcesDir = new File(path);

        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);
        when(fileLayoutInfo.getBaseRevision()).thenReturn(ArtifactVersion);
        when(fileLayoutInfo.getModule()).thenReturn(ArtifactName);

        var repoPath = Mockito.mock(RepoPath.class);
        when(repoPath.getRepoKey()).thenReturn(RepoKey);
        when(repoPath.getName()).thenReturn(format("%s-%s", ArtifactName, ArtifactVersion));
        when(repoPath.getPath()).thenReturn(format("%s/-/%s-%s.tgz\"", ArtifactName, ArtifactVersion, ArtifactVersion));

        var localRepositoryConfiguration = Mockito.mock(LocalRepositoryConfiguration.class);
        when(localRepositoryConfiguration.getPackageType()).thenReturn(ArtifactType);

        var repositories = Mockito.mock(Repositories.class);
        when(repositories.exists(repoPath)).thenReturn(false);
        when(repositories.getLayoutInfo(repoPath)).thenReturn(fileLayoutInfo);
        when(repositories.getRepositoryConfiguration(RepoKey)).thenReturn(localRepositoryConfiguration);
        when(repositories.setProperty(isA(RepoPath.class), isA(String.class), isA(String.class))).thenReturn(null);

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
}
