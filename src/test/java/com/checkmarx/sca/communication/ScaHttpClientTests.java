package com.checkmarx.sca.communication;

import com.checkmarx.sca.TestsInjector;
import com.checkmarx.sca.communication.exceptions.FailedToRefreshTokenException;
import com.checkmarx.sca.communication.exceptions.UnexpectedResponseBodyException;
import com.checkmarx.sca.communication.exceptions.UnexpectedResponseCodeException;
import com.checkmarx.sca.communication.models.AuthenticationHeader;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.models.ArtifactId;
import com.checkmarx.sca.scan.ArtifactChecker;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.artifactory.repo.Repositories;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.slf4j.Logger;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

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
                WireMock.get("/packages/Npm/lodash/0.2.1")
                        .willReturn(ok()
                                .withHeader("Content-Type", "application/json; charset=UTF-8")
                                .withBody("{\"id\":{\"identifier\":\"Npm-lodash-0.2.1\"},\"name\":\"lodash\",\"version\":\"0.2.1\",\"type\":\"Npm\",\"releaseDate\":\"2012-05-24T21:53:08\",\"description\":\"A drop-in replacement for Underscore.js that delivers performance improvements, bug fixes, and additional features.\",\"repositoryUrl\":\"git://github.com/bestiejs/lodash.git\",\"binaryUrl\":\"https://registry.npmjs.org/lodash/-/lodash-0.2.1.tgz\",\"projectUrl\":\"\",\"bugsUrl\":null,\"sourceUrl\":\"\",\"projectHomePage\":\"http://lodash.com\",\"homePage\":\"\",\"license\":\"\",\"summary\":\"\",\"url\":\"\",\"owner\":\"\"}"))
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        var artifactInfo = scaHttpClient.getArtifactInformation("Npm", "lodash", "0.2.1");

        Assertions.assertNotNull(artifactInfo);
        Assertions.assertEquals("Npm-lodash-0.2.1", artifactInfo.getId().getIdentifier());
    }

    @DisplayName("Failed to get artifact information - Not found")
    @Test
    public void failedToGetArtifactInformationNotFound() throws ExecutionException, InterruptedException {

        this.wireMockServer.stubFor(
                WireMock.get("/packages/Npm/lodash/0.2.1")
                        .willReturn(aResponse().withStatus(404))
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        var exception = Assertions.assertThrows(UnexpectedResponseCodeException.class, () -> scaHttpClient.getArtifactInformation("Npm", "lodash", "0.2.1"));

        Assertions.assertEquals(404, exception.StatusCode);
    }

    @DisplayName("Failed to get artifact information - Unexpected Response Code")
    @Test
    public void failed() throws InterruptedException {

        this.wireMockServer.stubFor(
                WireMock.get("/packages/Npm/lodash/0.2.1")
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
                WireMock.get("/packages/Npm/lodash/0.2.1")
                        .willReturn(ok())
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        Assertions.assertThrows(UnexpectedResponseBodyException.class, () -> scaHttpClient.getArtifactInformation("Npm", "lodash", "0.2.1"));
    }

    @DisplayName("Get artifact vulnerabilities with success")
    @Test
    public void getArtifactVulnerabilitiesWithSuccess() throws ExecutionException, InterruptedException {

        this.wireMockServer.stubFor(
                WireMock.post("/vulnerabilities/search-requests")
                        .withRequestBody(containing("[\"Npm-lodash-0.2.1\"]"))
                        .willReturn(ok()
                                .withHeader("Content-Type", "application/json; charset=UTF-8")
                                .withBody("[{\"id\":\"CVE-2020-28500\",\"cwe\":\"CWE-400\",\"description\":\"Lodash before 4.17.21 is vulnerable to Regular Expression Denial of Service (ReDoS) via the toNumber, trim and trimEnd functions.\",\"vulnerabilityType\":\"Regular\",\"publishDate\":\"2021-02-15T11:15:00\",\"score\":5.3,\"severity\":\"Medium\",\"created\":\"2021-03-04T08:29:31\",\"cveName\":\"CVE-2020-28500\",\"updateTime\":\"2022-01-07T06:04:48\"}]"))
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        var artifactId = new ArtifactId("Npm-lodash-0.2.1");

        var vulnerabilities = scaHttpClient.getVulnerabilitiesForArtifact(artifactId);

        Assertions.assertEquals(1, vulnerabilities.size());
    }

    @DisplayName("Failed to get artifact vulnerabilities - Unexpected Response Code")
    @Test
    public void failedToGetArtifactVulnerabilitiesUnexpectedResponseCode() {

        this.wireMockServer.stubFor(
                WireMock.post("/vulnerabilities/search-requests")
                        .withRequestBody(containing("[\"Npm-lodash-0.2.1\"]"))
                        .willReturn(aResponse().withStatus(500))
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        var artifactId = new ArtifactId("Npm-lodash-0.2.1");

        Assertions.assertThrows(UnexpectedResponseCodeException.class, () -> scaHttpClient.getVulnerabilitiesForArtifact(artifactId));
    }

    @DisplayName("Failed to get artifact vulnerabilities - Unexpected Response Body")
    @Test
    public void failedToGetArtifactVulnerabilitiesUnexpectedResponseBody() {

        this.wireMockServer.stubFor(
                WireMock.post("/vulnerabilities/search-requests")
                        .withRequestBody(containing("[\"Npm-lodash-0.2.1\"]"))
                        .willReturn(ok())
        );

        var injector = CreateAppInjectorForTests();

        var scaHttpClient = injector.getInstance(ScaHttpClient.class);

        var artifactId = new ArtifactId("Npm-lodash-0.2.1");

        Assertions.assertThrows(UnexpectedResponseBodyException.class, () -> scaHttpClient.getVulnerabilitiesForArtifact(artifactId));
    }

    private Injector CreateAppInjectorForTests(){
        var logger = Mockito.mock(Logger.class);
        var artifactChecker = Mockito.mock(ArtifactChecker.class);
        var accessControlClient = Mockito.mock(AccessControlClient.class);
        Mockito.when(accessControlClient.GetAuthorizationHeader()).thenReturn(new AuthenticationHeader<>("Authorization", "Bearer token"));

        var properties = new Properties();

        properties.setProperty("sca.api.url", "http://localhost:8080/");
        properties.setProperty("sca.authentication.url", "http://localhost:8080/");

        var configuration = new PluginConfiguration(properties);

        var appInjector = new TestsInjector(logger, artifactChecker, configuration);
        appInjector.setAccessControlClient(accessControlClient);
        return Guice.createInjector(appInjector);
    }
}
