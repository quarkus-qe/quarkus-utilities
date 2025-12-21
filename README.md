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

### disabled-tests-inspector
Disabled Tests Inspector is a tool that analyzes Java test code in a GitHub repository, identifying disabled tests and categorizing them by annotation type and module.
It generates a JSON report summarizing the findings.

#### Features
- Scans Java test files for disabled test annotations (`@Disabled`, `@DisabledOnNative`, `@DisabledOnOs`, etc.)
- Extracts reasons and issue links from annotations and comments
- Supports multiline reasons
- Detects closed GitHub issues
- Lite report mode: optional mode to filter out environment/configuration-specific exclusions and focus on actual bugs
- Categorizes disabled tests per module
- Generates structured JSON reports

#### Requirements
- Java 17+
- Maven 3.8.6+
- A GitHub personal access token

#### Usage example
You can configure the tool using Java system properties

| Property             | Description                                                      | Default  |
|:---------------------|:-----------------------------------------------------------------|:---------|
| `repoOwner`          | The owner of the GitHub repository                               | Required |
| `repoName`           | The name of the repository                                       | Required |
| `branches`           | Comma-separated list of branches to analyze                      | Required |
| `baseOutputFileName` | Base name for the generated JSON files                           | Required |
| `liteReport`         | If `true`, filters out noise (see `Running in Lite Mode` below)  | `false`  |


#### Running the analysis
If you want to inspect multiple branches, list them comma-separated in `branches` property as described in the example below:
```shell
export GITHUB_OAUTH=your_personal_access_token  
mvn clean install
java -DrepoOwner=repoOwner -DrepoName=repoName -Dbranches="main,3.27" -DbaseOutputFileName=disabled-tests -jar target/quarkus-app/quarkus-run.jar
```

#### Running in Lite Mode
Lite Mode automatically filters out "noise" from configuration annotations (like specific JRE or OS requirements)
It ensures that you only see environment-specific disabled tests if they are actually tracking a bug
`java -DliteReport=true ...`

#### What is filtered in Lite Mode?
- Always skipped: configuration annotations like `DisabledForJreRange`, `EnabledWhenLinuxContainersAvailable`, etc.
- Conditionally skipped: annotations like `DisabledOnNative` or `DisabledOnOs` are skipped unless they contain a link to an issue tracker

#### Example output
After running the tool, you will get the following output files:

1. `disabled-tests-3.27.json` – A detailed list of all disabled tests in `3.27` branch
2. `disabled-tests-3.27-stats.json` – A summary of disabled tests per annotation type per module in `3.27` branch
3. `disabled-tests-main.json` – A detailed list of all disabled tests in `main` branch
4. `disabled-tests-main-stats.json` – A summary of disabled tests per annotation type per module in `main` branch

### test-stats-analyzer

Tool that allows to analyze how many tests are run for given Java git project.
Unlikely tools that use static analysis, this tool transform given project so that test methods and JUnit 5 callback methods have empty bodies.
This way, when tests are run, we can only collect statistics without executing any verification.
The downside to this approach is that it takes considerable time to generate the stats.
Advantage to this approach is that static approach cannot reliably analysis following scenarios:

- how many times is method invoked for a parametrized test value source
- dynamic test execution conditions written in Java (e.g. disable on Podman, disable on FIPS and RHBQ and so on)

This tool gives overall statistics for executed and skipped tests, you can analyse the disabled tests with the 'disabled-tests-inspector' tool placed also in this project.

#### Prerequisites

You need to have installed JDK 21 or higher and Maven 3.9.6 or higher.

#### How to use it

Use the `test-stats-analyzer/generate-stats.sh` script to generate statistics.
If you have this project built locally as well as the project for which you want to generate statistics, you can execute `./test-stats-analyzer/generate-stats.sh -d "$PWD" -f ~/sources/quarkus-test-suite`.
This is the most efficient (quickest) way to generate statistics, however it will modify your project and requires that both projects are built.
Usually, you will want to generate statistics from ephemeral project directory.
For example, if you want to generate statistics for JVM mode, native mode, Kubernetes in JVM mode, OpenShift in JVM mode and OpenShift in native for Quarkus QE Test Suite branches 3.15, 3.20 and main, you will do:

```
wget -q https://raw.githubusercontent.com/quarkus-qe/quarkus-utilities/refs/heads/main/test-stats-analyzer/generate-stats.sh
chmod +x generate-stats.sh
./generate-stats.sh -b '3.15,3.20,main' -c 'jvm-mode,native-mode,openshift,kubernetes,openshift+native-mode'
```

You may want to take it easy (e.g. just one branch and one mode) in order to wait shorter.

Following command options are supported:

| Option | Description                                                                                                | Default value                                        |
|--------|------------------------------------------------------------------------------------------------------------|------------------------------------------------------|
| -c     | Modes: 'jvm-mode', 'native-mode', 'openshift', 'kubernetes' as well as any combination of previous values. | jvm-mode                                             |
| -b     | Target project tested branches.                                                                            | main                                                 |
| -t     | Target project URL (AKA: URL to project for which you want to generate statistics).                        | https://github.com/quarkus-qe/quarkus-test-suite.git |
| -f     | Target project directory. If specified, the Target project URL will be ignored                             |                                                      |
| -d     | Quarkus Utilities project directory. If specified, the Recipe project URL will be ignored.                 |                                                      |
| -u     | Quarkus Utilities project URL.                                                                             | https://github.com/quarkus-qe/quarkus-utilities.git  |
| -s     | Quarkus Utilities project branch.                                                                          | main                                                 |
| -a     | Additional Maven command arguments.                                                                        |                                                      |
| -r     | Working directory. The main purpose of this directory is to store results.                                 | temporary directory                                  |

For projects other than Quarkus QE Test Suite and Quarkus QE Test Framework, dry run may or may not work depending on JUnit extensions you use.
Meaning - you would have to tweak the OpenRewrite recipe to remove some additional annotations specific for your project so that code unnecessary for the analysis is not executed.

Please note that if the target project build requires some dependencies not available locally or in configured Maven repositories (like `3.23.999-SNAPSHOT`), you have 2 options.
Either build the missing dependencies so that they are available, or enhance the `generate-stats.sh` script to recognize supported alternatives.
For example, for `3.20` branch we currently use `3.20.1` and for `3.15` branch we currently use `3.15.5`, which is done automatically for you.

#### Example result

The `generate-stats.sh` generates `generated_test_stats.xml` into the working directory which has the following format:

```xml
<report>
  <project-url>https://github.com/quarkus-qe/quarkus-test-suite.git</project-url>
  <project-git-branches>main</project-git-branches>
  <combinations>
   <combination>
     <command-arguments></command-arguments>
     <git-branch>main</git-branch>
     <modules>
       <module>
         <path>./websockets/websocket-next/target/failsafe-reports/failsafe-summary.xml</path>
         <name>Quarkus QE TS: Websocket Next</name>
         <completed>24</completed>
         <skipped>1</skipped>
       </module>
     </modules>
     <completed>2294</completed>
     <skipped>51</skipped>
     <summary>Total number of executed tests is 2294 while 51 tests were skipped for combination 'jvm-mode', git branch 'main'</summary>
   </combination>
  </combinations>
</report>
```
