package io.quarkus.qe.utils;

record MavenCoordinates(String group, String artifact, String version) {
    public static MavenCoordinates parse(String source) {
        String[] parts = source.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid maven coordinates: " + source);
        }
        return new MavenCoordinates(parts[0], parts[1], parts[2]);
    }

    public MavenCoordinates withArtifact(String artifact) {
        return new MavenCoordinates(group, artifact, version);
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s", group, artifact, version);
    }
}
