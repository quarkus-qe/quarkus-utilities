package io.quarkus.qe;

import io.restassured.path.xml.XmlPath;
import org.apache.commons.lang3.StringUtils;
import org.grep4j.core.model.Profile;
import org.grep4j.core.model.ProfileBuilder;
import org.grep4j.core.result.GrepResult;
import org.grep4j.core.result.GrepResults;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static org.grep4j.core.Grep4j.constantExpression;
import static org.grep4j.core.Grep4j.grep;
import static org.grep4j.core.fluent.Dictionary.option;
import static org.grep4j.core.fluent.Dictionary.with;
import static org.grep4j.core.options.Option.extraLinesAfter;

public final class Artifact {
    private final Path file;
    private final String baseFileName;
    private final Path rootDirectory; // only for toString

    public Artifact(Path file, Path rootDirectory) {
        this.file = file;
        this.baseFileName = file.getFileName().toString();
        this.rootDirectory = rootDirectory;
    }

    public Path file() {
        return file;
    }

    public Path directory() {
        return file.getParent();
    }

    public String baseFileName() {
        return baseFileName;
    }

    public Path relativePath() {
        return rootDirectory.relativize(file);
    }

    public String artifactIdInParentDirName() {
        return file.getParent().getParent().getFileName().toString();
    }

    public String versionInParentDirName() {
        return file.getParent().getFileName().toString();
    }

    public boolean isPom() {
        return baseFileName.endsWith(".pom");
    }

    public Pom asPom() {
        return new Pom();
    }

    @Override
    public String toString() {
        return "artifact " + rootDirectory.relativize(file);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Artifact)) {
            return false;
        }

        Artifact artifact = (Artifact) o;
        return Objects.equals(file, artifact.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(file);
    }

    public final class Pom {
        private static final String DEPENDENCY_TAG = "<dependency>";
        private static final String DEPENDENCY_CLOSING_TAG = "</dependency>";
        private static final String GROUP_ID_TAG = "<groupId>";
        private static final String ARTIFACT_ID_TAG = "<artifactId>";
        private static final String VERSION_TAG = "<version>";
        private static final String CLOSING_TAG_START = "</";
        private static final String EXCLUSIONS_TAG = "<exclusions>";

        private Pom() {
            if (!isPom()) {
                throw new IllegalStateException("Not a POM: " + Artifact.this);
            }
        }

        public VersionedCoordinates versionedCoordinates() {
            XmlPath xml = XmlPath.from(file.toFile());
            String artifactId = xml.getString("project.artifactId");
            String groupId = xml.getString("project.groupId");
            if (groupId == null || groupId.isEmpty()) {
                groupId = xml.getString("project.parent.groupId");
            }
            String version = xml.getString("project.version");
            if (version == null || version.isEmpty()) {
                version = xml.getString("project.parent.version");
            }

            return new VersionedCoordinates(groupId, artifactId, version);
        }

        public Coordinates coordinates() {
            return versionedCoordinates().withoutVersion();
        }

        public VersionedCoordinates parentVersionedCoordinates() {
            XmlPath xml = XmlPath.from(file.toFile());
            String artifactId = xml.getString("project.parent.artifactId");
            String groupId = xml.getString("project.parent.groupId");
            String version = xml.getString("project.parent.version");

            return new VersionedCoordinates(groupId, artifactId, version);
        }

        public Coordinates parentCoordinates() {
            return parentVersionedCoordinates().withoutVersion();
        }

        public Stream<String> getDependenciesGav() {
            final Profile pomFile = ProfileBuilder.newBuilder()
                    .name("POM file")
                    .filePath(file.toString())
                    .onLocalhost()
                    .build();
            final GrepResults grepResults = grep(
                    constantExpression("<dependency>"),
                    pomFile,
                    with(option(extraLinesAfter(6))));
            return grepResults.stream()
                    .map(GrepResult::getText)
                    .flatMap(this::extractDependenciesGav);
        }

        private Stream<String> extractDependenciesGav(String grepResult) {
            final String[] dependencies = StringUtils.substringsBetween(grepResult, DEPENDENCY_TAG, DEPENDENCY_CLOSING_TAG);
            if (dependencies == null) {
                return Stream.empty();
            }
            return Arrays.stream(dependencies)
                    .map(dep -> StringUtils.substringBefore(dep, EXCLUSIONS_TAG))
                    .map(dep -> {
                        final String groupId = StringUtils.substringBetween(dep, GROUP_ID_TAG, CLOSING_TAG_START);
                        final String artifactId = StringUtils.substringBetween(dep, ARTIFACT_ID_TAG, CLOSING_TAG_START);
                        final String version = StringUtils.substringBetween(dep, VERSION_TAG, CLOSING_TAG_START);
                        return String.format("%s:%s:%s", groupId, artifactId, version == null ? "" : version);
                    });
        }
    }
}
