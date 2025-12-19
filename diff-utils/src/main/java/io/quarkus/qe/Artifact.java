package io.quarkus.qe;

import io.restassured.path.xml.XmlPath;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;


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

        public Stream<String> getDependenciesGav() {
            try {
                File pomFile = new File(file.toString());
                MavenXpp3Reader mavenReader = new MavenXpp3Reader();
                XmlStreamReader streamReader = new XmlStreamReader(pomFile);
                Model model = mavenReader.read(streamReader);

                List<Dependency> dependencies = model.getDependencies();
                if (model.getDependencyManagement() != null){
                    dependencies.addAll(model.getDependencyManagement().getDependencies());
                }

                return dependencies.stream().map(dependency -> dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion());
            } catch (IOException | XmlPullParserException e){
                throw new RuntimeException(e);
            }
        }
    }
}
