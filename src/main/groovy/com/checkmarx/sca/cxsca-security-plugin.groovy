package com.checkmarx.sca

import groovy.transform.Field
import org.artifactory.repo.RepoPath
import org.artifactory.request.Request

@Field ScaPlugin scaPlugin

scanExistingArtifacts()

private void scanExistingArtifacts() {
    log.info("Initializing Security Plugin...")

    File pluginsDirectory = ctx.artifactoryHome.pluginsDir
    scaPlugin = new ScaPlugin(log, pluginsDirectory, repositories)

    log.info("Initialization of Sca Security Plugin completed")
}

download {
    beforeDownload { Request request, RepoPath repoPath ->
        scaPlugin.beforeDownload(repoPath)
    }
}
