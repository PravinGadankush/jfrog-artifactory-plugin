package com.checkmarx.sca.suggestion;

import com.checkmarx.sca.PackageManager;
import com.checkmarx.sca.communication.ScaHttpClient;
import com.checkmarx.sca.communication.exceptions.UnexpectedResponseCodeException;
import com.checkmarx.sca.models.ArtifactId;
import com.checkmarx.sca.scan.ArtifactIdBuilder;
import com.google.inject.Inject;
import org.artifactory.exception.CancelException;
import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.lang.String.format;

public class PrivatePackageSuggestionHandler {

    public static final String SUGGESTED_KEY = "CxSCA.PrivatePackageSuggested";

    @Inject
    private Logger _logger;

    @Inject
    private ScaHttpClient _scaHttpClient;

    @Inject
    private ArtifactIdBuilder _artifactIdBuilder;

    private final Repositories _repositories;
    private final boolean _noAuthConfiguration;

    @Inject
    public PrivatePackageSuggestionHandler(@Nonnull Repositories repositories, boolean hasAuthConfiguration) {
        _repositories = repositories;
        _noAuthConfiguration = !hasAuthConfiguration;
    }

    public void suggestPrivatePackage(
            @Nonnull RepoPath repoPath,
            @Nonnull ArrayList<RepoPath> nonVirtualRepoPaths) throws CancelException {

        if (_noAuthConfiguration)
            return;

        var isSuggested = _repositories.hasProperty(repoPath, SUGGESTED_KEY);
        if (isSuggested)
            return;

        if (!nonVirtualRepoPaths.contains(repoPath))
            return;

        var repositoryKey = repoPath.getRepoKey();
        var repoConfiguration = _repositories.getRepositoryConfiguration(repositoryKey);

        ArtifactId artifactId;
        try {
            var packageType = repoConfiguration.getPackageType();
            var packageManager = PackageManager.GetPackageType(packageType);

            var fileLayoutInfo = _repositories.getLayoutInfo(repoPath);

            artifactId = _artifactIdBuilder.getArtifactId(fileLayoutInfo, repoPath, packageManager);

            if (artifactId.isInvalid()) {
                _logger.error(format("The artifact id was not built correctly. PackageType: %s, Name: %s, Version: %s", artifactId.PackageType, artifactId.Name, artifactId.Version));
                return;
            }

            var succeeded = performSuggestion(artifactId);

            if(succeeded && !markResourceAsSuggested(repoPath)) {
                _logger.info("Failed to mark the package as suggested.");
            }
        } catch (Exception ex) {
            _logger.error(format("Exception Message: %s. Artifact Name: %s.", ex.getMessage(), repoPath.getName()), ex);
        }
    }

    private Boolean performSuggestion(ArtifactId artifactId) {
        try {
            var output = _scaHttpClient.suggestPrivatePackage(artifactId);
            _logger.info("The package was suggested as potential private.");
            return output;
        } catch (ExecutionException | UnexpectedResponseCodeException | InterruptedException exc){
            _logger.warn("Failed to publish private package suggestion", exc);
            return false;
        }
    }

    private boolean markResourceAsSuggested(RepoPath repoPath) {
        var props = _repositories.setProperty(repoPath, SUGGESTED_KEY, "true");
        return props.containsKey(SUGGESTED_KEY);
    }
}

