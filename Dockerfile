FROM ubuntu:20.04 as build

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