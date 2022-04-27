library "pipelineUtils"

pipeline{
    parameters{
        booleanParam(name: "shouldPublishToDownloads", defaultValue: false, description: 'Should the artifacts be made available for user download')
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        MASTER_BRANCH = "master"
        ARTIFACTS_DIRECTORY = "artifacts"
        VERSION = pipelineUtils.getSemanticVersion(1, 0)
    }

    agent { node { label 'docker' } }

    stages{
        stage("Build"){
            steps{
                script{
                    env.FORMATTED_VERSION = "${VERSION}"
                    currentBuild.displayName = "${env.FORMATTED_VERSION}"
                    def containerName = "jfrog-plugin-${env.FORMATTED_VERSION}"
                    pipelineUtils.buildDockerImage(null, "VERSION=${VERSION}")
                    sh "docker create --name $containerName buildimage"
                    sh "docker cp $containerName:/artifacts ${env.ARTIFACTS_DIRECTORY}"
                }
            }
        }
        stage("Publish"){
            steps{
                withAWS([credentials: 's3publish', region: 'eu-central-1']) {
                    script {
                        def filesToUpload = [findFiles(glob: "${env.ARTIFACTS_DIRECTORY}/*")].flatten()
                        filesToUpload.each {
                            def fullPath = it.getPath()
                            fileName = it.getName()
                            fileDir = fullPath.substring(0, fullPath.lastIndexOf('/'))
                            s3Upload([bucket      : 'lumo-dev-deployment-bucket/published-artifacts/ScaJFrogPlugin/' + env.FORMATTED_VERSION,
                                      workingDir  : fileDir,
                                      file        : fileName,
                                      metadatas   : ["Build:" + env.FORMATTED_VERSION],
                                      cacheControl: 'max-age=315360000'
                            ])
                            if (env.BRANCH_NAME == env.MASTER_BRANCH) {
                                s3Upload([bucket      : 'lumo-dev-deployment-bucket/published-artifacts/ScaJFrogPlugin/latest-master',
                                          workingDir  : fileDir,
                                          file        : fileName,
                                          metadatas   : ["Build:" + env.FORMATTED_VERSION],
                                          cacheControl: 'max-age=315360000'
                                ])
                            }
                        }
                    }
                }
                withAWS([credentials: 'jenkins_user_prod', region: 'us-east-1']) {
                    script {
                        if (params.shouldPublishToDownloads && env.BRANCH_NAME == env.MASTER_BRANCH) {
                            def filesToUpload = [findFiles(glob: "${env.ARTIFACTS_DIRECTORY}/*")].flatten()
                            filesToUpload.each {
                                def fullPath = it.getPath()
                                fileName = it.getName()
                                fileDir = fullPath.substring(0, fullPath.lastIndexOf('/'))
                                s3Upload([bucket      : 'sca-downloads/ScaJFrogPlugin/' + env.FORMATTED_VERSION,
                                          workingDir  : fileDir,
                                          file        : fileName,
                                          metadatas   : ["Build:" + env.FORMATTED_VERSION],
                                          cacheControl: 'max-age=315360000',
                                          acl: 'PublicRead'
                                ])
                                s3Upload([bucket      : 'sca-downloads/ScaJFrogPlugin/latest',
                                          workingDir  : fileDir,
                                          file        : fileName,
                                          metadatas   : ["Build:" + env.FORMATTED_VERSION],
                                          cacheControl: 'max-age=315360000',
                                          acl: 'PublicRead'
                                ])
                            }
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                if (env.BRANCH_NAME == "master")
                    pipelineUtils.sendNotificationEmail()
            }
        }
    }
}