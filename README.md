# quarkus-utilities
Utilities used for experiments and testing with Quarkus

## Existing utilities

### diff-utils

`diff-gav-artifact.sh` script creates diffs for GAV artifacts included in RHBQ maven repository zip

- `create_colorful_diff.sh` utility to create  the colorful diff between two files.
- Java program that prints dependencies of added artifacts \
`mvn clean install exec:java -Dquarkus.maven.dir="path_to_dir" -Dquarkus.new-artifacts-list="added_artifacts.txt"`