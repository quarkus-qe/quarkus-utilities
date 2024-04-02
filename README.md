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

- Preparation:
  - `git clone https://github.com/quarkusio/quarkus.git`
  - `git checkout <tag>` tag should be version which will be base for comparison e.g. 3.8.3
  - `mvn versions:compare-dependencies -DremotePom=com.redhat.quarkus.platform:quarkus-bom:<version_to_compare> -Dmaven.repo.local="<path_to_local_repository>" -DreportOutputFile=<name_of_output_file>`
- Generation
  - `mvn clean install exec:java -Dpath-to-quarkus-repository=<path_to_quarkus_repo> -Dversion-plugin.report-output-file=<name_of_output_file_from_version_plugin> -DskipTests`
  - The diff is stored in `outputDiff.html` and `outputDiffDetailed.html` contains which extension or part of Quarkus are affected.
- Testing
  - There is possibility to test if there is some artifact which have different version
  - `mvn clean test -Dpath-to-quarkus-repository=<path_to_quarkus_repo> -Dversion-plugin.report-output-file=<name_of_output_file_from_version_plugin> -Dquarkus-version=<base_version_of_quarkus>`
  - `quarkus-version` property is optional if not set all different artifacts are treated as not allowed
  - Example of `quarkus-version` usage `-Dquarkus-version=3.8` or just run with example `-Dquarkus-version=example`
  - These allowed artifacts config file needs to be stored in `src/main/resources` and should have format of `<quarkus_version>_allowed_artifacts.yaml`, where `_allowed_artifacts.yaml` is must have to work.
