package com.checkmarx.sca.communication;

import com.checkmarx.sca.AppInjector;
import com.checkmarx.sca.communication.exceptions.AuthenticationFailedException;
import com.checkmarx.sca.communication.exceptions.FailedToRefreshTokenException;
import com.checkmarx.sca.communication.exceptions.UnexpectedAuthenticationResponseException;
import com.checkmarx.sca.communication.exceptions.UserIsNotAuthenticatedException;
import com.checkmarx.sca.communication.models.AccessControlCredentials;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.scan.ArtifactChecker;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static java.lang.String.format;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@DisplayName("AccessControlClient")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AccessControlClientTests {

    private WireMockServer wireMockServer;
    private Logger logger;

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

    @DisplayName("Authenticate the user with success")
    @Test
    public void authenticateUserWithSuccess() {

        String token = "eyJhbGciOiJSUzI1NiIsC6";
        this.wireMockServer.stubFor(
                WireMock.post("/identity/connect/token")
                        .willReturn(ok()
                                .withHeader("Content-Type", "application/json; charset=UTF-8")
                                .withBody(format("{\"access_token\":\"%s\",\"expires_in\":3600,\"token_type\":\"Bearer\"}", token)))
        );

        var injector = CreateAppInjectorForTests();

        var accessControlClient = injector.getInstance(AccessControlClient.class);

        var accessControlCredentials = new AccessControlCredentials("user", "pass", "tenant");

        var result = accessControlClient.Authenticate(accessControlCredentials);

        Assertions.assertTrue(result);
    }

    @DisplayName("Fail to authenticate the user - bad status code")
    @Test
    public void failToAuthenticateUser() {

        this.wireMockServer.stubFor(
                WireMock.post("/identity/connect/token")
                        .willReturn(badRequest())
        );

        var injector = CreateAppInjectorForTests();

        var accessControlClient = injector.getInstance(AccessControlClient.class);

        var accessControlCredentials = new AccessControlCredentials("user", "pass", "tenant");

        var result = accessControlClient.Authenticate(accessControlCredentials);

        Assertions.assertFalse(result);
        Mockito.verify(logger, times(1)).error(Mockito.anyString(), Mockito.any(AuthenticationFailedException.class));
    }

    @DisplayName("Fail to authenticate the user - unexpected response")
    @Test
    public void failToAuthenticateUserUnexpectedResponse() {

        this.wireMockServer.stubFor(
                WireMock.post("/identity/connect/token")
                        .willReturn(ok())
        );

        var injector = CreateAppInjectorForTests();

        var accessControlClient = injector.getInstance(AccessControlClient.class);

        var accessControlCredentials = new AccessControlCredentials("user", "pass", "tenant");

        var result = accessControlClient.Authenticate(accessControlCredentials);

        Assertions.assertFalse(result);
        Mockito.verify(logger, times(1)).error(Mockito.anyString(), Mockito.any(UnexpectedAuthenticationResponseException.class));
    }

    @DisplayName("Get authorization header with success")
    @Test
    public void getAuthorizationHeaderWithSuccess() {

        String token = "eyJhbGciOiJSUzI1NiIsC6";
        this.wireMockServer.stubFor(
                WireMock.post("/identity/connect/token")
                        .willReturn(ok()
                                .withHeader("Content-Type", "application/json; charset=UTF-8")
                                .withBody(format("{\"access_token\":\"%s\",\"expires_in\":3600,\"token_type\":\"Bearer\"}", token)))
        );

        var injector = CreateAppInjectorForTests();

        var accessControlClient = injector.getInstance(AccessControlClient.class);

        var accessControlCredentials = new AccessControlCredentials("user", "pass", "tenant");

        var result = accessControlClient.Authenticate(accessControlCredentials);
        Assertions.assertTrue(result);

        var authenticationHeader = accessControlClient.GetAuthorizationHeader();

        Assertions.assertEquals("Authorization", authenticationHeader.getKey());
        Assertions.assertEquals(format("Bearer %s", token), authenticationHeader.getValue());
    }

    @DisplayName("Failed to get authorization header - not authenticated")
    @Test()
    public void failedToGetAuthorizationHeaderNotAuthenticated() {

        var injector = CreateAppInjectorForTests();

        var accessControlClient = injector.getInstance(AccessControlClient.class);

        Assertions.assertThrows(UserIsNotAuthenticatedException.class, () -> accessControlClient.GetAuthorizationHeader());
    }

    @DisplayName("Get authorization header with success - refresh token")
    @Test
    public void getAuthorizationHeaderWithSuccessAfterRefreshToken() {

        String oldToken = "eyJhbGciOiJSUzI1NiIsC6";
        this.wireMockServer.stubFor(
                WireMock.post("/identity/connect/token")
                        .inScenario("Refresh Scenario")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(ok()
                                .withHeader("Content-Type", "application/json; charset=UTF-8")
                                .withBody(format("{\"access_token\":\"%s\",\"expires_in\":0,\"token_type\":\"Bearer\"}", oldToken)))
                        .willSetStateTo("Cause Success")
        );

        String newToken = "eyJhbGciOiJSUzI1NiIsC6";
        this.wireMockServer.stubFor(
                WireMock.post("/identity/connect/token")
                        .inScenario("Refresh Scenario")
                        .whenScenarioStateIs("Cause Success")
                        .willReturn(ok()
                                .withHeader("Content-Type", "application/json; charset=UTF-8")
                                .withBody(format("{\"access_token\":\"%s\",\"expires_in\":3600,\"token_type\":\"Bearer\"}", newToken)))
        );

        var injector = CreateAppInjectorForTests();

        var accessControlClient = injector.getInstance(AccessControlClient.class);

        var accessControlCredentials = new AccessControlCredentials("user", "pass", "tenant");

        var result = accessControlClient.Authenticate(accessControlCredentials);
        Assertions.assertTrue(result);

        var authenticationHeader = accessControlClient.GetAuthorizationHeader();

        Assertions.assertEquals("Authorization", authenticationHeader.getKey());
        Assertions.assertEquals(format("Bearer %s", newToken), authenticationHeader.getValue());
    }

    @DisplayName("Failed to get authorization header - fail to refresh token")
    @Test
    public void failedToGetAuthorizationHeaderFailToRefreshToken() {

        String oldToken = "eyJhbGciOiJSUzI1NiIsC6";
        this.wireMockServer.stubFor(
                WireMock.post("/identity/connect/token")
                        .inScenario("Refresh Scenario")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(ok()
                                .withHeader("Content-Type", "application/json; charset=UTF-8")
                                .withBody(format("{\"access_token\":\"%s\",\"expires_in\":0,\"token_type\":\"Bearer\"}", oldToken)))
                        .willSetStateTo("Cause Success")
        );

        this.wireMockServer.stubFor(
                WireMock.post("/identity/connect/token")
                        .inScenario("Refresh Scenario")
                        .whenScenarioStateIs("Cause Success")
                        .willReturn(badRequest())
        );

        var injector = CreateAppInjectorForTests();

        var accessControlClient = injector.getInstance(AccessControlClient.class);

        var accessControlCredentials = new AccessControlCredentials("user", "pass", "tenant");

        var result = accessControlClient.Authenticate(accessControlCredentials);

        Assertions.assertTrue(result);
        Assertions.assertThrows(FailedToRefreshTokenException.class, () -> accessControlClient.GetAuthorizationHeader());
    }

    private Injector CreateAppInjectorForTests(){
        logger = Mockito.mock(Logger.class);
        var artifactChecker = Mockito.mock(ArtifactChecker.class);

        var properties = new Properties();
        properties.setProperty("sca.api.url", "http://localhost:8080/");
        properties.setProperty("sca.authentication.url", "http://localhost:8080/");

        var configuration = new PluginConfiguration(properties);
        var accessControlClient = new AccessControlClient(configuration);

        var appInjector = new AppInjector(logger, artifactChecker, configuration, accessControlClient);

        return Guice.createInjector(appInjector);
    }
}
