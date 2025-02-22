package com.checkmarx.sca.communication;

import com.checkmarx.sca.TestsInjector;
import com.checkmarx.sca.communication.exceptions.UnexpectedResponseBodyException;
import com.checkmarx.sca.communication.exceptions.UnexpectedResponseCodeException;
import com.checkmarx.sca.communication.models.AuthenticationHeader;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.models.ArtifactId;
import com.checkmarx.sca.scan.ArtifactRisksFiller;
import com.checkmarx.sca.scan.SecurityThresholdChecker;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;

@DisplayName("ScaHttpClient")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ScaHttpClientTests {

    private WireMockServer wireMockServer;

    @BeforeAll
    public void beforeAll() {
        this.wireMockServer = new WireMockServer();
        this.wireMockServer.start();
    }

    @AfterEach
    public void afterEach() {
        this.wireMockServer.resetAll();
    }

    @AfterAll
    public void afterAll() {
        this.wireMockServer.stop();
    }

    @DisplayName("Get artifact information with success")
    @Test
    public void getArtifactInformationWithSuccess() throws ExecutionException, InterruptedException {

        this.wireMockServer.stubFor(
                WireMock.get("/public/packages/Npm/lodash/versions/0.2.1")
                        .withHeader("cxorigin", matching("JFrog .*"))
                        .willReturn(ok()
                                .withHeader("Content-Type", "application/json; charset=UTF-8")
                                .withBody("{\"packageId\":\"Npm#-#lodash#-#0.2.1\",\"legacyPackageId\":\"Npm-lodash-0.2.1\",\"type\":\"Npm\",\"name\":\"lodash\",\"version\":\"0.2.1\",\"description\":\"A drop-in replacement for Underscore.js that delivers performance improvements\",\"projectHomePage\":\"http://lodash.com\",\"releaseDate\":\"2012-05-24T21:53:08.449+00:00\",\"keywords\":[\"browser\"],\"authors\":\"John-David Dalton\",\"binaryUrl\":\"https://registry.npmjs.org/lodash/-/lodash-0.2.1.tgz\",\"mainFile\":\"lodash\",\"authorUrl\":\"http://allyoucanleet.com/\",\"authorEmail\":\"john@fusejs.com\",\"bugsUrl\":\"https://github.com/bestiejs/lodash/issues\",\"dependencies\":\"{}\",\"sourceRepository\":\"git://github.com/bestiejs/lodash.git\",\"repositoryType\":\"git\",\"preRelease\":false,\"requirements\":[],\"source\":\"NPM\"}"))
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        var artifactInfo = scaHttpClient.getArtifactInformation("Npm", "lodash", "0.2.1");

        Assertions.assertNotNull(artifactInfo);
        Assertions.assertEquals("Npm-lodash-0.2.1", artifactInfo.getId());
    }

    @DisplayName("Get artifact information with success after PyPi retry")
    @Test
    public void getArtifactInformationWithSuccessAfterPyPiRetry() throws ExecutionException, InterruptedException {

        this.wireMockServer.stubFor(
                WireMock.get("/public/packages/python/google_parser/versions/0.0.135")
                        .withHeader("cxorigin", matching("JFrog .*"))
                        .willReturn(aResponse().withStatus(404))
        );

        this.wireMockServer.stubFor(
                WireMock.get("/public/packages/python/google-parser/versions/0.0.135")
                        .withHeader("cxorigin", matching("JFrog .*"))
                        .willReturn(ok()
                                .withHeader("Content-Type", "application/json; charset=UTF-8")
                                .withBody("{\"packageId\":\"Python#-#google-parser#-#0.0.135\",\"legacyPackageId\":\"Python-google-parser-0.0.135\",\"type\":\"Python\",\"name\":\"google-parser\",\"version\":\"0.0.135\",\"projectHomePage\":\"https://github.com/KokocGroup/google-parser\",\"releaseDate\":\"2021-11-22T11:43:16+00:00\",\"keywords\":[],\"projectUrl\":\"https://pypi.org/project/google-parser/\",\"sourceRepository\":\"https://github.com/KokocGroup/google-parser\",\"preRelease\":false,\"requirements\":[],\"packageFiles\":\"\"}"))
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        var artifactInfo = scaHttpClient.getArtifactInformation("python", "google_parser", "0.0.135");

        Assertions.assertNotNull(artifactInfo);
        Assertions.assertEquals("Python-google-parser-0.0.135", artifactInfo.getId());
    }

    @DisplayName("Failed to get artifact information - Not found")
    @Test
    public void failedToGetArtifactInformationNotFound() {

        this.wireMockServer.stubFor(
                WireMock.get("/public/packages/Npm/lodash/versions/0.2.1")
                        .withHeader("cxorigin", matching("JFrog .*"))
                        .willReturn(aResponse().withStatus(404))
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        var exception = Assertions.assertThrows(UnexpectedResponseCodeException.class, () -> scaHttpClient.getArtifactInformation("Npm", "lodash", "0.2.1"));

        Assertions.assertEquals(404, exception.StatusCode);
    }

    @DisplayName("Failed to get artifact information - Unexpected Response Code")
    @Test
    public void failed() {

        this.wireMockServer.stubFor(
                WireMock.get("/public/packages/Npm/lodash/versions/0.2.1")
                        .withHeader("cxorigin", matching("JFrog .*"))
                        .willReturn(aResponse().withStatus(500))
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        var exception = Assertions.assertThrows(UnexpectedResponseCodeException.class, () -> scaHttpClient.getArtifactInformation("Npm", "lodash", "0.2.1"));

        Assertions.assertEquals(500, exception.StatusCode);
    }

    @DisplayName("Failed to get artifact information - Unexpected Response Body")
    @Test
    public void failedToGetArtifactInformationUnexpectedResponseBody() {

        this.wireMockServer.stubFor(
                WireMock.get("/public/packages/Npm/lodash/versions/0.2.1")
                        .withHeader("cxorigin", matching("JFrog .*"))
                        .willReturn(ok())
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        Assertions.assertThrows(UnexpectedResponseBodyException.class, () -> scaHttpClient.getArtifactInformation("Npm", "lodash", "0.2.1"));
    }

    @DisplayName("Get artifact risk aggregation with success")
    @Test
    public void getArtifactVulnerabilitiesWithSuccess() throws ExecutionException, InterruptedException {

        this.wireMockServer.stubFor(
                WireMock.post("/public/risk-aggregation/aggregated-risks")
                        .withHeader("cxorigin", matching("JFrog .*"))
                        .withRequestBody(containing("{\"packageName\":\"lodash\",\"version\":\"0.2.1\",\"packageManager\":\"Npm\"}"))
                        .willReturn(ok()
                                .withHeader("Content-Type", "application/json; charset=UTF-8")
                                .withBody("{\"packageVulnerabilitiesAggregation\":{\"vulnerabilitiesCount\":159,\"maxRiskSeverity\":\"High\",\"maxRiskScore\":9.8,\"highRiskCount\":151,\"mediumRiskCount\":8,\"lowRiskCount\":0}}"))
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        var riskAggregationOfArtifact = scaHttpClient.getRiskAggregationOfArtifact("Npm", "lodash", "0.2.1");

        Assertions.assertEquals("High", riskAggregationOfArtifact.getVulnerabilitiesAggregation().getMaxRiskSeverity());
        Assertions.assertEquals(159, riskAggregationOfArtifact.getVulnerabilitiesAggregation().getVulnerabilitiesCount());
        Assertions.assertEquals(9.8, riskAggregationOfArtifact.getVulnerabilitiesAggregation().getMaxRiskScore());
        Assertions.assertEquals(151, riskAggregationOfArtifact.getVulnerabilitiesAggregation().getHighRiskCount());
        Assertions.assertEquals(8, riskAggregationOfArtifact.getVulnerabilitiesAggregation().getMediumRiskCount());
        Assertions.assertEquals(0, riskAggregationOfArtifact.getVulnerabilitiesAggregation().getLowRiskCount());
    }

    @DisplayName("Failed to get artifact risk aggregation - Unexpected Response Code")
    @Test
    public void failedToGetArtifactRiskAggregationUnexpectedResponseCode() {

        this.wireMockServer.stubFor(
                WireMock.post("/public/risk-aggregation/aggregated-risks")
                        .withHeader("cxorigin", matching("JFrog .*"))
                        .withRequestBody(containing("{\"packageName\":\"lodash\",\"version\":\"0.2.1\",\"packageManager\":\"Npm\"}"))
                        .willReturn(aResponse().withStatus(500))
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        Assertions.assertThrows(UnexpectedResponseCodeException.class, () -> scaHttpClient.getRiskAggregationOfArtifact("Npm", "lodash", "0.2.1"));
    }

    @DisplayName("Failed to get artifact risk aggregation - Unexpected Response Body")
    @Test
    public void failedToGetArtifactRiskAggregationUnexpectedResponseBody() {

        this.wireMockServer.stubFor(
                WireMock.post("/public/risk-aggregation/aggregated-risks")
                        .withHeader("cxorigin", matching("JFrog .*"))
                        .withRequestBody(containing("{\"packageName\":\"lodash\",\"version\":\"0.2.1\",\"packageManager\":\"Npm\"}"))
                        .willReturn(ok())
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        Assertions.assertThrows(UnexpectedResponseBodyException.class, () -> scaHttpClient.getRiskAggregationOfArtifact("Npm", "lodash", "0.2.1"));
    }

    @DisplayName("Suggest private package with success")
    @Test
    public void suggestPrivatePackageWithSuccess() throws ExecutionException, InterruptedException {

        this.wireMockServer.stubFor(
                WireMock.post("/private-dependencies-repository/dependencies")
                        .withHeader("cxorigin", matching("JFrog .*"))
                        .withRequestBody(containing("[{\"name\":\"lodash\",\"packageManager\":\"Npm\",\"version\":\"0.2.1\",\"resolvedBy\":\"PrivateArtifactory\"}]"))
                        .willReturn(ok())
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        var result = scaHttpClient.suggestPrivatePackage(new ArtifactId("Npm", "lodash", "0.2.1"));

        Assertions.assertTrue(result);
    }

    @DisplayName("Failed to suggest private package - Unexpected Response")
    @Test
    public void failedToSuggestPrivatePackage() {

        this.wireMockServer.stubFor(
                WireMock.post("/private-dependencies-repository/dependencies")
                        .withHeader("cxorigin", matching("JFrog .*"))
                        .withRequestBody(containing("[{\"name\":\"lodash\",\"packageManager\":\"Npm\",\"version\":\"0.2.1\",\"resolvedBy\":\"PrivateArtifactory\"}]"))
                        .willReturn(aResponse().withStatus(500))
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        Assertions.assertThrows(UnexpectedResponseBodyException.class, () -> scaHttpClient.suggestPrivatePackage(new ArtifactId("Npm", "lodash", "0.2.1")));
    }

    private Injector CreateAppInjectorForTests() {
        var logger = Mockito.mock(Logger.class);
        var artifactFiller = Mockito.mock(ArtifactRisksFiller.class);
        var securityThresholdChecker = Mockito.mock(SecurityThresholdChecker.class);
        var accessControlClient = Mockito.mock(AccessControlClient.class);
        Mockito.when(accessControlClient.GetAuthorizationHeader()).thenReturn(new AuthenticationHeader<>("Authorization", "Bearer token"));

        var properties = new Properties();

        properties.setProperty("sca.api.url", "http://localhost:8080/");
        properties.setProperty("sca.authentication.url", "http://localhost:8080/");

        var configuration = new PluginConfiguration(properties, logger);

        var appInjector = new TestsInjector(logger, configuration, artifactFiller, securityThresholdChecker);
        appInjector.setAccessControlClient(accessControlClient);
        return Guice.createInjector(appInjector);
    }
}
