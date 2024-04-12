package io.quarkus.qe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.quarkus.qe.PrepareOperation.VERSION_PLUGIN_OUTPUT_FILE_NAME;

public class GenerateReport {

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
            <div>
                <table>""";

    public static final String HTML_BASE_END = """
                </table>
            </div>
            </body>
            </html>""";
    public static final String ALLOWED_ARTIFACTS_BASE = "_allowed_artifacts.yaml";
    public TreeMap<String, Artifact> differentArtifacts = new TreeMap<>();

    Map<String, List<String>> allowedArtifacts;

    private boolean majorMinorVersionDiffer = false;
    private boolean patchOrOthersVersionDiffer = false;

    private final Path quarkusRepoDirectory;

    public GenerateReport(Path quarkusRepoDirectory) {
        this.quarkusRepoDirectory = quarkusRepoDirectory;
        createAllowedHashMap();
    }

    public void generateReport() {
        List<Path> dependencyDiffFiles = generateListOfDiffFiles();

        for (Path path : dependencyDiffFiles) {

            try (Stream<String> lines = Files.lines(path)) {
                lines.forEach(line -> {
                    Pattern artifactPattern = Pattern.compile("[\\w.-]+:[\\w.-]+");
                    Pattern versionsPattern = Pattern.compile("[\\S\\d.]+.->.+");
                    Matcher artifactMatcher = artifactPattern.matcher(line);
                    Matcher versionsMatcher = versionsPattern.matcher(line);
                    if (artifactMatcher.matches() && !versionsMatcher.matches()) {
                        throw new RuntimeException("Matcher not found");
                    }
                    if (artifactMatcher.find() && versionsMatcher.find()) {
                        String versionsTogether = versionsMatcher.group();
                        String[] versions = versionsTogether.replaceAll("\\s+", "").split("->");
                        if (!versionComparator(versions[0], versions[1])) {
                            String artifact = artifactMatcher.group();
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

        try(FileWriter fw = new FileWriter("outputDiff.html")) {
            fw.write(HTML_BASE_START);
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
            fw.write(HTML_BASE_END);
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

        try(FileWriter fw = new FileWriter("outputDiffDetailed.html")) {
            fw.write(HTML_BASE_START);
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
            fw.write(HTML_BASE_END);
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
     * Comparing version if they are the same.
     * Also modify them as they are differences between upstream and productized artifact
     * @param upstreamVersion Version of upstream artifact
     * @param downstreamVersion version of downstream artifact
     * @return false if the version are not the same
     */
    public boolean versionComparator(String upstreamVersion, String downstreamVersion) {
        String version = upstreamVersion.replace("-alpha", ".alpha");

        // match any artifact which have only major and minor version
        Pattern pattern = Pattern.compile("^(\\d+\\.\\d+$)");
        Matcher matcher = pattern.matcher(version);
        // match any artifact which have only major and minor version and some suffix
        Pattern pattern2 = Pattern.compile("^(\\d+\\.\\d+((\\.|-)\\D))");
        Matcher matcher2 = pattern2.matcher(version);
        if (matcher.find()) {
            version += ".0";
        }
        if (matcher2.find()) {
            String versionHelper = matcher2.group();
            version = version.substring(0, versionHelper.length() -2) + ".0" + version.substring(versionHelper.length() -2);
        }
        return downstreamVersion.contains(version);
    }

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

    /**
     * Creating the hashmap with artifacts and version which are allowed to have different version from upstream
     */
    public void createAllowedHashMap() {
        String quarkusVersion = Objects.requireNonNullElse(System.getProperty("quarkus-version"), "");
        if (quarkusVersion.isBlank()) {
            return;
        }
        allowedArtifacts = new HashMap<>();
        String resourceName = "/" + quarkusVersion + ALLOWED_ARTIFACTS_BASE;
        try {
            InputStream inputStream = this.getClass().getResourceAsStream(resourceName);
            ObjectMapper om = new ObjectMapper(new YAMLFactory());
            AllowedArtifacts allowedArtifactList = om.readValue(inputStream, AllowedArtifacts.class);

            for (AllowedArtifacts.AllowedArtifact allowedArtifact : allowedArtifactList.getAllowedArtifact()) {
                if (!allowedArtifacts.containsKey(allowedArtifact.getArtifact())) {
                    allowedArtifacts.put(allowedArtifact.getArtifact(), new ArrayList<>());
                }
                for (String version : allowedArtifact.getRhbqVersions()) {
                    allowedArtifacts.get(allowedArtifact.getArtifact()).add(version);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error when loading allowed file " + resourceName + ". Log trace:", e);
        }
    }


    public boolean isMajorMinorVersionDiffer() {
        return majorMinorVersionDiffer;
    }

    public boolean isPatchOrOthersVersionDiffer() {
        return patchOrOthersVersionDiffer;
    }
}
