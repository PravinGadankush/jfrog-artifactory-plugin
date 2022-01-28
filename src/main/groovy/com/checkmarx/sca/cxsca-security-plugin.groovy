package com.checkmarx.sca

import groovy.transform.Field
import org.artifactory.repo.RepoPath
import org.artifactory.request.Request

@Field ScaPlugin scaPlugin

scanExistingPackages()

private void scanExistingPackages() {
    log.warn("Initializing Security Plugin...")

    scaPlugin = new ScaPlugin(repositories)

    log.warn("Initialization of Security Plugin completed")
}

download {
  beforeRemoteDownload { Request request, RepoPath repoPath ->
      scaPlugin.beforeRemoteDownload(request, repoPath)
  }
}
