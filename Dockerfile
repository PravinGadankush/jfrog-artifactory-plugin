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

WORKDIR /opt/jfrog/artifactory/var/etc/artifactory/plugins/lib
COPY --from=build /build/lib/sca-artifactory-plugin.jar .

WORKDIR /opt/jfrog/artifactory

FROM debian:12 AS test

RUN apt update && apt install -y docker.io iputils-ping wget curl unzip

# NPM
# ------
ARG NODE_VERSION=18.19.0
ARG NPM_VERSION=8.18.0
ARG LERNA_VERSION=5.0.0
ENV PATH=$PATH:/usr/local/lib/node-v${NODE_VERSION}-linux-x64/bin

WORKDIR /usr/local
RUN apt install -yq --no-install-recommends xz-utils \
    && curl -sSL -O https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-x64.tar.xz \
    && tar -xf node-v${NODE_VERSION}-linux-x64.tar.xz -C /usr/local/lib \
    && rm node-v${NODE_VERSION}-linux-x64.tar.xz \
    && npm install -g npm@${NPM_VERSION} \
    && npm install -g lerna@${LERNA_VERSION}

# BOWER
# --------
ARG BOWER_VERSION=1.8.14
ARG BOWER_DEPENDENCY_TREE_VERSION=0.1.2

RUN npm install -g bower@${BOWER_VERSION} \
    && npm install -g bower-dependency-tree@${BOWER_DEPENDENCY_TREE_VERSION} \
    && npm install -g --save bower-art-resolver

# RUBY GEMS
# ------------
RUN apt install -yq --no-install-recommends ruby-rubygems ruby-dev make gcc libcurl4 libc6-dev git locales \
    && gem install bundler \
    && mkdir -p -m777 "$(gem environment gemdir)/bundler/gems"

# COCOAPODS
# ------------
ARG COCOAPODS_VERSION=1.11.3
RUN gem install cocoapods -v ${COCOAPODS_VERSION} \
    && gem install cocoapods-art \
    && echo 'en_US.UTF-8 UTF-8' | tee -a /etc/locale.gen \
    && echo 'export LC_ALL=en_US.UTF-8' | tee -a ~/.bashrc \
    && locale-gen
ENV LC_ALL=en_US.UTF-8

# GO MODULES
# -------------
ARG GOMODULES_VERSION=1.21.6
WORKDIR /usr/local
RUN curl -sSL -O https://golang.org/dl/go${GOMODULES_VERSION}.linux-amd64.tar.gz \
    && tar -xvzf go${GOMODULES_VERSION}.linux-amd64.tar.gz \
    && rm -f go${GOMODULES_VERSION}.linux-amd64.tar.gz
ENV PATH=$PATH:/usr/local/go/bin

# JDK
# --------
RUN mkdir -p /usr/share/man/man1 \
    && apt install -yq --no-install-recommends default-jre

# MAVEN
# --------
ARG MAVEN_VERSION=3.8.8
ENV MAVEN_HOME=/usr/share/maven
ENV MAVEN_CONFIG="/root/.m2"

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
    && curl -fsSL -o /tmp/apache-maven.tar.gz https://dlcdn.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
    && echo "332088670d14fa9ff346e6858ca0acca304666596fec86eea89253bd496d3c90deae2be5091be199f48e09d46cec817c6419d5161fb4ee37871503f472765d00 /tmp/apache-maven.tar.gz" | sha512sum -c - \
    && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
    && rm -f /tmp/apache-maven.tar.gz \
    && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn \
    && rm -rf /usr/share/maven/conf/settings.xml

# GRADLE
# ---------
ENV GRADLE_VERSION=7.5.1
WORKDIR /usr/local
RUN curl -sSL https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip -o gradle-$GRADLE_VERSION-bin.zip \
    && unzip gradle-$GRADLE_VERSION-bin.zip \
    && rm -f gradle-$GRADLE_VERSION-bin.zip
ENV GRADLE_HOME=/usr/local/gradle-$GRADLE_VERSION
ENV PATH=$PATH:$GRADLE_HOME/bin

