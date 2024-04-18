package io.quarkus.qe;

import org.apache.commons.lang3.RandomStringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PrepareOperation {

    public static final String VERSION_PLUGIN_OUTPUT_FILE_NAME = "depDiffs.txt";
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

}
