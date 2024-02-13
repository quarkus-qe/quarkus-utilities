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
