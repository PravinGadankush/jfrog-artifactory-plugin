package com.checkmarx.sca.communication;

import com.checkmarx.sca.PackageManager;
import com.checkmarx.sca.communication.exceptions.UnexpectedResponseBodyException;
import com.checkmarx.sca.communication.exceptions.UnexpectedResponseCodeException;
import com.checkmarx.sca.scan.fallbacks.ComposerFallback;
import com.checkmarx.sca.communication.fallbacks.PyPiFallback;
import com.checkmarx.sca.configuration.ConfigurationEntry;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.models.ArtifactInfo;
import com.checkmarx.sca.models.PackageRiskAggregation;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import static java.lang.String.format;

public class ScaHttpClient {
    private final String UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.92 Safari/537.36";
    private final HttpClient _httpClient;
    private final String _apiUrl;

    @Inject
    private PyPiFallback _pyPiFallback;

    @Inject
    public ScaHttpClient(@Nonnull PluginConfiguration configuration) {
        var apiUrl = configuration.getPropertyOrDefault(ConfigurationEntry.API_URL);
        if (!apiUrl.endsWith("/")) {
            apiUrl = apiUrl + "/";
        }

        _apiUrl = apiUrl;
        _httpClient = HttpClient.newHttpClient();
    }

    private HttpResponse<String> getArtifactInfoResponse(String packageType, String name, String version) throws ExecutionException, InterruptedException {
        var request = getArtifactInfoRequest(packageType, name, version);

        var responseFuture = _httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        return responseFuture.get();
    }

    public ArtifactInfo getArtifactInformation(String packageType, String name, String version) throws ExecutionException, InterruptedException {

        var artifactResponse = getArtifactInfoResponse(packageType, name, version);

        if (artifactResponse.statusCode() == 404) {
            artifactResponse = TryToFallback(artifactResponse, packageType, name, version);
        }

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

    public PackageRiskAggregation getRiskAggregationOfArtifact(String packageType, String name, String version) throws ExecutionException, InterruptedException {
        var request = getRiskAggregationArtifactRequest(packageType, name, version);

        var responseFuture = _httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        var risksResponse = responseFuture.get();
        if (risksResponse.statusCode() != 200)
            throw new UnexpectedResponseCodeException(risksResponse.statusCode());

        PackageRiskAggregation packageRiskAggregation;
        try {
            Type listType = new TypeToken<PackageRiskAggregation>(){}.getType();

            packageRiskAggregation = new Gson().fromJson(risksResponse.body(), listType);
        } catch (Exception ex) {
            throw new UnexpectedResponseBodyException(risksResponse.body());
        }

        if (packageRiskAggregation == null) {
            throw new UnexpectedResponseBodyException("");
        }

        return packageRiskAggregation;
    }

    private HttpRequest getRiskAggregationArtifactRequest(String packageType, String name, String version) {

        String body = format("{\"packageName\":\"%s\",\"version\":\"%s\",\"packageManager\":\"%s\"}", name, version, packageType);

        return HttpRequest.newBuilder(URI.create(format("%s%s", _apiUrl, "public/risk-aggregation/aggregated-risks")))
                .header("content-type", "application/json")
                .header("User-Agent", UserAgent)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private HttpRequest getArtifactInfoRequest(@NotNull String packageType, @NotNull String name, @NotNull String version) {

        name = URLEncoder.encode(name, StandardCharsets.UTF_8);
        version = URLEncoder.encode(version, StandardCharsets.UTF_8);

        var artifactPath = format("public/packages/%s/%s/%s", packageType, name, version);

        return HttpRequest.newBuilder(URI.create(format("%s%s", _apiUrl, artifactPath)))
                .header("User-Agent", UserAgent)
                .GET()
                .build();
    }

    private HttpResponse<String> TryToFallback(HttpResponse<String> previousResponse, String packageType, String name, String version) throws ExecutionException, InterruptedException {

        String newName = null;
        if (packageType.equals(PackageManager.PYPI.packageType())) {
            newName = _pyPiFallback.applyFallback(name);
        }

        if (newName == null) {
            throw new UnexpectedResponseCodeException(previousResponse.statusCode());
        }

        var artifactResponse = getArtifactInfoResponse(packageType, newName, version);

        if (artifactResponse.statusCode() == 404) {
            throw new UnexpectedResponseCodeException(artifactResponse.statusCode());
        }

        return artifactResponse;
    }


}
