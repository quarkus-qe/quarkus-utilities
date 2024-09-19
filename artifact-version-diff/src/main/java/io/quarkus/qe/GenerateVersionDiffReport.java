package io.quarkus.qe;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.quarkus.qe.PrepareOperation.VERSION_PLUGIN_OUTPUT_FILE_NAME;

public class GenerateVersionDiffReport {

    private static final Logger LOG = Logger.getLogger(GenerateVersionDiffReport.class.getName());

    public static final String HTML_BASE_START = """
            <!DOCTYPE html>
            <html>
            <style>
                table, td, th {
                    border: 1px solid;
                    vertical-align: top;
                    text-align: left;
                    padding: 5px;
                }

                table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .newer {
                    background-color: #FFDD88;
                }
                .older {
                    background-color: #EE9999;
                }
            </style>
            <body>
            <div><h1>Compared Quarkus versions substitute-quarkus-upstream vs substitute-quarkus-rhbq</h1></div>
            <div>
                <table>""";

    public static final String HTML_BASE_END = """
                </table>
            </div>
            </body>
            <footer>
                <p>Generated on substitute-date</p>
            </footer>
            </html>""";
    public TreeMap<String, Artifact> differentArtifacts = new TreeMap<>();

    Map<String, List<String>> allowedArtifacts;

    private boolean majorMinorVersionDiffer = false;
    private boolean patchOrOthersVersionDiffer = false;

    private final Path quarkusRepoDirectory;

    public GenerateVersionDiffReport(Path quarkusRepoDirectory, AllowedArtifacts allowedArtifactsFile) {
        this.quarkusRepoDirectory = quarkusRepoDirectory;
        this.allowedArtifacts = PrepareOperation.createAllowedHashMap(allowedArtifactsFile);
    }

