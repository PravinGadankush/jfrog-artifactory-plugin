#!/bin/bash

line='001'
while [ "$line" != '200' ]
do 
    echo 'Waiting...'
    sleep 5
    artifactoryReady=`curl --write-out '%{http_code}' --silent --output /dev/null -u admin:password http://artifactory:8081/artifactory/api/system/ping`
    line="$artifactoryReady"
done

create_repository () {
    curl -u "admin:password" \
        -X PUT https://nginx/artifactory/api/repositories/$1 \
        -H 'Content-Type: application/json' \
        -d @$2
}

upload_package() {
  REPOSITORY=$1
  ARTIFACT_NAME=$2
  ARTIFACT=$3

    curl -u "admin:password" \
        -X PUT "https://nginx/artifactory/$REPOSITORY/$ARTIFACT_NAME" \
        -T "$ARTIFACT"
}



clean_logs () {
    > /opt/cxsca.log
}

validate_result () {
    expectedLogFileContent=$(</projects/$1/jfrog/expected.log)
    expectedResult=$(echo "$expectedLogFileContent" | grep -oP '(?<=Ended\sthe\sartifact\sverification\.\sArtifact\sname:\s).*' | sort | uniq | tr '\n' ' ')

    currentLogFileContent=$(</opt/cxsca.log)
    echo -e $currentLogFileContent
    currentResult=$(echo "$currentLogFileContent" | grep -oP '(?<=Ended\sthe\sartifact\sverification\.\sArtifact\sname:\s).*' | sort | uniq | tr '\n' ' ')

    if [ "$expectedResult" != "$currentResult" ]; then        
        echo -e "\n[FAILED] to compare the number of dependencies scanned of $1!"
        echo -e "\nExpected: $expectedResult \nFound: $currentResult"
        succeeded=false
    else
        echo -e "\n[SUCCESS] $1 was successfully scanned"
    fi
}

cd /projects;
succeeded=true
update-ca-certificates;

# Create Repositories
create_repository "go-remote" "./go/jfrog/remote.json";
create_repository "go-virtual" "./go/jfrog/virtual.json";

create_repository "pip-remote" "./pip/jfrog/remote.json";

create_repository "ivy-remote" "./ivy/jfrog/remote.json";

create_repository "npm-remote" "./npm/jfrog/remote.json";
create_repository "npm-local" "./npm-suggestion-private/jfrog/local.json";

create_repository "mvn-local" "./maven/jfrog/local.json";
create_repository "mvn-remote" "./maven/jfrog/remote.json";
create_repository "mvn-virtual" "./maven/jfrog/virtual.json";

create_repository "gradle-local" "./gradle/jfrog/local.json";
create_repository "gradle-remote" "./gradle/jfrog/remote.json";
create_repository "gradle-virtual" "./gradle/jfrog/virtual.json";

create_repository "bower-remote" "./bower/jfrog/remote.json";

create_repository "composer-remote" "./composer/jfrog/remote.json";

create_repository "cocoapods-remote" "./cocoapods/jfrog/remote.json";

create_repository "nuget-remote" "./nuget/jfrog/remote.json";

create_repository "sbt-remote" "./sbt/jfrog/remote.json";

# Auth
encryptedPassword=`curl -u "admin:password" -X GET https://nginx/artifactory/api/security/encryptedPassword`
encryptedPasswordBase64=`echo -n $encryptedPassword | base64`

# NPM Test
cd /projects/npm/project;

clean_logs;

sed -e "s/\${password}/$encryptedPasswordBase64/" template.npmrc > .npmrc

npm i

validate_result "npm";

# NPM private package Test
cd /projects/npm/project;

clean_logs;

upload_package "npm-local" "test/-/npmtestpackage-it-1.0.0.tgz" "/projects/npm-suggestion-private/project/npmtestpackage-it-1.0.0.tgz"

validate_result "npm-suggestion-private";

# Go Test
cd /projects/go/project;

clean_logs;

export GOPROXY="https://admin:$encryptedPassword@nginx/artifactory/api/go/go-virtual";

go mod download;

validate_result "go";

# CocoaPods Test
cd /projects/cocoapods/project/Example;

clean_logs;

export COCOAPODS_ART_CREDENTIALS="admin:password"

pod repo-art add cocoapods-remote "https://nginx/artifactory/api/pods/cocoapods-remote" --allow-root

pod install --allow-root

validate_result "cocoapods";

# Composer Test
cd /projects/composer/project;

clean_logs;

sed -e "s/\${password}/$encryptedPassword/" auth.template > auth.json

composer install --no-scripts --no-autoloader --ignore-platform-reqs -n

validate_result "composer";

# Bower Test
cd /projects/bower/project;

clean_logs;

sed -e "s/\${password}/$encryptedPassword/" template.bowerrc > .bowerrc

bower install --allow-root

validate_result "bower";

# Ivy Test
cd /projects/ivy/project;

clean_logs;

ant -file build.xml

validate_result "ivy";

# Pip Test
cd /projects/pip/project;

clean_logs;

sed -e "s/\${password}/$encryptedPassword/" template.requirements.txt > requirements.txt

pip install -r requirements.txt --trusted-host artifactory

validate_result "pip";

# Maven Test
cd /projects/maven/project;

clean_logs;

sed -e "s/\${password}/$encryptedPassword/" .mvn/template.settings.xml > .mvn/settings.xml

mvn dependency:tree

validate_result "maven";

# Gradle Test
cd /projects/gradle/project;

clean_logs;

sed -e "s/\${password}/$encryptedPassword/" template.properties > gradle.properties

gradle dependencies

validate_result "gradle";

# Nuget Test
cd /projects/nuget/project;

clean_logs;

dotnet restore 

validate_result "nuget";

# Sbt Test
cd /projects/sbt/project;

clean_logs;

sbt clean compile

validate_result "sbt";

if [ "$succeeded" = false ] ; then
  exit 2
fi