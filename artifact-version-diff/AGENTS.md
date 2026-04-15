# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Project Overview

This is a Java Maven utility for comparing artifact versions between upstream Quarkus and Red Hat Build of Quarkus (RHBQ). The tool analyzes dependency differences across the Quarkus ecosystem and generates detailed HTML reports highlighting version discrepancies.

**Main Technologies:**
- Java 17
- Maven 3.x
- Jackson (YAML processing)
- Maven Artifact API
- JUnit 5 (testing)

**Core Functionality:**
1. Clones the Quarkus repository at a specific tag
2. Executes `mvn versions:compare-dependencies` to identify version differences
3. Compares platform BOMs between upstream and RHBQ
4. Generates HTML reports with color-coded version differences
5. Validates differences against allowed artifact lists

## Building and Running

### Prerequisites
- Java 17+
- Maven 3.x
- Git (for cloning Quarkus repository)

### Build the Project
```bash
mvn clean install
```

### Run the Main Application
```bash
mvn exec:java
```

**Required System Properties:**
- `quarkus.platform.bom`: RHBQ platform BOM coordinates (e.g., `com.redhat.quarkus.platform:quarkus-bom:3.27.3.redhat-00003`)
- `quarkus.repo.tag`: Upstream Quarkus version/tag (e.g., `3.27.3`)

**Optional System Properties:**
- `maven.repo.local`: Custom Maven local repository path
- `quarkus-version`: Version prefix for loading allowed artifacts file (e.g., `3.27`)

### Example Usage via Shell Script
```bash
./prod_vs_upstream.sh 3.27.3.redhat-00003 /path/to/maven/repo
```

### Run Tests
```bash
mvn test
```

## Key Components

### Main Entry Point
- **Main.java**: Orchestrates the comparison workflow by calling PrepareOperation, GenerateVersionDiffReport, and GeneratePomComparison

### Core Classes
- **PrepareOperation**: Clones Quarkus repository and executes Maven versions plugin
- **GenerateVersionDiffReport**: Analyzes version differences and generates HTML reports
- **GeneratePomComparison**: Compares platform BOMs and identifies missing/extra artifacts
- **PomComparator**: Parses and compares Maven POM files
- **AllowedArtifacts**: Manages YAML configuration for allowed version differences
- **Artifact**: Data model for artifact version information

### Output Files
The tool generates three HTML reports:
1. **outputDiff.html**: Simple overview of version differences
2. **outputDiffDetailed.html**: Detailed report with affected locations
3. **platformBomComparison.html**: BOM comparison showing missing/extra artifacts

### Allowed Artifacts Configuration
YAML files in `src/main/resources/` define allowed version differences:
- Format: `{version}_allowed_artifacts.yaml` (e.g., `3.27_allowed_artifacts.yaml`)
- Structure:
  - `allowed-artifacts`: List of artifacts with permitted RHBQ versions
  - `allowed-missing-artifacts-bom-comparison`: Artifacts allowed to be missing in RHBQ BOM
  - `allowed-extra-artifacts-bom-comparison`: Artifacts allowed to be extra in RHBQ BOM

## Development Conventions

### Version Comparison Logic
- **Major/Minor differences**: Highlighted in red (older) or yellow (newer)
- **Patch differences**: Tracked but not highlighted
- **Allowed artifacts**: Must match patterns in YAML configuration (supports wildcards with `*`)

### Test Strategy
- Tests validate that version differences are within acceptable bounds
- Major/minor version differences cause test failures
- Patch-only differences mark builds as unstable
- BOM comparison tests ensure all differences are in allowed lists

### Code Organization
- Main source: `src/main/java/io/quarkus/qe/`
- Resources: `src/main/resources/` (allowed artifacts YAML files)
- Tests: `src/test/java/io/quarkus/qe/`

### Error Handling
- Uses RuntimeException for critical failures
- Validates required system properties at startup
- Provides detailed error messages with context

### Logging
- Uses `java.util.logging.Logger` for all logging
- Log levels: INFO for progress, detailed error messages on failures

## Working with This Codebase

When modifying this tool:
1. **Adding new allowed artifacts**: Update or create version-specific YAML files in `src/main/resources/`
2. **Changing comparison logic**: Focus on `GenerateVersionDiffReport.differencesInVersions()`
3. **Modifying HTML output**: Update HTML templates in `HTML_BASE_START` and `HTML_BASE_END` constants
4. **Adding new validations**: Extend test cases in `DiffReportTest.java`
5. **Handling complex versions**: See `setComplicatedArtifactVersion()` for special version parsing logic

### Important Notes
- The tool creates temporary directories for Quarkus repository clones
- Maven versions plugin output files are named `depDiffs.txt`
- Regex patterns in allowed artifacts use `.*` instead of `*` for matching
- Version comparison uses `DefaultArtifactVersion` from Maven Artifact API
