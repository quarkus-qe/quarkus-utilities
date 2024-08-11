package io.quarkus.qe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PrepareOperation {

    public static final String VERSION_PLUGIN_OUTPUT_FILE_NAME = "depDiffs.txt";
    public static final String ALLOWED_ARTIFACTS_BASE = "_allowed_artifacts.yaml";
    private static final Logger LOG = Logger.getLogger(PrepareOperation.class.getName());

    /**
     * Clone Quarkus repository with specific tag and execute `mvn versions:compare-dependencies`
     * @return path to directory which include Quarkus
     * @throws IOException
     */
    public static Path prepareVersionPluginOutput() throws IOException {
        String generatedRandomDirName = "artifact-comparison-" + RandomStringUtils.randomAlphabetic(5);
        Path tmpDirectory = Files.createDirectories(Paths.get(System.getProperty("java.io.tmpdir"), generatedRandomDirName));

        LOG.info("Cloning Quarkus repository");
        String branch = Objects.requireNonNull(System.getProperty("quarkus.repo.tag"), "The quarkus.repo.tag property wasn't set.");
        List<String> gitCloneQuarkus = new ArrayList<>(
                Arrays.asList("git", "clone", "--single-branch", "--branch", branch, "https://github.com/quarkusio/quarkus.git"));
        executeProcess(gitCloneQuarkus, "Failed to clone Quarkus repository", tmpDirectory);

        LOG.info("Executing mvn versions:compare-dependencies");
        List<String> mvnVersionsExecute = new ArrayList<>(Arrays.asList("mvn", "versions:compare-dependencies"));
        mvnVersionsExecute.addAll(prepareMavenProperties());
        Path quarkusRepoDirectory = Paths.get(tmpDirectory.toAbsolutePath().toString(), "quarkus");
        executeProcess(mvnVersionsExecute, "Failed to add remote origin", quarkusRepoDirectory);

        return quarkusRepoDirectory;
    }

    public static void executeProcess(List<String> command, String errorMsg, Path path) {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(path.toFile());
        try {
            Process process = builder.redirectErrorStream(true)
                    .directory(path.toFile())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            process.waitFor();
            assertEquals(0, process.exitValue(), errorMsg);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**\
     * Prepare properties for versions maven plugin.
     * These properties are `maven.repo.local` (optional), `remotePom` and `reportOutputFile`
     * @return list containing the properties
     */
    public static List<String> prepareMavenProperties() {
        List<String> extraProperties = new ArrayList<>();
        String localRepo = Objects.requireNonNullElse(System.getProperty("maven.repo.local"), "");
        if (!localRepo.isEmpty()) {
            extraProperties.add("-Dmaven.repo.local=" + localRepo);
        }

        String remotePom = Objects.requireNonNull(System.getProperty("quarkus.platform.bom"), "The quarkus.platform.bom wasn't set.");
        extraProperties.add("-DremotePom=" + remotePom);

        extraProperties.add("-DreportOutputFile=" + VERSION_PLUGIN_OUTPUT_FILE_NAME);

        return extraProperties;
    }

    /**
     * Check for maven.repo.local property to propagated it. If the property is not set the default M2 home
     * @return String path to local repository
     */
    public static String getLocalRepository() {
        String localRepo = Objects.requireNonNullElse(System.getProperty("maven.repo.local"), "");
        if (localRepo.isEmpty()) {
            localRepo = Paths.get(System.getProperty("user.home"), ".m2", "repository").toString();
        }
        return localRepo;
    }

    /**
     * Download the upstream platform bom and return its path
     * @return path to downloaded platform bom
     */
    public static Path getUpstreamBom() {
        LOG.info("Executing mvn dependency:get");
        String version = Objects.requireNonNull(System.getProperty("quarkus.repo.tag"), "The quarkus.platform.bom wasn't set.");

        List<String> mvnVersionsExecute = new ArrayList<>(
                Arrays.asList("mvn", "dependency:get", "-Dartifact=io.quarkus.platform:quarkus-bom:" + version + ":pom",
                        "-Dmaven.repo.local=" + getLocalRepository()));
        executeProcess(mvnVersionsExecute, "Failed to execute dependency:get for downloading upstream platform bom.",
                Paths.get("").toAbsolutePath());

        return Paths.get(getLocalRepository(), "io", "quarkus", "platform", "quarkus-bom", version, "quarkus-bom-" + version + ".pom");
    }

    /**
     * Get RHBQ platform bom from local repository and return its path
     * @return path to platform bom
     */
    public static Path getRHBQBom() {
        String platformBom = Objects.requireNonNull(System.getProperty("quarkus.platform.bom"), "The quarkus.platform.bom wasn't set.");
        String version = platformBom.substring(platformBom.lastIndexOf(":") + 1);
        return Paths.get(getLocalRepository(), "com", "redhat", "quarkus", "platform", "quarkus-bom", version, "quarkus-bom-" + version + ".pom");
    }

    /**
     * Creating the hashmap with artifacts and version which are allowed to have different version from upstream
     */
    public static Map<String, List<String>> createAllowedHashMap(AllowedArtifacts loadedAllowedArtifacts) {
        if (loadedAllowedArtifacts == null) {
            return null;
        }
        Map<String, List<String>> allowedArtifacts = new HashMap<>();
        for (AllowedArtifacts.AllowedArtifact allowedArtifact : loadedAllowedArtifacts.getVersionComparisonsArtifacts()) {
            if (!allowedArtifacts.containsKey(allowedArtifact.getArtifact())) {
                allowedArtifacts.put(allowedArtifact.getArtifact(), new ArrayList<>());
            }
            for (String version : allowedArtifact.getRhbqVersions()) {
                allowedArtifacts.get(allowedArtifact.getArtifact()).add(version);
            }
        }
        return allowedArtifacts;
    }

    /**
     * Load allowed artifact file as object for check if some artifacts are allowed
     * @return loaded yaml file as object
     */
    public static AllowedArtifacts loadAllowedArtifactFile() {
        String quarkusVersion = Objects.requireNonNullElse(System.getProperty("quarkus-version"), "");
        if (quarkusVersion.isBlank()) {
            return null;
        }
        String resourceName = "/" + quarkusVersion + ALLOWED_ARTIFACTS_BASE;
        try {
            InputStream inputStream = PrepareOperation.class.getResourceAsStream(resourceName);
            ObjectMapper om = new ObjectMapper(new YAMLFactory());
            return om.readValue(inputStream, AllowedArtifacts.class);
        } catch (IOException e) {
            throw new RuntimeException("Error when loading allowed file " + resourceName + ". Log trace:", e);
        }
    }

}
