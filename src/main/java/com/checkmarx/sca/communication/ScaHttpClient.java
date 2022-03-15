package com.checkmarx.sca.communication;

import com.checkmarx.sca.communication.exceptions.UnexpectedResponseBodyException;
import com.checkmarx.sca.communication.exceptions.UnexpectedResponseCodeException;
import com.checkmarx.sca.configuration.ConfigurationEntry;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.models.ArtifactId;
import com.checkmarx.sca.models.ArtifactInfo;
import com.checkmarx.sca.models.Vulnerability;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import static java.lang.String.format;

public class ScaHttpClient {

    private final AccessControlClient _accessControlClient;
    private final HttpClient _httpClient;
    private final String _apiUrl;

    @Inject
    public ScaHttpClient(@Nonnull PluginConfiguration configuration, @Nonnull AccessControlClient accessControlClient) {
        var apiUrl = configuration.getPropertyOrDefault(ConfigurationEntry.API_URL);
        if (!apiUrl.endsWith("/")) {
            apiUrl = apiUrl + "/";
        }

        _apiUrl = apiUrl;
        _httpClient = HttpClient.newHttpClient();
        _accessControlClient = accessControlClient;
    }

    public ArtifactInfo getArtifactInformation(String packageType, String name, String version) throws ExecutionException, InterruptedException {
        var request = getArtifactInfoRequest(packageType, name, version);

        var responseFuture = _httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        var artifactResponse = responseFuture.get();

        if (artifactResponse.statusCode() == 404)
            throw new UnexpectedResponseCodeException(artifactResponse.statusCode());

        if (artifactResponse.statusCode() != 200)
            throw new UnexpectedResponseCodeException(artifactResponse.statusCode());

        ArtifactInfo artifactInfo;
        try {
            artifactInfo = new Gson().fromJson(artifactResponse.body(), ArtifactInfo.class);
        } catch (Exception ex) {
            throw new UnexpectedResponseBodyException(artifactResponse.body());
        }

        if (artifactInfo == null) {
            throw new UnexpectedResponseBodyException("");
        }

        return artifactInfo;
    }

    public ArrayList<Vulnerability> getVulnerabilitiesForArtifact(@NotNull ArtifactId artifactId) throws ExecutionException, InterruptedException {
        var request = getVulnerabilitiesForArtifactRequest(artifactId);

        var responseFuture = _httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        var vulnerabilitiesResponse = responseFuture.get();
        if (vulnerabilitiesResponse.statusCode() != 200)
            throw new UnexpectedResponseCodeException(vulnerabilitiesResponse.statusCode());

        ArrayList<Vulnerability> vulnerabilitiesList;
        try {
            Type listType = new TypeToken<ArrayList<Vulnerability>>(){}.getType();

            vulnerabilitiesList = new Gson().fromJson(vulnerabilitiesResponse.body(), listType);
        } catch (Exception ex) {
            throw new UnexpectedResponseBodyException(vulnerabilitiesResponse.body());
        }

        if (vulnerabilitiesList == null) {
            throw new UnexpectedResponseBodyException("");
        }

        return vulnerabilitiesList;
    }

    private HttpRequest getVulnerabilitiesForArtifactRequest(@NotNull ArtifactId artifactId) {

        var authHeader = _accessControlClient.GetAuthorizationHeader();

        String body = format("[\"%s\"]", artifactId.getIdentifier());

        var request = HttpRequest.newBuilder(URI.create(format("%s%s", _apiUrl, "vulnerabilities/search-requests")))
                .header("content-type", "application/json")
                .setHeader(authHeader.getKey(), authHeader.getValue())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return request;
    }

    private HttpRequest getArtifactInfoRequest(@NotNull String packageType, @NotNull String name, @NotNull String version) {

        var authHeader = _accessControlClient.GetAuthorizationHeader();
        var artifactPath = format("packages/%s/%s/%s", packageType, name, version);

        var request = HttpRequest.newBuilder(URI.create(format("%s%s", _apiUrl, artifactPath)))
                .setHeader(authHeader.getKey(), authHeader.getValue())
                .GET()
                .build();

        return request;
    }
}
