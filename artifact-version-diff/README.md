# Artifact Version Diff Tool

A Java Maven utility for comparing artifact versions between upstream Quarkus and Red Hat Build of Quarkus (RHBQ). This tool analyzes dependency differences across the Quarkus ecosystem and generates detailed HTML reports highlighting version discrepancies.

## Features

- 🔍 Automated comparison of artifact versions between upstream Quarkus and RHBQ
- 📊 Three detailed HTML reports with color-coded differences
- ✅ Configurable allowed version differences via YAML files
- 🧪 Automated testing to validate version differences
- 🎯 Platform BOM comparison to identify missing/extra artifacts

## Prerequisites

- Java 17 or higher
- Maven 3.x
- Git (for cloning Quarkus repository)

## Installation

Clone the repository and build the project:

```bash
git clone <repository-url>
cd artifact-version-diff
mvn clean install
```

## Usage

### Using Maven Exec Plugin

```bash
mvn exec:java \
  -Dquarkus.platform.bom=com.redhat.quarkus.platform:quarkus-bom:3.27.3.redhat-00003 \
  -Dquarkus.repo.tag=3.27.3 \
  -Dmaven.repo.local=/path/to/maven/repo \
  -Dquarkus-version=3.27
```

### Using the Shell Script

A convenience script is provided for easier execution:

```bash
./prod_vs_upstream.sh <RHBQ_VERSION> <MAVEN_LOCAL_REPO>
```

Example:
```bash
./prod_vs_upstream.sh 3.27.3.redhat-00003 /path/to/maven/repo
```

### System Properties

| Property | Required | Description | Example |
|----------|----------|-------------|---------|
| `quarkus.platform.bom` | Yes | RHBQ platform BOM coordinates | `com.redhat.quarkus.platform:quarkus-bom:3.27.3.redhat-00003` |
| `quarkus.repo.tag` | Yes | Upstream Quarkus version/tag | `3.27.3` |
| `maven.repo.local` | No | Custom Maven local repository path | `/path/to/maven/repo` |
| `quarkus-version` | No | Version prefix for loading allowed artifacts file | `3.27` |

## Output Files

The tool generates three HTML reports in the project root directory:

### 1. outputDiff.html
Simple overview showing:
- Artifact names
- Version differences (upstream → RHBQ)
- Color coding: 🟡 Yellow (newer), 🔴 Red (older)

### 2. outputDiffDetailed.html
Detailed report including:
- All information from simple report
- Locations of affected extensions/modules
- Full dependency tree context

### 3. platformBomComparison.html
Platform BOM comparison showing:
- Missing artifacts in RHBQ BOM
- Extra artifacts in RHBQ BOM
- Validation against allowed lists

## Configuration

### Allowed Artifacts

Version differences can be whitelisted using YAML configuration files in `src/main/resources/`:

**File naming convention:** `{version}_allowed_artifacts.yaml`

Example: `3.27_allowed_artifacts.yaml`

```yaml
allowed-artifacts:
  - artifact: 'com.aayushatharva.brotli4j:brotli4j'
    allowed-rhbq-versions:
      - 1.14.0.redhat-00001
      - 1.15.0.redhat-00001
  - artifact: 'net.java.dev.jna:jna-platform'
    allowed-rhbq-versions:
      - 5.8.0

allowed-missing-artifacts-bom-comparison:
  - io.quarkus:quarkus-rest-client-mutiny

allowed-extra-artifacts-bom-comparison:
  - com.aayushatharva.brotli4j:native-linux-ppc64le
  - io.netty:netty-transport-native-epoll-ppcle
```

**Wildcard Support:** Artifact patterns support wildcards using `*`:
```yaml
- artifact: 'io.quarkus:*'
  allowed-rhbq-versions:
    - 3.27.3.redhat-00003
```

## Testing

Run the test suite:

```bash
mvn test
```

### Test Behavior

- ✅ **Pass**: All version differences are within allowed bounds
- ❌ **Fail**: Major or minor version differences detected
- ⚠️ **Unstable**: Only patch version differences detected

## How It Works

1. **Clone Repository**: Clones the upstream Quarkus repository at the specified tag
2. **Execute Maven Plugin**: Runs `mvn versions:compare-dependencies` to generate version comparison files
3. **Parse Results**: Analyzes the generated `depDiffs.txt` files across the repository
4. **Compare BOMs**: Downloads and compares platform BOMs between upstream and RHBQ
5. **Validate**: Checks differences against allowed artifacts configuration
6. **Generate Reports**: Creates three HTML reports with color-coded results

## Project Structure

```
artifact-version-diff/
├── src/
│   ├── main/
│   │   ├── java/io/quarkus/qe/
│   │   │   ├── Main.java                      # Entry point
│   │   │   ├── PrepareOperation.java          # Repository cloning & Maven execution
│   │   │   ├── GenerateVersionDiffReport.java # Version comparison & HTML generation
│   │   │   ├── GeneratePomComparison.java     # BOM comparison
│   │   │   ├── PomComparator.java             # POM parsing & comparison
│   │   │   ├── AllowedArtifacts.java          # YAML configuration handling
│   │   │   └── Artifact.java                  # Artifact data model
│   │   └── resources/
│   │       └── *_allowed_artifacts.yaml       # Version-specific configurations
│   └── test/
│       └── java/io/quarkus/qe/
│           └── DiffReportTest.java            # Test suite
├── pom.xml
├── prod_vs_upstream.sh                        # Convenience script
└── README.md
```

## Development

### Adding New Allowed Artifacts

1. Create or update the version-specific YAML file in `src/main/resources/`
2. Add artifact patterns and allowed versions
3. Run tests to validate configuration

### Modifying Comparison Logic

The main comparison logic is in `GenerateVersionDiffReport.differencesInVersions()`. This method:
- Compares major and minor versions
- Applies color coding for HTML output
- Validates against allowed artifacts

### Handling Complex Versions

Some artifacts have complex version schemes (e.g., `23.1.2.0-3-redhat-00001`). The `setComplicatedArtifactVersion()` method handles special parsing for these cases.

## Troubleshooting

### Common Issues

**Issue:** `The quarkus.platform.bom property wasn't set`
- **Solution:** Ensure you provide the `-Dquarkus.platform.bom` system property

**Issue:** `Failed to clone Quarkus repository`
- **Solution:** Check your internet connection and Git configuration

**Issue:** `Error when loading allowed file`
- **Solution:** Verify the YAML file exists and matches the naming convention

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request
