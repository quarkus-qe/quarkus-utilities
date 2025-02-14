# Disabled Tests Inspector

## Overview
**Disabled Tests Inspector** is a tool that analyzes Java test code in a GitHub repository, identifying disabled tests and categorizing them by annotation type and module. It generates a JSON report summarizing the findings.

## Features
- Scans Java test files for disabled test annotations
- Extracts reasons and issue links from annotations and comments
- Categorizes disabled tests per module
- Generates structured JSON reports

## Requirements
- Java 17+
- Maven 3.8.6+
- A GitHub personal access token

## Usage example
If you want to inspect multiple branches, list them comma-separated in `branches` property as described in the example below:
```shell
export GITHUB_OAUTH=your_personal_access_token  
mvn clean install
java -DrepoOwner=repoOwner -DrepoName=repoName -Dbranches="main,3.15" -DbaseOutputFileName=disabled-tests -jar target/quarkus-app/quarkus-run.jar
```

## Example output
After running the tool, you will get the following output files:

1. `disabled-tests-3.15.json` – A detailed list of all disabled tests in `3.15` branch
2. `disabled-tests-3.15-stats.json` – A summary of disabled tests per annotation type per module in `3.15` branch
3. `disabled-tests-main.json` – A detailed list of all disabled tests in `main` branch
4. `disabled-tests-main-stats.json` – A summary of disabled tests per annotation type per module in `main` branch
