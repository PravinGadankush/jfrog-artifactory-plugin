package com.checkmarx.sca;

import org.artifactory.repo.RepoPath;
import org.artifactory.repo.Repositories;
import org.artifactory.request.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;

public class ScaPlugin
{
    private final Repositories _repositories;
    private final Logger _logger;

    public ScaPlugin(@Nonnull Repositories repositories, Logger logger) {
        _repositories = repositories;
        _logger = logger;
    }

    public void beforeRemoteDownload(Request request, RepoPath repoPath) {
        _logger.error("Handle 'beforeRemoteDownload' event.");
    }
}
