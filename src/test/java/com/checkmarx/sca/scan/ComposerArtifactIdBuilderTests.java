package com.checkmarx.sca.scan;

import com.checkmarx.sca.PackageManager;
import com.checkmarx.sca.TestsInjector;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.artifactory.repo.RepoPath;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static org.mockito.Mockito.*;

@DisplayName("ComposerArtifactIdBuilder")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ComposerArtifactIdBuilderTests {
    private WireMockServer wireMockServer;
    private Injector _injector;
    private Logger _logger;

    @BeforeAll
    public void beforeAll() {
        this.wireMockServer = new WireMockServer();
        this.wireMockServer.start();
    }

    @BeforeEach
    public void beforeEach() {
        _logger = Mockito.mock(Logger.class);

        var properties = new Properties();
        properties.setProperty("packagist.repository", "http://localhost:8080");

        var configuration = new PluginConfiguration(properties, _logger);

        var artifactRisksFiller = Mockito.mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = Mockito.mock(SecurityThresholdChecker.class);

        var appInjector = new TestsInjector(_logger, configuration, artifactRisksFiller, securityThresholdChecker);

        _injector = Guice.createInjector(appInjector);
    }

    @AfterEach
    public void afterEach() {
        this.wireMockServer.resetAll();
    }

    @AfterAll
    public void afterAll() {
        this.wireMockServer.stop();
    }

    @DisplayName("Generate artifact id with success")
    @Test
    public void generateArtifactId() {
        this.wireMockServer.stubFor(
                WireMock.get("/p2/zircote/swagger-php.json")
                        .willReturn(ok()
                                .withBody("{\"minified\":\"composer/2.0\",\"packages\":{\"zircote/swagger-php\":[{\"version\":\"4.4.4\",\"version_normalized\":\"4.4.4.0\",\"source\":{\"url\":\"https://github.com/zircote/swagger-php.git\",\"type\":\"git\",\"reference\":\"fb967b3ef9e311626e7232fa71096763d5f3eec2\"}},{\"version\":\"4.4.3\",\"version_normalized\":\"4.4.3.0\",\"source\":{\"url\":\"https://github.com/zircote/swagger-php.git\",\"type\":\"git\",\"reference\":\"05e3cb201dd4b08c5263193bd48841e3a4ca22a0\"}},{\"version\":\"4.4.2\",\"version_normalized\":\"4.4.2.0\",\"source\":{\"url\":\"https://github.com/zircote/swagger-php.git\",\"type\":\"git\",\"reference\":\"1653b53eb3973a9a6dac87ef8f2676801b14469f\"}},{\"version\":\"3.1.0\",\"version_normalized\":\"3.1.0.0\",\"source\":{\"url\":\"https://github.com/zircote/swagger-php.git\",\"type\":\"git\",\"reference\":\"9d172471e56433b5c7061006b9a766f262a3edfd\"}}]}}"))
        );

        var repoPath = Mockito.mock(RepoPath.class);
        when(repoPath.getPath()).thenReturn("zircote/swagger-php/commits/9d172471e56433b5c7061006b9a766f262a3edfd/swagger-php-9d172471e56433b5c7061006b9a766f262a3edfd.zip");

        var composerArtifactIdBuilder = _injector.getInstance(ComposerArtifactIdBuilder.class);

        var id = composerArtifactIdBuilder.generateArtifactId(repoPath, PackageManager.COMPOSER);

        Assertions.assertEquals("zircote/swagger-php", id.Name);
        Assertions.assertEquals("3.1.0", id.Version);
        Assertions.assertEquals("php", id.PackageType);

        Mockito.verify(_logger, never()).error(isA(String.class));
        Mockito.verify(_logger, never()).error(isA(String.class), isA(Exception.class));

        Mockito.verify(_logger, never()).warn(isA(String.class));
        Mockito.verify(_logger, never()).warn(isA(String.class), isA(Exception.class));
    }

    @DisplayName("Generate artifact id - fail to parse RepoPath")
    @Test
    public void generateArtifactIdFailRepoPath() {
        var repoPath = Mockito.mock(RepoPath.class);
        when(repoPath.getPath()).thenReturn("zircote/swagger-php-9d172471e56433b5c7061006b9a766f262a3edfd.zip");

        var composerArtifactIdBuilder = _injector.getInstance(ComposerArtifactIdBuilder.class);

        var id = composerArtifactIdBuilder.generateArtifactId(repoPath, PackageManager.COMPOSER);

        Assertions.assertNull(id.Name);
        Assertions.assertNull(id.Version);
        Assertions.assertEquals("php", id.PackageType);

        Mockito.verify(_logger, times(1)).error(isA(String.class));
    }

    @DisplayName("Generate artifact id - unexpected status code")
    @Test
    public void generateArtifactIdUnexpectedStatusCode() {
        this.wireMockServer.stubFor(
                WireMock.get("/p2/zircote/swagger-php.json")
                        .willReturn(badRequest())
        );

        var repoPath = Mockito.mock(RepoPath.class);
        when(repoPath.getPath()).thenReturn("zircote/swagger-php/commits/9d172471e56433b5c7061006b9a766f262a3edfd/swagger-php-9d172471e56433b5c7061006b9a766f262a3edfd.zip");

        var composerArtifactIdBuilder = _injector.getInstance(ComposerArtifactIdBuilder.class);

        var id = composerArtifactIdBuilder.generateArtifactId(repoPath, PackageManager.COMPOSER);

        Assertions.assertNull(id.Name);
        Assertions.assertNull(id.Version);
        Assertions.assertEquals("php", id.PackageType);

        Mockito.verify(_logger, times(1)).warn(isA(String.class));
    }

    @DisplayName("Generate artifact id - unexpected body")
    @Test
    public void generateArtifactIdUnexpectedBody() {
        this.wireMockServer.stubFor(
                WireMock.get("/p2/zircote/swagger-php.json")
                        .willReturn(ok())
        );

        var repoPath = Mockito.mock(RepoPath.class);
        when(repoPath.getPath()).thenReturn("zircote/swagger-php/commits/9d172471e56433b5c7061006b9a766f262a3edfd/swagger-php-9d172471e56433b5c7061006b9a766f262a3edfd.zip");

        var composerArtifactIdBuilder = _injector.getInstance(ComposerArtifactIdBuilder.class);

        var id = composerArtifactIdBuilder.generateArtifactId(repoPath, PackageManager.COMPOSER);

        Assertions.assertNull(id.Name);
        Assertions.assertNull(id.Version);
        Assertions.assertEquals("php", id.PackageType);

        Mockito.verify(_logger, times(1)).error(isA(String.class));
    }
}
