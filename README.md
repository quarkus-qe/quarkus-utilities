# quarkus-utilities
Utilities used for experiments and testing with Quarkus

## Existing utilities

### diff-utils

`diff-gav-artifact.sh` script creates diffs for GAV artifacts included in RHBQ maven repository zip

- `create_colorful_diff.sh` utility to create  the colorful diff between two files.
- Java program that prints dependencies of added artifacts \
`mvn clean install exec:java -Dquarkus.maven.dir="path_to_dir" -Dquarkus.new-artifacts-list="added_artifacts.txt"`


### quarkus-extensions-with-dependency

Creates PDF with Quarkus extensions contain given dependency (quarkus-vertx, quarkus-resteasy etc.) 

- Usage:`get-extensions.sh DEPENDENCY_NAME [OUTPUT_FILE_NAME].pdf` 
- Example: `./get-extensions.sh quarkus-vertx extensions-vertx.pdf`
- Result will be written by default to the PDF document `Quarkus-extensions-with-*.pdf` or to the file with your custom name

### artifact-version-diff

Create the report of different version between two Qaurkus version. This can be used for upstream vs RHBQ or upstream vs upstream.
The utility clone Quarkus repository with specified tag and run `mvn versions:compare-dependencies` to create diff files used to generate report.
It's possible only generate report, to check which artifact are different. Testing is useful when running automated check.

To quickly generate diff between RHBQ and upstream you can use `prod_vs_upstream.sh` script. This script need two argument:

1. RHBQ platform bom version (e.g. `3.8.3.redhat-00002`)
2. Path to local maven repository

Example usage of script `./prod_vs_upstream.sh 3.8.3.redhat-00002 ~/rh-quarkus-platform-3.8.3.GA-maven-repository/maven-repository`

Or you can run it manually as described below.

- Generation
  - `mvn clean install exec:java -DskipTests -Dmaven.repo.local=<path_to_maven_local_repository> -Dquarkus.repo.tag=<tag_to_checkout> -Dquarkus.platform.bom=<platform_bom>`
  - The `maven.repo.local` is optional
  - The `quarkus.repo.tag` if source/base for comparing the versions. This needs to be valid tag in Quarkus git repository
  - The `quarkus.platform.bom` needs to be in format `<groupID>:<artifactID>:version`
  - The diff is stored in `outputDiff.html` and `outputDiffDetailed.html` contains which extension or part of Quarkus are affected.
  - Comparing with RHBQ example (version 3.8.3 vs 3.8.3.redhat-00002):
    - `mvn clean install exec:java -DskipTests -Dmaven.repo.local=<path_to_maven_local_repository> -Dquarkus.platform.bom=com.redhat.quarkus.platform:quarkus-bom:3.8.3.redhat-00002 -Dquarkus.repo.tag=3.8.3`
  - Comparing two upstream versions example (version 3.8.3 vs 3.8.4):
    - `mvn clean install exec:java -DskipTests -Dquarkus.platform.bom=io.quarkus.platform:quarkus-bom:3.8.4 -Dquarkus.repo.tag=3.8.3`
- Testing
  - There is possibility to test if there is some artifact which have different version
  - `mvn clean test -Dmaven.repo.local=<path_to_maven_local_repository> -Dquarkus.repo.tag=<tag_to_checkout> -Dquarkus.platform.bom=<platform_bom> -Dquarkus-version=<major_minor_quarkus_cersion>`
  - The `maven.repo.local` is optional
  - The `quarkus-version` is optional if not set all different artifacts are treated as not allowed
  - The `quarkus.repo.tag` if source/base for comparing the versions. This needs to be valid tag in Quarkus git repository
  - The `quarkus.platform.bom` needs to be in format `<groupID>:<artifactID>:version`
  - Example of `quarkus-version` usage `-Dquarkus-version=3.8` or just run with example `-Dquarkus-version=example`
  - These allowed artifacts config file needs to be stored in `src/main/resources` and should have format of `<quarkus_version>_allowed_artifacts.yaml` (e.g `3.8_allowed_artifacts.yaml`), where `_allowed_artifacts.yaml` is must have to work.
  - Testing with RHBQ example (version 3.8.3 vs 3.8.3.redhat-00002):
    - `mvn clean test -Dmaven.repo.local=<path_to_maven_local_repository> -Dquarkus.platform.bom=com.redhat.quarkus.platform:quarkus-bom:3.8.3.redhat-00002 -Dquarkus.repo.tag=3.8.3`
