package com.checkmarx.sca.communication;

import com.checkmarx.sca.communication.exceptions.AuthenticationFailedException;
import com.checkmarx.sca.communication.exceptions.FailedToRefreshTokenException;
import com.checkmarx.sca.communication.exceptions.UnexpectedAuthenticationResponseException;
import com.checkmarx.sca.communication.exceptions.UserIsNotAuthenticatedException;
import com.checkmarx.sca.communication.models.AccessControlCredentials;
import com.checkmarx.sca.communication.models.AccessControlToken;
import com.checkmarx.sca.communication.models.AuthenticationHeader;
import com.checkmarx.sca.configuration.ConfigurationEntry;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.google.gson.Gson;
import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class AccessControlClient {
    private final String TokenEndpointPath = "identity/connect/token";
    private final String ClientId = "sca_resource_owner";
    private final String OAuthScope = "sca_api";

    @Inject
    private Logger _logger;

    private final HttpClient _httpClient;
    private final String _authenticationUrl;
    private AccessControlToken _accessControlToken;
    private AccessControlCredentials _accessControlCredentials;

    @Inject
    public AccessControlClient(@Nonnull PluginConfiguration configuration)
    {
        var authenticationUrl = configuration.getPropertyOrDefault(ConfigurationEntry.AUTHENTICATION_URL);
        if (!authenticationUrl.endsWith("/")) {
            authenticationUrl = authenticationUrl + "/";
        }

        _authenticationUrl = authenticationUrl;
        _httpClient = HttpClient.newHttpClient();
    }

    public boolean Authenticate(@NotNull AccessControlCredentials accessControlCredentials)
    {
        try{
            _accessControlCredentials = accessControlCredentials;

            AuthenticateResourceOwner();
        } catch (Exception ex) {
            _logger.error(ex.getMessage(), ex);
            return false;
        }

        return true;
    }

    public AuthenticationHeader<String, String> GetAuthorizationHeader()
    {
        if(_accessControlToken == null) {
            throw new UserIsNotAuthenticatedException();
        }

        if (_accessControlToken.isActive())
            return GenerateTokenAuthorizationHeader();

        var success = RefreshTokenAsync();
        if (!success) {
            throw new FailedToRefreshTokenException();
        }

        return GenerateTokenAuthorizationHeader();
    }

    public String GetAuthorizationToken()
    {
        if(_accessControlToken == null) {
            throw new UserIsNotAuthenticatedException();
        }

        if (_accessControlToken.isActive())
            return _accessControlToken.getAccessToken();

        var success = RefreshTokenAsync();
        if (!success) {
            throw new FailedToRefreshTokenException();
        }

        return _accessControlToken.getAccessToken();
    }

    private void AuthenticateResourceOwner() throws ExecutionException, InterruptedException {
        var resourceOwnerGrantRequest = CreateResourceOwnerGrantRequest();

        var responseFuture = _httpClient.sendAsync(resourceOwnerGrantRequest, HttpResponse.BodyHandlers.ofString());

        var authenticateResponse = responseFuture.get();

        if (authenticateResponse.statusCode() != 200)
            throw new AuthenticationFailedException(authenticateResponse.statusCode());

        AccessControlToken accessControlToken;
        try {
            accessControlToken = new Gson().fromJson(authenticateResponse.body(), AccessControlToken.class);
        } catch (Exception ex) {
            throw new UnexpectedAuthenticationResponseException(authenticateResponse.body());
        }

        if (accessControlToken == null || !accessControlToken.isBearerToken())
            throw new UnexpectedAuthenticationResponseException(authenticateResponse.body());

        _accessControlToken = accessControlToken;
    }

    private HttpRequest CreateResourceOwnerGrantRequest()
    {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("scope", OAuthScope);
        parameters.put("client_id", ClientId);
        parameters.put("username", _accessControlCredentials.getUsername());
        parameters.put("password", _accessControlCredentials.getPassword());
        parameters.put("grant_type", "password");
        parameters.put("acr_values", format("Tenant:%s", _accessControlCredentials.getTenant()));

        String form = parameters.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));

        var request = HttpRequest.newBuilder(URI.create(format("%s%s", _authenticationUrl, TokenEndpointPath)))
                .header("content-type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        return request;
    }

    private boolean RefreshTokenAsync()
    {
        return Authenticate(_accessControlCredentials);
    }

    private AuthenticationHeader<String, String> GenerateTokenAuthorizationHeader()
    {
        return new AuthenticationHeader<>("Authorization", format("Bearer %s", _accessControlToken.getAccessToken()));
    }
}
