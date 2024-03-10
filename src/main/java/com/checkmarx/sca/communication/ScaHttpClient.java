package com.checkmarx.sca.communication;

import com.checkmarx.sca.PackageManager;
import com.checkmarx.sca.communication.exceptions.UnexpectedResponseBodyException;
import com.checkmarx.sca.communication.exceptions.UnexpectedResponseCodeException;
import com.checkmarx.sca.communication.exceptions.UserIsNotAuthenticatedException;
import com.checkmarx.sca.models.ArtifactId;
import com.checkmarx.sca.communication.fallbacks.PyPiFallback;
import com.checkmarx.sca.configuration.ConfigurationEntry;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.models.ArtifactInfo;
import com.checkmarx.sca.models.PackageAnalysisAggregation;
import com.checkmarx.sca.models.PackageLicensesModel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import org.artifactory.exception.CancelException;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class ScaHttpClient {
    private final String UserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.92 Safari/537.36";
    private final HttpClient _httpClient;
    private final String _apiUrl;

    @Inject
    private PyPiFallback _pyPiFallback;

    @Inject(optional = true)
    private AccessControlClient _accessControlClient;

    @Inject
    public ScaHttpClient(@Nonnull PluginConfiguration configuration) {
        var apiUrl = configuration.getPropertyOrDefault(ConfigurationEntry.API_URL);
        if (!apiUrl.endsWith("/")) {
            apiUrl += '/';
        }

        _apiUrl = apiUrl;
        _httpClient = HttpClient.newHttpClient();
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

    public PackageAnalysisAggregation getRiskAggregationOfArtifact(String packageType, String name, String version) throws ExecutionException, InterruptedException {
        var request = getRiskAggregationArtifactRequest(packageType, name, version);

        var responseFuture = _httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        var risksResponse = responseFuture.get();
        if (risksResponse.statusCode() != 200)
            throw new UnexpectedResponseCodeException(risksResponse.statusCode());

        PackageAnalysisAggregation packageAnalysisAggregation;
        try {
            Type listType = new TypeToken<PackageAnalysisAggregation>() {
            }.getType();

            packageAnalysisAggregation = new Gson().fromJson(risksResponse.body(), listType);
        } catch (Exception ex) {
            throw new UnexpectedResponseBodyException(risksResponse.body());
        }

        if (packageAnalysisAggregation == null) {
            throw new UnexpectedResponseBodyException("");
        }

        List<String> licenses = List.of();
        try {
            var license = getPackageLicenseOfArtifact(packageType, name, version);
            if (license.getIdentifiedLicenses() != null && license.getIdentifiedLicenses().size() > 0) {
                licenses = license.getIdentifiedLicenses().stream()
                        .map(identifiedLicense -> identifiedLicense.getLicense().getName())
                        .collect(Collectors.toList());
            }
        } catch (Exception ex) {
            licenses = List.of();
        }
        packageAnalysisAggregation.setLicenses(licenses);

        return packageAnalysisAggregation;
    }

    public Boolean suggestPrivatePackage(ArtifactId artifactId) throws ExecutionException, InterruptedException, MissingResourceException {

        var request = getSuggestPrivatePackageRequest(artifactId);

        var responseFuture = _httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        var response = responseFuture.get();

        if (response.statusCode() != 200) {
            throw new UnexpectedResponseBodyException(response.body());
        }

        return true;
    }

    private HttpRequest getRiskAggregationArtifactRequest(String packageType, String name, String version) throws CancelException {

        String body = format("{\"packageName\":\"%s\",\"version\":\"%s\",\"packageManager\":\"%s\"}", name, version, packageType);

        return HttpRequest.newBuilder(URI.create(format("%s%s", _apiUrl, "public/risk-aggregation/aggregated-risks")))
                .header("content-type", "application/json")
                .header("User-Agent", UserAgent)
                .header("cxorigin", getCxOrigin())
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private HttpRequest getLicenceArtifactRequest(@NotNull String packageType, @NotNull String name, @NotNull String version) throws CancelException {

        name = URLEncoder.encode(name, StandardCharsets.UTF_8);
        version = URLEncoder.encode(version, StandardCharsets.UTF_8);

        var url = format("public/packages/%s/%s/versions/%s/licenses", packageType, name, version);

        return HttpRequest.newBuilder(URI.create(format("%s%s", _apiUrl, url)))
                .header("User-Agent", UserAgent)
                .header("cxorigin", getCxOrigin())
                .GET()
                .build();
    }

    private HttpRequest getArtifactInfoRequest(@NotNull String packageType, @NotNull String name, @NotNull String version) {

        name = URLEncoder.encode(name, StandardCharsets.UTF_8);
        version = URLEncoder.encode(version, StandardCharsets.UTF_8);

        var artifactPath = format("public/packages/%s/%s/versions/%s", packageType, name, version);

        return HttpRequest.newBuilder(URI.create(format("%s%s", _apiUrl, artifactPath)))
                .header("User-Agent", UserAgent)
                .header("cxorigin", getCxOrigin())
                .GET()
                .build();
    }

    private HttpRequest getSuggestPrivatePackageRequest(ArtifactId artifactId) throws CancelException {

        var body = format("[{\"name\":\"%s\",\"packageManager\":\"%s\",\"version\":\"%s\",\"resolvedBy\":\"PrivateArtifactory\"}]",
                artifactId.Name, artifactId.PackageType, artifactId.Version);

        if (_accessControlClient == null) {
            throw new UserIsNotAuthenticatedException();
        }

        var authHeader = _accessControlClient.GetAuthorizationHeader();
        return HttpRequest.newBuilder(URI.create(format("%s%s", _apiUrl, "private-dependencies-repository/dependencies")))
                .header(authHeader.getKey(), authHeader.getValue())
                .header("content-type", "application/json")
                .header("User-Agent", UserAgent)
                .header("cxorigin", getCxOrigin())
                .POST(HttpRequest.BodyPublishers.ofString(body))
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

    private HttpResponse<String> TryToFallbackLicense(HttpResponse<String> previousResponse, String packageType, String name, String version) throws ExecutionException, InterruptedException {

        String newName = null;
        if (packageType.equals(PackageManager.PYPI.packageType())) {
            newName = _pyPiFallback.applyFallback(name);
        }

        if (newName == null) {
            throw new UnexpectedResponseCodeException(previousResponse.statusCode());
        }

        var artifactRequest = getLicenceArtifactRequest(packageType, newName, version);

        var artifactResponse = _httpClient.sendAsync(artifactRequest, HttpResponse.BodyHandlers.ofString()).get();

        if (artifactResponse.statusCode() == 404) {
            throw new UnexpectedResponseCodeException(artifactResponse.statusCode());
        }

        return artifactResponse;
    }

    private HttpResponse<String> getArtifactInfoResponse(String packageType, String name, String version) throws ExecutionException, InterruptedException {
        var request = getArtifactInfoRequest(packageType, name, version);

        var responseFuture = _httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        return responseFuture.get();
    }

    private PackageLicensesModel getPackageLicenseOfArtifact(String packageType, String name, String version) throws ExecutionException, InterruptedException {
        var request = getLicenceArtifactRequest(packageType, name, version);

        var responseFuture = _httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        var licenseResponse = responseFuture.get();

        if (licenseResponse.statusCode() == 404) {
            licenseResponse = TryToFallbackLicense(licenseResponse, packageType, name, version);
        }
        if (licenseResponse.statusCode() != 200) {
            throw new UnexpectedResponseCodeException(licenseResponse.statusCode());
        }
        PackageLicensesModel packageAnalysisAggregation;
        try {
            Type listType = new TypeToken<PackageLicensesModel>() {
            }.getType();

            packageAnalysisAggregation = new Gson().fromJson(licenseResponse.body(), listType);
        } catch (Exception ex) {
            throw new UnexpectedResponseBodyException(licenseResponse.body());
        }

        if (packageAnalysisAggregation == null) {
            throw new UnexpectedResponseBodyException("");
        }

        return packageAnalysisAggregation;
    }

    private String getCxOrigin(){
        Package p = getClass().getPackage();
        var version = p.getImplementationVersion() != null ? p.getImplementationVersion() : "1.0.0";
        return String.format("JFrog %s", version);
    }
}
