package com.checkmarx.sca.scan.fallbacks;

import com.checkmarx.sca.configuration.ConfigurationEntry;
import com.checkmarx.sca.configuration.PluginConfiguration;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.lang.String.format;

public class ComposerFallback {

    @Inject
    private Logger _logger;

    private final String _baseUrl;
    private final HttpClient _httpClient;

    @Inject
    public ComposerFallback(@Nonnull PluginConfiguration configuration) {
        _baseUrl = configuration.getPropertyOrDefault(ConfigurationEntry.PACKAGIST_REPOSITORY);

        _httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public String applyFallback(String name) {

        _logger.debug("Using Composer Fallback System.");

        var arrOfStr = name.split("/", 2);
        if (arrOfStr.length != 2) {
            return null;
        }

        try {
            var request = HttpRequest.newBuilder(URI.create(format("%s/search.json?q=%s", _baseUrl, arrOfStr[1])))
                    .GET()
                    .build();

            var responseFuture = _httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

            var response = responseFuture.get();

            if (response.statusCode() == 200) {
                JsonElement jElement = JsonParser.parseString(response.body());
                JsonObject jObject = jElement.getAsJsonObject();
                var results = jObject.getAsJsonArray("results");

                for (var result : results) {
                    var packageData = result.getAsJsonObject();
                    var repository = packageData.get("repository").getAsString();

                    if (repository != null && repository.contains(name)) {
                        _logger.debug("Composer fallback found new name for the artifact.");
                        return packageData.get("name").getAsString();
                    }
                }
            }
        } catch (Exception ex) {
            _logger.debug("Exception", ex);
        }

        _logger.debug("Composer fallback couldn't find any alternative name for the artifact.");

        return null;
    }
}
