package io.quarkus.qe.utils;

import io.quarkus.qe.GenerateVersionDiffReport;
import io.quarkus.qe.PrepareOperation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled("We run tests in prod but this one only needed for checking the code itself")
public class UnitTests {
    @Test
    public void readPlatform() throws IOException {
        VersionRetriever versions = VersionRetriever.getVersionsFromPlatform(Path.of("target/test-classes/pom.xml"));
        assertEquals("3.27.4", versions.getQuarkusVersion());
        assertEquals("1.2.5", versions.getLangChain4jVersion());
        assertEquals("1.7.3", versions.getMcpVersion());
    }

    @Test
    public void compareVersions() {
        Configuration config = new Configuration("3.33.1", "com.redhat.quarkus.platform:quarkus-bom:3.33.3.redhat-00001", null);
        assertTrue(config.isQuarkusVersionAtLeast(3,33));
        assertTrue(config.isQuarkusVersionAtLeast(3,27));
        assertFalse(config.isQuarkusVersionAtLeast(3,35));

        assertEquals("com.redhat.quarkus.platform:quarkus-bom:3.33.3.redhat-00001", config.getPlatformBom());
        assertEquals("3.33.3.redhat-00001", config.getRHBQVersion());
        assertEquals("com.redhat.quarkus.platform:quarkus-langchain4j-bom:3.33.3.redhat-00001", config.getQLC4JBom());
        assertEquals("com.redhat.quarkus.platform:quarkus-mcp-server-bom:3.33.3.redhat-00001", config.getMCPBom());
    }

    @Test
    public void checkMavenCoordinates() {
        String source = "com.redhat.quarkus.platform:quarkus-bom:3.33.3.redhat-00001";
        MavenCoordinates coordinates = MavenCoordinates.parse(source);
        assertEquals("com.redhat.quarkus.platform", coordinates.group());
        assertEquals("3.33.3.redhat-00001", coordinates.version());
        assertEquals("quarkus-bom", coordinates.artifact());
        assertEquals(source, coordinates.toString());
    }

    @Test
    public void fullRun() throws IOException {
        Path path = Path.of("target/repos").toAbsolutePath();
        Configuration config = new Configuration("3.33.1",
                "com.redhat.quarkus.platform:quarkus-bom:3.33.3.redhat-00001",
                path);

        Path directory = PrepareOperation.prepareVersionPluginOutput(config);
        GenerateVersionDiffReport report = new GenerateVersionDiffReport(directory,
                PrepareOperation.loadAllowedArtifactFile(),
                config);
        List<Path> paths = report.generateListOfDiffFiles(directory);
        Assertions.assertEquals(986, paths.size());
    }
}
