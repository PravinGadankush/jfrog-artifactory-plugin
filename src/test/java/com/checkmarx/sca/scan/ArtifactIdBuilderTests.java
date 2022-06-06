package com.checkmarx.sca.scan;

import com.checkmarx.sca.PackageManager;
import com.checkmarx.sca.TestsInjector;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.artifactory.fs.FileLayoutInfo;
import org.artifactory.repo.RepoPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.Properties;

import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class ArtifactIdBuilderTests {

    private Injector _injector;
    private Logger _logger;

    @BeforeEach
    public void beforeEach() {
        _logger = Mockito.mock(Logger.class);
        var configuration = new PluginConfiguration(new Properties(), _logger);
        var artifactRisksFiller = Mockito.mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = Mockito.mock(SecurityThresholdChecker.class);

        var appInjector = new TestsInjector(_logger, configuration, artifactRisksFiller, securityThresholdChecker);

        _injector = Guice.createInjector(appInjector);
    }

    @DisplayName("Get artifact id with success - NPM")
    @ParameterizedTest
    @CsvSource({"@types/fs-extra,9.0.13", "http,0.0.1-security"})
    public void getNpmArtifactIdWithSuccessWithComplexVersion(String name, String version) {

        var artifactIdBuilder = _injector.getInstance(ArtifactIdBuilder.class);
        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);

        when(fileLayoutInfo.isValid()).thenReturn(true);

        var repoPath = Mockito.mock(RepoPath.class);
        when(repoPath.getPath()).thenReturn(String.format("%1$s/-/%1$s-%2$s.tgz", name, version));

        var id = artifactIdBuilder.getArtifactId(fileLayoutInfo, repoPath, PackageManager.NPM);

        Assertions.assertEquals(name, id.Name);
        Assertions.assertEquals(version, id.Version);
        Assertions.assertEquals("npm", id.PackageType);
    }

    @DisplayName("Get artifact id with success - Maven")
    @Test
    public void getMavenArtifactIdWithSuccess() {

        var artifactIdBuilder = _injector.getInstance(ArtifactIdBuilder.class);
        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);

        when(fileLayoutInfo.getBaseRevision()).thenReturn("1.2.3");
        when(fileLayoutInfo.getOrganization()).thenReturn("org");
        when(fileLayoutInfo.getModule()).thenReturn("test");
        when(fileLayoutInfo.isValid()).thenReturn(true);

        var repoPath = Mockito.mock(RepoPath.class);

        var id = artifactIdBuilder.getArtifactId(fileLayoutInfo, repoPath, PackageManager.MAVEN);

        Assertions.assertEquals("org:test", id.Name);
        Assertions.assertEquals("1.2.3", id.Version);
        Assertions.assertEquals("maven", id.PackageType);
    }

    @DisplayName("Get artifact id with success - Maven with file revision")
    @Test
    public void getMavenArtifactIdWithSuccessWithFileRevision() {

        var artifactIdBuilder = _injector.getInstance(ArtifactIdBuilder.class);
        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);

        when(fileLayoutInfo.getBaseRevision()).thenReturn("1.2.3");
        when(fileLayoutInfo.getOrganization()).thenReturn("org");
        when(fileLayoutInfo.getFileIntegrationRevision()).thenReturn("2");
        when(fileLayoutInfo.getModule()).thenReturn("test");
        when(fileLayoutInfo.isValid()).thenReturn(true);

        var repoPath = Mockito.mock(RepoPath.class);

        var id = artifactIdBuilder.getArtifactId(fileLayoutInfo, repoPath, PackageManager.MAVEN);

        Assertions.assertEquals("org:test", id.Name);
        Assertions.assertEquals("1.2.3-2", id.Version);
        Assertions.assertEquals("maven", id.PackageType);
    }

    @DisplayName("Get artifact id with success - Python")
    @Test
    public void getPythonArtifactIdWithSuccess() {

        var artifactIdBuilder = _injector.getInstance(ArtifactIdBuilder.class);
        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);

        var repoPath = Mockito.mock(RepoPath.class);
        when(repoPath.getPath()).thenReturn("pip-remote-cache/51/bd/23c926cd341ea6b7dd0b2a00aba99ae0f828be89d72b2190f27c11d4b7fb/requests-2.22.0-py2.py3-none-any.whl");

        var id = artifactIdBuilder.getArtifactId(fileLayoutInfo, repoPath, PackageManager.PYPI);

        Assertions.assertEquals("requests", id.Name);
        Assertions.assertEquals("2.22.0", id.Version);
        Assertions.assertEquals("python", id.PackageType);
    }

    @DisplayName("Get artifact id with success - Nuget")
    @Test
    public void getNugetArtifactIdWithSuccess() {

        var artifactIdBuilder = _injector.getInstance(ArtifactIdBuilder.class);
        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);

        var repoPath = Mockito.mock(RepoPath.class);
        when(repoPath.getPath()).thenReturn("dbup-core.4.5.0.nupkg");

        var id = artifactIdBuilder.getArtifactId(fileLayoutInfo, repoPath, PackageManager.NUGET);

        Assertions.assertEquals("dbup-core", id.Name);
        Assertions.assertEquals("4.5.0", id.Version);
        Assertions.assertEquals("nuget", id.PackageType);
    }

    @DisplayName("Fail to get artifact id by regex")
    @Test
    public void failToGetArtifactIdByRegex() {

        var artifactIdBuilder = _injector.getInstance(ArtifactIdBuilder.class);
        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);

        var path = "path";
        var repoPath = Mockito.mock(RepoPath.class);
        when(repoPath.getName()).thenReturn("name");
        when(repoPath.getPath()).thenReturn(path);

        var packageManager = PackageManager.COCOAPODS;
        artifactIdBuilder.getArtifactId(fileLayoutInfo, repoPath, packageManager);

        Mockito.verify(_logger, times(1)).info(Mockito.argThat(s -> s.contains(packageManager.packageType())));
        Mockito.verify(_logger, times(1)).debug(Mockito.argThat(s -> s.contains(path)));
    }

    @DisplayName("Get artifact id with success - Composer")
    @Test
    public void getComposerArtifactIdWithSuccess() {

        var artifactIdBuilder = _injector.getInstance(ArtifactIdBuilder.class);
        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);

        var repoPath = Mockito.mock(RepoPath.class);
        when(repoPath.getPath()).thenReturn("zircote/swagger-php/commits/9d172471e56433b5c7061006b9a766f262a3edfd/swagger-php-9d172471e56433b5c7061006b9a766f262a3edfd.zip");

        var id = artifactIdBuilder.getArtifactId(fileLayoutInfo, repoPath, PackageManager.COMPOSER);

        Assertions.assertEquals("zircote/swagger-php", id.Name);
        Assertions.assertEquals("3.1.0", id.Version);
        Assertions.assertEquals("php", id.PackageType);
    }

    @DisplayName("Get artifact id with success - GO")
    @ParameterizedTest
    @CsvSource(
            {
                "h12.io/socks/@v/v1.0.1.zip, h12.io/socks,v1.0.1",
                "github.com/google/go-github/@v/v17.0.0+incompatible.zip, github.com/google/go-github, v17.0.0",
                "github.com/golang/glog/@v/v0.0.0-20160126235308-23def4e6c14b.zip, github.com/golang/glog, v0.0.0-20160126235308-23def4e6c14b"
            })
    public void getGoArtifactIdWithSuccess(String path, String name, String version) {

        var artifactIdBuilder = _injector.getInstance(ArtifactIdBuilder.class);
        var fileLayoutInfo = Mockito.mock(FileLayoutInfo.class);

        when(fileLayoutInfo.isValid()).thenReturn(true);

        var repoPath = Mockito.mock(RepoPath.class);
        when(repoPath.getPath()).thenReturn(path);

        var id = artifactIdBuilder.getArtifactId(fileLayoutInfo, repoPath, PackageManager.GO);

        Assertions.assertEquals(name, id.Name);
        Assertions.assertEquals(version, id.Version);
        Assertions.assertEquals("go", id.PackageType);
    }
}
