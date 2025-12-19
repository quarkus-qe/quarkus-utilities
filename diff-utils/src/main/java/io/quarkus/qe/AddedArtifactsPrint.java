package io.quarkus.qe;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.jboss.logging.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AddedArtifactsPrint {

    private final String addedArtifactsListPath;
    private static MavenRepo repo;
    private static final char DEPENDENCY_INDEX_DELIMITER = '\t';
    private static final String DEPENDENCY_INDEX_PATH = AddedArtifactsPrint.class.getProtectionDomain().getCodeSource().getLocation().getPath() + "quarkus-dependency-index.txt";
    private static final Logger LOG = org.jboss.logging.Logger.getLogger(AddedArtifactsPrint.class);

    public AddedArtifactsPrint() {
        addedArtifactsListPath = Objects.requireNonNull(System.getProperty("quarkus.new-artifacts-list"),
                "System property 'quarkus.new-artifacts-list' expected");
        String mavenRepoDirStr = Objects.requireNonNull(System.getProperty("quarkus.maven.dir"),
                "System property 'quarkus.maven.dir' expected");
        repo = MavenRepo.at(Paths.get(mavenRepoDirStr).resolve("."));
    }

    public void printToFile() throws IOException {
        String currentWorkingDir = System.getProperty("user.dir");

        buildDependencyIndex();
        Multimap<Coordinates, String> allCoordinates = HashMultimap.create();
        try (Stream<Artifact> artifacts = repo.artifacts()) {
            artifacts
                    .filter(Artifact::isPom)
                    .peek(artifact -> LOG.debug(String.valueOf(artifact)))
                    .map(artifact -> artifact.asPom().versionedCoordinates())
                    .forEach(coords -> allCoordinates.put(coords.withoutVersion(), coords.version()));
        }

        // load all added artifacts into one string
        String addedArtifacts = Files.readString(Path.of(addedArtifactsListPath));

        try (FileWriter fileWriter = new FileWriter(currentWorkingDir + "/added_artifacts_deps.txt")) {
            PrintWriter printWriter = new PrintWriter(fileWriter);
            allCoordinates.keySet()
                    .forEach(coords -> {
                        // only do for artifacts that were added
                        // this is a dumb way to do a fulltext search to check it, but works
                        if (addedArtifacts.contains(coords.toString())) {
                            String version = allCoordinates.get(coords).toString();
                            printWriter.printf("\nDependants for %s - %s :: ADDED \n(%s)\n",
                                    coords, version, getDependentsInfo(coords));
                        }
                    });
        }
    }

    private void buildDependencyIndex() throws IOException {
        try (PrintWriter writer = new PrintWriter(DEPENDENCY_INDEX_PATH, StandardCharsets.UTF_8)) {
            repo.artifacts()
                    .filter(Artifact::isPom)
                    .map(Artifact::asPom)
                    .flatMap(this::getPomDependencyIndexEntries)
                    .forEach(writer::println);
        }
    }

    private Stream<String> getPomDependencyIndexEntries(Artifact.Pom pom) {
        final VersionedCoordinates versionedCoordinates = pom.versionedCoordinates();
        return pom.getDependenciesGav()
                .map(dependency -> String.format("%s%s%s", versionedCoordinates, DEPENDENCY_INDEX_DELIMITER, dependency));
    }

    private String getDependentsInfo(Coordinates coordinates) {
        final String dependents = grepDependencyIndex(String.format("%s:", coordinates));
        return dependents.isEmpty() ? "no dependents found" : String.format("dependents: %s", dependents);
    }

    private String grepDependencyIndex(String pattern) {
        try {
            return Files.lines(Path.of(DEPENDENCY_INDEX_PATH))
                    .filter(line -> line.contains(DEPENDENCY_INDEX_DELIMITER + pattern))
                    .map(line -> line.replace(String.valueOf(DEPENDENCY_INDEX_DELIMITER), " <- "))
                    .collect(Collectors.joining(", \n"));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
