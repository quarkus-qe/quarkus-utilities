package io.quarkus.qe;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.qe.utils.Configuration;
import io.quarkus.qe.utils.Repository;
import io.quarkus.qe.utils.DependencyProcessor;
import io.quarkus.qe.utils.VersionRetriever;

import java.io.BufferedReader;
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
     *
     * @return path to directory which include Quarkus
     * @throws IOException
     */
    public static Path prepareVersionPluginOutput(Configuration config) throws IOException {
        Path tmpDirectory = Files.createDirectories(config.getWorkingDirectory());

        String branch = config.getUpstreamVersion();

        DependencyProcessor platform = new DependencyProcessor(Repository.PLATFORM,tmpDirectory);
        platform.cloneRepo(branch);
        VersionRetriever versions = VersionRetriever.getVersionsFromPlatform(platform.getDirectory().resolve("pom.xml"));

        String remotePom = config.getPlatformBom();
        DependencyProcessor core = new DependencyProcessor(Repository.CORE,tmpDirectory);
        core.cloneRepo(versions.getQuarkusVersion());
        core.compareVersions(remotePom);
        if (config.isQuarkusVersionAtLeast(3,27)) { // we started testing mcp and langchain4j in 3.27
            DependencyProcessor mcp = new DependencyProcessor(Repository.MCP,tmpDirectory);
            mcp.cloneRepo(versions.getMcpVersion());
            mcp.compareVersions(config.getMCPBom());

            DependencyProcessor langchain = new DependencyProcessor(Repository.LANGCHAIN4J,tmpDirectory);
            langchain.cloneRepo(versions.getLangChain4jVersion());
            langchain.compareVersions(config.getQLC4JBom());
        }
        return tmpDirectory;
    }


    public static void executeProcess(List<String> command, String errorMsg, Path path) {
        LOG.info("Executing " + String.join(" ", command) + ", " + path);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(path.toFile());
        try {
            Process process = builder.redirectErrorStream(true)
                    .directory(path.toFile())
                    .start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = process.inputReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();
            assertEquals(0, process.exitValue(), errorMsg + ". Output : " + output);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(errorMsg, e);
        }
    }

    /**
     * \
     * Prepare properties for versions maven plugin.
     * These properties are `maven.repo.local` (optional), `remotePom` and `reportOutputFile`
     *
     * @return list containing the properties
     */
    public static List<String> prepareMavenPropertiesForBom(String remotePom) {
        List<String> extraProperties = new ArrayList<>();
        String localRepo = Objects.requireNonNullElse(System.getProperty("maven.repo.local"), "");
        if (!localRepo.isEmpty()) {
            extraProperties.add("-Dmaven.repo.local=" + localRepo);
        }
        extraProperties.add("-DremotePom=" + remotePom);

        extraProperties.add("-DreportOutputFile=" + VERSION_PLUGIN_OUTPUT_FILE_NAME);
        extraProperties.add("-DreportMode=false");

        // This turn off maven INFO logs and show only ERROR logs
        extraProperties.add("--quiet");

        return extraProperties;
    }

    /**
     * Check for maven.repo.local property to propagated it. If the property is not set the default M2 home
     *
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
     *
     * @return path to downloaded platform bom
     */
    public static Path getUpstreamBom(Configuration config) {
        LOG.info("Executing mvn dependency:get");

        String upstreamVersion = config.getUpstreamVersion();
        List<String> mvnVersionsExecute = new ArrayList<>(
                Arrays.asList("mvn", "dependency:get", "-Dartifact=io.quarkus.platform:quarkus-bom:" + upstreamVersion + ":pom",
                        "-Dmaven.repo.local=" + getLocalRepository()));
        executeProcess(mvnVersionsExecute, "Failed to execute dependency:get for downloading upstream platform bom.",
                Paths.get("").toAbsolutePath());

        return Paths.get(getLocalRepository(), "io", "quarkus", "platform", "quarkus-bom", upstreamVersion, "quarkus-bom-" + upstreamVersion + ".pom");
    }

    /**
     * Get RHBQ platform bom from local repository and return its path
     *
     * @return path to platform bom
     */
    public static Path getRHBQBom(Configuration config) {
        String rhbqVersion = config.getRHBQVersion();
        return Paths.get(getLocalRepository(), "com", "redhat", "quarkus", "platform", "quarkus-bom", rhbqVersion, "quarkus-bom-" + rhbqVersion + ".pom");
    }

    /**
     * Creating the hashmap with artifacts and version which are allowed to have different version from upstream
     */
    public static Map<String, List<String>> createAllowedHashMap(AllowedArtifacts loadedAllowedArtifacts) {
        if (loadedAllowedArtifacts == null || loadedAllowedArtifacts.getVersionComparisonsArtifacts() == null) {
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
     *
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

