#!/bin/bash
# Script need two arguments
# 1st is RHBQ platform bom version
# 2nd argument is path to local maven repository

QUARKUS_PLATFORM_BOM_VERSION=$1
QUARKUS_REPO_TAG=$(echo $QUARKUS_PLATFORM_BOM_VERSION | sed 's/[-.]redhat.*$//')
MAVEN_REPO_LOCAL=$2

mvn clean install exec:java -DskipTests -Dmaven.repo.local=${MAVEN_REPO_LOCAL} -Dquarkus.platform.bom=com.redhat.quarkus.platform:quarkus-bom:${QUARKUS_PLATFORM_BOM_VERSION} -Dquarkus.repo.tag=${QUARKUS_REPO_TAG}
