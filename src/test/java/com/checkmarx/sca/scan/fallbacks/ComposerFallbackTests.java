package com.checkmarx.sca.scan.fallbacks;

import com.checkmarx.sca.TestsInjector;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.scan.ArtifactRisksFiller;
import com.checkmarx.sca.scan.SecurityThresholdChecker;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static java.lang.String.format;

@DisplayName("ComposerFallbackTests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ComposerFallbackTests {

    private WireMockServer wireMockServer;
    private Injector _injector;

    @BeforeAll
    public void beforeAll() {
        this.wireMockServer = new WireMockServer();
        this.wireMockServer.start();
    }

    @BeforeEach
    public void beforeEach() {
        var logger = Mockito.mock(Logger.class);

        var properties = new Properties();
        properties.setProperty("packagist.repository", "http://localhost:8080");

        var configuration = new PluginConfiguration(properties, logger);

        var artifactRisksFiller = Mockito.mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = Mockito.mock(SecurityThresholdChecker.class);

        var appInjector = new TestsInjector(logger, configuration, artifactRisksFiller, securityThresholdChecker);

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

    @DisplayName("Apply Fallback System with success")
    @ParameterizedTest
    @CsvSource({
            "php-fig/cache,cache,psr/cache",
            "php-fig/cache/cache,cache/cache,",
            "psr/cache,cache,",
            "cache,,"
    })
    public void applyFallbackWithSuccess(String name, String search, String expected) {
        this.wireMockServer.stubFor(
                WireMock.get(format("/search.json?q=%s", search))
                        .willReturn(ok()
                                .withBody("{\"results\":[{\"name\":\"psr/simple-cache\",\"description\":\"Common interfaces for simple caching\",\"url\":\"https://packagist.org/packages/psr/simple-cache\",\"repository\":\"https://github.com/php-fig/simple-cache\",\"downloads\":256246190,\"favers\":7887},{\"name\":\"psr/cache\",\"description\":\"Common interface for caching libraries\",\"url\":\"https://packagist.org/packages/psr/cache\",\"repository\":\"https://github.com/php-fig/cache\",\"downloads\":220523080,\"favers\":4681},{\"name\":\"doctrine/cache\",\"description\":\"PHP Doctrine Cache library is a popular cache implementation that supports many different drivers such as redis, memcache, apc, mongodb and others.\",\"url\":\"https://packagist.org/packages/doctrine/cache\",\"repository\":\"https://github.com/doctrine/cache\",\"downloads\":278504199,\"favers\":7507},{\"name\":\"symfony/cache\",\"description\":\"Provides an extended PSR-6, PSR-16 (and tags) implementation\",\"url\":\"https://packagist.org/packages/symfony/cache\",\"repository\":\"https://github.com/symfony/cache\",\"downloads\":97729887,\"favers\":3738}],\"total\":4}"))
        );

        var composerFallback = _injector.getInstance(ComposerFallback.class);

        var actual = composerFallback.applyFallback(name);

        Assertions.assertEquals(expected, actual);
    }

    @DisplayName("Apply Fallback unexpected status code")
    @Test
    public void applyFallbackUnexpectedStatusCode() {

        this.wireMockServer.stubFor(
                WireMock.get("/search.json?q=cache")
                        .willReturn(badRequest())
        );

        var composerFallback = _injector.getInstance(ComposerFallback.class);

        var actual = composerFallback.applyFallback("php-fig/cache");

        Assertions.assertNull(actual);
    }

    @DisplayName("Apply Fallback empty body")
    @Test
    public void applyFallbackEmptyBody() {

        this.wireMockServer.stubFor(
                WireMock.get("/search.json?q=cache")
                        .willReturn(ok()
                                .withBody("{\"results\":[],\"total\":0}"))
        );

        var composerFallback = _injector.getInstance(ComposerFallback.class);

        var actual = composerFallback.applyFallback("php-fig/cache");

        Assertions.assertNull(actual);
    }

    @DisplayName("Apply Fallback unexpected body")
    @Test
    public void applyFallbackUnexpectedBody() {

        this.wireMockServer.stubFor(
                WireMock.get("/search.json?q=cache")
                        .willReturn(ok()
                                .withBody(""))
        );

        var composerFallback = _injector.getInstance(ComposerFallback.class);

        var actual = composerFallback.applyFallback("php-fig/cache");

        Assertions.assertNull(actual);
    }
}
