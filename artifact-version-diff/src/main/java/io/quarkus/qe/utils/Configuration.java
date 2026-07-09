package io.quarkus.qe.utils;

import org.apache.commons.lang3.RandomStringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class Configuration {
    private final String upstreamVersion;
    private final MavenCoordinates platformBom;
    private final Path tmpFolder;

    public Configuration() {
        upstreamVersion = Objects.requireNonNull(System.getProperty("quarkus.repo.tag"), "The quarkus.repo.tag wasn't set.");
        platformBom = MavenCoordinates.parse(Objects.requireNonNull(System.getProperty("quarkus.platform.bom"), "The quarkus.platform.bom wasn't set."));
        String generatedRandomDirName = "artifact-comparison-" + RandomStringUtils.randomAlphabetic(5);
        this.tmpFolder=Paths.get(System.getProperty("java.io.tmpdir"), generatedRandomDirName);
    }

    // for unit tests only
    Configuration(String upstreamVersion, String platformBom, Path tmpFolder) {
        this.upstreamVersion = upstreamVersion;
        this.platformBom = MavenCoordinates.parse(platformBom);
        this.tmpFolder = tmpFolder;
    }

    public String getUpstreamVersion() {
        return upstreamVersion;
    }

    public boolean isQuarkusVersionAtLeast(int major, int minor) {;
        Version current = Version.parse(this.upstreamVersion);
        if (current.major > major) {
            return true;
        }
        if (current.major < major) {
            return false;
        }
        return current.minor >= minor;
    }

    public Path getWorkingDirectory() {
        return this.tmpFolder;
    }
    public String getRHBQVersion() {
        return platformBom.version();
    }

    public String getPlatformBom() {
        return platformBom.toString();
    }

    public String getMCPBom() {
        return platformBom.withArtifact("quarkus-mcp-server-bom").toString();
    }

    public String getQLC4JBom() {
        return platformBom.withArtifact("quarkus-langchain4j-bom").toString();
    }

    record Version(int major, int minor) {
        static String versionSeparator = "\\.";

        static Version parse(String source) {
            String[] arr = source.split(versionSeparator);
            if (arr.length < 2) {
                throw new IllegalArgumentException("Invalid version: " + source);
            }
            int major = Integer.parseInt(arr[0]);
            int minor = Integer.parseInt(arr[1]);
            return new Version(major, minor);
        }
    }
}
