package io.quarkus.qe.utils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static io.quarkus.qe.PrepareOperation.executeProcess;
import static io.quarkus.qe.PrepareOperation.prepareMavenPropertiesForBom;

public class DependencyProcessor {
    private static final Logger LOG = Logger.getLogger(DependencyProcessor.class.getName());

    private final Path directory;
    private final Repository member;

    public DependencyProcessor(Repository member, Path directory) {
        this.directory = directory;
        this.member = member;
    }

    public void cloneRepo(String branch) {
        LOG.info("Cloning %s repository".formatted(this.member.getName()));
        if (this.getDirectory().toFile().exists()) {
            LOG.info("Directory %s exists, skipping cloning".formatted(this.getDirectory()));
            return;
        }
        List<String> cloneCommand = Arrays.asList("git", "clone", "--depth", "1", "--single-branch", "--branch", branch,
                member.getUrl(), this.member.getName());
        executeProcess(cloneCommand, "Failed to clone Quarkus repository", this.directory);
    }

    public void compareVersions(String remotePom) {
        LOG.info("Executing mvn versions:compare-dependencies");
        List<String> mvnVersionsExecute = new ArrayList<>(Arrays.asList("mvn", "--batch-mode", "--no-transfer-progress", "versions:compare-dependencies"));
        mvnVersionsExecute.addAll(prepareMavenPropertiesForBom(remotePom));
        Path quarkusRepoDirectory = this.getDirectory();
        executeProcess(mvnVersionsExecute, "Error when executing versions:compare-dependencies plugin", quarkusRepoDirectory);
    }

    public Path getDirectory() {
        return Paths.get(directory.toAbsolutePath().toString(), member.getName());
    }
}