    /**
     * Method load the generated files from versions plugin.
     * Parse file to found the artifact and it's version.
     * Version of the artifact are compared and if they are not the same the artifact and its version are added to map
     * where the key is name of the artifact.
     * <p>
     * When map with artifacts which differ in version is loaded, simple and detailed html report is created.
     */
    public void generateReport() {
        List<Path> dependencyDiffFiles = generateListOfDiffFiles();
        // Pattern for artifact in format <groupID>:<artifactID>
        Pattern artifactPattern = Pattern.compile("[\\w.-]+:[\\w.-]+");
        // Pattern toma match version in format `<upstream_version> -> <downstream_version>`
        Pattern versionsPattern = Pattern.compile("[\\S\\d.]+.->.+");

        for (Path path : dependencyDiffFiles) {
            try (Stream<String> lines = Files.lines(path)) {
                lines.forEach(line -> {
                    // Check the line of file which was generated from versions plugin
                    // Line should contain artifact and version in format <groupID>:<artifactID> .... <upstream_version> -> <downstream_version>
                    Matcher artifactMatcher = artifactPattern.matcher(line);
                    Matcher versionsMatcher = versionsPattern.matcher(line);
                    if (artifactMatcher.matches() && !versionsMatcher.matches()) {
                        throw new RuntimeException("Matcher not found");
                    }
                    if (artifactMatcher.find() && versionsMatcher.find()) {
                        String versionsTogether = versionsMatcher.group();
                        String artifact = artifactMatcher.group();
                        if (!Artifact.versionComparator(artifact, versionsTogether)) {
                            addToDifferentArtifacts(artifact,
                                    path.toString().replace(quarkusRepoDirectory.toString(), "").replace(VERSION_PLUGIN_OUTPUT_FILE_NAME, ""),
                                    versionsTogether);
                        }
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException("Error when reading diff file lines for different dependencies. Error log: " + e);
            }
        }
        generateSimpleOverview();
        generateOverview();
    }

    /**
     * Generate simple report containing only artifact name and different versions
     * This output is saved in outputDiff.html file as table
     */
    public void generateSimpleOverview() {
        String tableHeader = """
                <tr>
                    <td>Artifact</td>
                    <td>Different version [upstream] -> [productized]</td>
                </tr>""";

        String template = """
                <tr>
                    <td>substitute-artifact</td>
                    <td class="substitute-class">substitute-version</td>
                </tr>
                """;

        LOG.info("Generating simple diff report");
        try(FileWriter fw = new FileWriter("outputDiff.html")) {
            fw.write(HTML_BASE_START.replace("substitute-quarkus-upstream", PrepareOperation.upstreamVersion)
                    .replace("substitute-quarkus-rhbq", PrepareOperation.rhbqVersion));
            fw.write(tableHeader);
            for (String artifact : differentArtifacts.keySet()) {
                List<String> versions = differentArtifacts.get(artifact).getDifferentVersions();
                for (int i = 0; i < versions.size(); i++) {
                    String writeCol = template;
                    writeCol = i == 0 ? writeCol.replace("substitute-artifact", artifact) : writeCol.replace("substitute-artifact", "");
                    writeCol = writeCol.replace("substitute-version", versions.get(i));
                    writeCol = writeCol.replace("substitute-class", differencesInVersions(artifact, versions.get(i)));
                    fw.write(writeCol);
                }
            }
            fw.write(HTML_BASE_END.replace("substitute-date", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS").format(new java.util.Date())));
        } catch (IOException e) {
            throw new RuntimeException("Unable to save output file. Log: " + e);
        }
    }

    /**
     * Generate simple report containing artifact name, different versions and location which are affected
     * This output is saved in outputDiff.html file as table
     */
    public void generateOverview() {
        String tableHeader = """
                <tr>
                    <td>Artifact</td>
                    <td>Different version [upstream] -> [productized]</td>
                    <td>Location of what is affected</td>
                </tr>""";

        String template = """
                <tr>
                    <td>substitute-artifact</td>
                    <td>substitute-version</td>
                    <td>substitute-locations</td>
                </tr>
                """;

        LOG.info("Generating detailed diff report");
        try(FileWriter fw = new FileWriter("outputDiffDetailed.html")) {
            fw.write(HTML_BASE_START.replace("substitute-quarkus-upstream", PrepareOperation.upstreamVersion)
                    .replace("substitute-quarkus-rhbq", PrepareOperation.rhbqVersion));
            fw.write(tableHeader);
            for (String artifact : differentArtifacts.keySet()) {
                List<String> versions = differentArtifacts.get(artifact).getDifferentVersions();
                for (int i = 0; i < versions.size(); i++) {
                    String writeColl = template;
                    writeColl = i == 0 ? writeColl.replace("substitute-artifact", artifact) : writeColl.replace("substitute-artifact", "");
                    writeColl = writeColl.replace("substitute-version", versions.get(i));
                    writeColl = writeColl.replace("substitute-location", differentArtifacts.get(artifact).locationForVersion(versions.get(i)));
                    fw.write(writeColl);
                }
            }
            fw.write(HTML_BASE_END.replace("substitute-date", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS").format(new java.util.Date())));
        } catch (IOException e) {
            throw new RuntimeException("Unable to save output file. Log: " + e);
        }
    }

    /**
     * Generate list of files generated by versions-maven-plugin inside of Quarkus repository
     * @return List of paths to generated files
     */
    public List<Path> generateListOfDiffFiles() {
        try (Stream<Path> entries = Files.walk(quarkusRepoDirectory)) {
            return entries.filter(Files::isRegularFile)
                    .filter(f -> f.getFileName().toString().contains(VERSION_PLUGIN_OUTPUT_FILE_NAME) &&
                            (f.toAbsolutePath().toString().contains(quarkusRepoDirectory + File.separator + "bom") ||
                                    f.toAbsolutePath().toString().contains(quarkusRepoDirectory + File.separator + "core") ||
                                    f.toAbsolutePath().toString().contains(quarkusRepoDirectory + File.separator + "extensions") ||
                                    f.toAbsolutePath().toString().contains(quarkusRepoDirectory + File.separator + "test-framework")))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Error when generating list of diff files. Error log: " + e);
        }
    }

    /**
     * Add second version to the different artifacts map or create new entry in map
     * @param artifact artifact name which is used as key in map
     * @param location is path what is the affected extension
     * @param versions upstream and downstream versions together
     */
    public void addToDifferentArtifacts(String artifact, String location, String versions) {
        if(differentArtifacts.containsKey(artifact)) {
            differentArtifacts.get(artifact).addArtifactVersionAndLocation(location, versions);
        } else {
            differentArtifacts.put(artifact, new Artifact(location, versions));
        }
    }

    /**
     * Checking the difference only between major and minor versions to highlight them in html table.
     * This also setting the flags for JUnit test to be able to modify the jenkins result as when there are only differences
     * in patch version or others
     * @param versionsTogether String of different version
     * @return html class name to highlight the table cells
     */
    public String differencesInVersions(String artifact, String versionsTogether) {
        String[] versions = versionsTogether.replaceAll("\\s+", "").split("->");
        DefaultArtifactVersion upstream = new DefaultArtifactVersion(versions[0]);
        DefaultArtifactVersion upstreamMajorMinor = new DefaultArtifactVersion(upstream.getMajorVersion() + "." +
                upstream.getMinorVersion());
        DefaultArtifactVersion downstream = new DefaultArtifactVersion(versions[1]);
        DefaultArtifactVersion downstreamMajorMinor = new DefaultArtifactVersion(downstream.getMajorVersion() + "." +
                downstream.getMinorVersion());

        // If the productized version is to complicated (graal-sdk for example 23.1.2.0-3-redhat-00001) `DefaultArtifactVersion`
        // not parse major and minor version correctly but get these values to 0.0
        // This check manually get major and minor version from productized version
        if (downstream.getMajorVersion() == 0 && downstream.getMinorVersion() == 0) {
            String[] complicateVersion = versions[1].split("\\.");
            downstreamMajorMinor = new DefaultArtifactVersion(complicateVersion[0] + "." + complicateVersion[1]);
        }

        int versionComparison = upstreamMajorMinor.compareTo(downstreamMajorMinor);
        if (allowedArtifacts != null && versionComparison < 0) {
            isAllowedWithDifferentVersion(true, artifact, versions[1]);
            return "newer";
        } else if (allowedArtifacts != null && versionComparison > 0) {
            isAllowedWithDifferentVersion(true, artifact, versions[1]);
            return "older";
        } else if (allowedArtifacts != null) {
            isAllowedWithDifferentVersion(false, artifact, versions[1]);
            return "";
        } else if (versionComparison < 0) {
            setMajorMinorPatch(true);
            return "newer";
        } else if (versionComparison > 0) {
            setMajorMinorPatch(true);
            return "older";
        } else {
            setMajorMinorPatch(false);
            return "";
        }
    }

    /**
     *
     * @param majorMinor is this comparison of the major or minor version
     * @param artifact artifact name
     * @param downstreamVersion downstream artifact version e.g. `1.15.0.redhat-00001`
     */
    public void isAllowedWithDifferentVersion(boolean majorMinor, String artifact, String downstreamVersion) {
        if (!allowedArtifacts.containsKey(artifact)) {
            setMajorMinorPatch(majorMinor);
            return;
        }

        boolean versionContainsAllowed = false;
        for (String version : allowedArtifacts.get(artifact)) {
            if (downstreamVersion.contains(version)) {
                versionContainsAllowed = true;
                break;
            }
        }
        if (!versionContainsAllowed) {
            setMajorMinorPatch(majorMinor);
        }
    }

    public void setMajorMinorPatch(boolean majorMinor) {
        if (majorMinor) {
            majorMinorVersionDiffer = true;
        } else {
            patchOrOthersVersionDiffer = true;
        }
    }


    public boolean isMajorMinorVersionDiffer() {
        return majorMinorVersionDiffer;
    }

    public boolean isPatchOrOthersVersionDiffer() {
        return patchOrOthersVersionDiffer;
    }
}
