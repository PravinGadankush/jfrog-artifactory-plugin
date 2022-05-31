package com.checkmarx.sca.scan;

import com.checkmarx.sca.PackageManager;
import com.checkmarx.sca.scan.fallbacks.ComposerFallback;
import com.checkmarx.sca.configuration.ConfigurationEntry;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.checkmarx.sca.models.ArtifactId;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import org.artifactory.repo.RepoPath;
import org.jfrog.security.util.Pair;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static java.lang.String.format;

public class ComposerArtifactIdBuilder {
    @Inject
    private Logger _logger;

    @Inject
    private ComposerFallback _composerFallback;

    private final String _baseUrl;
    private final HttpClient _httpClient;

    @Inject
    public ComposerArtifactIdBuilder(@Nonnull PluginConfiguration configuration) {
        _baseUrl = configuration.getPropertyOrDefault(ConfigurationEntry.PACKAGIST_REPOSITORY);

        _httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public ArtifactId generateArtifactId(@Nonnull RepoPath repoPath, @Nonnull PackageManager packageManager) {

        var artifactInfo = parseRepoPath(repoPath);

        if (artifactInfo.getFirst() == null) {
            return new ArtifactId(packageManager.packageType(), null, null);
        }

        try {

            var artifactId = requestPackageInfoFromPackagist(packageManager, artifactInfo.getFirst(), artifactInfo.getSecond());
            if (artifactId != null) {
                return artifactId;
            }

            var newName = _composerFallback.applyFallback(artifactInfo.getFirst());
            if (newName != null) {
                artifactId = requestPackageInfoFromPackagist(packageManager, newName, artifactInfo.getSecond());
                if (artifactId != null) {
                    return artifactId;
                }
            }

            _logger.warn(format("Unable to get artifact version from Composer. Artifact path: %s", repoPath.getPath()));
        } catch (Exception ex) {
            _logger.error(format("There was a problem trying to get the artifact version from Composer. Artifact path: %s", repoPath.getPath()));
            _logger.debug("Exception", ex);
        }

        return new ArtifactId(packageManager.packageType(), null, null);
    }

    private Pair<String, String> parseRepoPath(@Nonnull RepoPath repoPath){
        var regex = "(?<name>.+)/commits/(?<version>.+)/.+";
        var pattern = Pattern.compile(regex);
        var matcher = pattern.matcher(repoPath.getPath());

        if (!matcher.matches()) {
            _logger.error(format("Unable to parse RepoPath from Composer. Artifact path: %s", repoPath.getPath()));
            return new Pair<>(null, null);
        }

        var artifactName = matcher.group("name");
        var artifactVersion = matcher.group("version");

        if (artifactName == null || artifactVersion == null) {
            _logger.error(format("Unable to parse RepoPath from Composer. Artifact path: %s", repoPath.getPath()));
            return new Pair<>(null, null);
        }

        return new Pair<>(artifactName, artifactVersion);
    }

    private ArtifactId requestPackageInfoFromPackagist(@Nonnull PackageManager packageManager, @Nonnull String packageName, @Nonnull String commitReference) throws ExecutionException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(format("%s/p2/%s.json", _baseUrl, packageName)))
                .GET()
                .build();

        var responseFuture = _httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

        var response = responseFuture.get();
        if (response.statusCode() == 200) {
            JsonElement jElement = JsonParser.parseString(response.body());
            JsonObject jObject = jElement.getAsJsonObject();
            var packages = jObject.getAsJsonObject("packages");
            var versions = packages.getAsJsonArray(packageName);

            for (var vElement : versions) {
                var version = vElement.getAsJsonObject();
                var source = version.getAsJsonObject("source");

                if(source != null) {
                    var reference = source.get("reference").getAsString();
                    if (commitReference.equalsIgnoreCase(reference)) {
                        var usedVersion = version.get("version").getAsString();
                        return new ArtifactId(packageManager.packageType(), packageName, usedVersion);
                    }
                }
            }
        }

        return null;
    }
}