# IVY
# ------
ARG ANT_VERSION=1.10.14
ARG IVY_VERSION=2.5.2
ENV ANT_OPTS="-Divy.default.ivy.user.dir=/usr/share/.ivy2"
ENV ANT_HOME="/usr/share/ant/apache-ant-${ANT_VERSION}"
ENV PATH="${PATH}:${ANT_HOME}/bin:${ANT_OPTS}"

RUN curl -sS -O https://dlcdn.apache.org/ant/binaries/apache-ant-${ANT_VERSION}-bin.tar.gz \
    && mkdir /usr/share/ant \
    && tar xf apache-ant-*.tar.gz -C /usr/share/ant \
    && rm apache-ant-*.tar.gz

RUN curl -sS -O https://dlcdn.apache.org/ant/ivy/${IVY_VERSION}/apache-ivy-${IVY_VERSION}-bin.tar.gz \
    && mkdir /usr/share/ivy \
    && mkdir /usr/share/.ivy2 \
    && tar xf apache-ivy-*.tar.gz -C /usr/share/ivy \
    && cp /usr/share/ivy/apache-ivy-${IVY_VERSION}/ivy-*.jar /usr/share/ant/apache-ant-${ANT_VERSION}/lib \
    && rm apache-ivy-*.tar.gz

# DOTNET
# ------
RUN wget https://packages.microsoft.com/config/debian/10/packages-microsoft-prod.deb -O packages-microsoft-prod.deb\
    && dpkg -i packages-microsoft-prod.deb \
    && rm packages-microsoft-prod.deb\
    && apt -q update \
    && apt install apt-transport-https \
    && apt -q update \
    && apt -q install -y dotnet-sdk-7.0\
    && apt -q clean

# PIP
# ------
RUN apt update -yq \
    && apt install -y --no-install-recommends make build-essential libssl-dev zlib1g-dev libbz2-dev libreadline-dev libsqlite3-dev  \
            wget curl llvm libncurses5-dev xz-utils tk-dev libxml2-dev libxmlsec1-dev libffi-dev liblzma-dev mecab-ipadic-utf8 git
            
ENV HOME="/root"
WORKDIR $HOME
RUN git clone --depth=1 https://github.com/pyenv/pyenv.git .pyenv

ENV PYENV_ROOT="$HOME/.pyenv"
ENV PATH="$PYENV_ROOT/shims:$PYENV_ROOT/bin:$PATH"

RUN pyenv install 3.11.3 \
    && pyenv global 3.11.3 \
    && pip install virtualenv

# COMPOSER
# -----------
ARG COMPOSER_VERSION=2.4.0
RUN apt install --no-install-recommends -yq php-cli php-mbstring php-xml php-zip php-gd php-ldap subversion \
    && curl -sS https://getcomposer.org/installer -o composer-setup.php \
    && php composer-setup.php --install-dir=/usr/local/bin --filename=composer --version=${COMPOSER_VERSION} \
    && rm composer-setup.php

# SBT (with SCALA dependencies)
# --------------------------------
ARG SBT_VERSION=1.7.1
ARG SCALA_VERSION=3.1.3
ARG SCALA_PATH=/usr/local/scala3-3.1.3/bin/
ENV PATH=$PATH:$SCALA_PATH

WORKDIR /usr/local
RUN apt remove -yq scala-library scala \
    && apt install -yq --no-install-recommends apt-transport-https \
    && curl -sSL -O https://github.com/lampepfl/dotty/releases/download/${SCALA_VERSION}/scala3-${SCALA_VERSION}.tar.gz \
    && tar -xvzf scala3-${SCALA_VERSION}.tar.gz -C /usr/local \
    && rm scala3-${SCALA_VERSION}.tar.gz

WORKDIR /opt
RUN apt install -yq --no-install-recommends gnupg \
    && echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | tee /etc/apt/sources.list.d/sbt.list \
    && echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | tee /etc/apt/sources.list.d/sbt_old.list \
    && curl -sSL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add \
    && apt update -yq \
    && apt install -yq --no-install-recommends sbt=${SBT_VERSION}

WORKDIR /integration-tests

COPY /integration-tests/projects /projects
COPY /integration-tests/runTests.sh .

COPY /integration-tests/projects/sbt/jfrog/repositories /root/.sbt/repositories

ENTRYPOINT ["bash", "-cex"]
