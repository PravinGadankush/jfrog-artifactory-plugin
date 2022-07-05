FROM ubuntu:20.04 AS build

RUN apt-get update \
    && apt-get install zip maven -y

COPY pom.xml /project-root/
COPY src/ /project-root/src/

WORKDIR /project-root

ARG VERSION=1.0.0
RUN mvn versions:set -DnewVersion=${VERSION} && mvn package -X

WORKDIR /build

RUN mkdir -p lib
RUN cp /project-root/target/sca-artifactory-plugin.jar ./lib \
    && cp /project-root/src/main/groovy/com/checkmarx/sca/cxsca-security-plugin.groovy . \
    && cp /project-root/src/main/groovy/com/checkmarx/sca/cxsca-security-plugin.properties . \
    && zip -r sca-jfrog-plugin.zip . \
    && mkdir -p /artifacts \
    && cp sca-jfrog-plugin.zip /artifacts

FROM releases-docker.jfrog.io/jfrog/artifactory-pro:7.38.10 AS jfrog

WORKDIR /opt/jfrog/artifactory/var/etc/artifactory
COPY /integration-tests/artifactory-data/logback.xml .
COPY /integration-tests/artifactory-data/artifactory.config.xml .

WORKDIR /opt/jfrog/artifactory/var/etc/artifactory/plugins
COPY --from=build /build/cxsca-security-plugin.groovy .
COPY --from=build /build/cxsca-security-plugin.properties .

WORKDIR /opt/jfrog/artifactory/var/etc/artifactory/plugins/lib
COPY --from=build /build/lib/sca-artifactory-plugin.jar .

WORKDIR /opt/jfrog/artifactory

FROM 882696877841.dkr.ecr.us-east-1.amazonaws.com/source-resolver:2022.06.22.1507-e1c14de AS test

RUN apt update && apt install -y docker.io iputils-ping

# GO MODULES
# -------------
WORKDIR /usr/local
RUN curl -sSL -O https://golang.org/dl/go1.16.6.linux-amd64.tar.gz \
  && tar -xvzf go1.16.6.linux-amd64.tar.gz \
  && rm -f go1.16.6.linux-amd64.tar.gz
ENV PATH=$PATH:/usr/local/go/bin

RUN gem install cocoapods-art

RUN npm install -g --save bower-art-resolver

WORKDIR /integration-tests

COPY /integration-tests/projects /projects
COPY /integration-tests/runTests.sh .

COPY /integration-tests/projects/sbt/jfrog/repositories /root/.sbt/repositories

ENTRYPOINT ["bash", "-cex"]