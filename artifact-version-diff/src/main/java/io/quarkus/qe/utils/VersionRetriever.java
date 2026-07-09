package io.quarkus.qe.utils;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public record VersionRetriever(Properties properties) {

    public String getQuarkusVersion() {
        return getUpstreamVersion("quarkus.version");
    }

    public String getMcpVersion() {
        return getUpstreamVersion("quarkus-mcp-server.version");
    }

    public String getLangChain4jVersion() {
        return getUpstreamVersion("quarkus-langchain4j.version");
    }

    private String getUpstreamVersion(String key) {
        String property = properties.getProperty(key);
        if (property == null) {
            throw new IllegalStateException("The %s property wasn't set.".formatted(key));
        }
        String[] split = property.split("\\.");
        if (split.length < 3) {
            throw new IllegalStateException("The %s property has invalid value: %s".formatted(key, property));
        }
        return split[0] + '.' + split[1] + '.' + split[2];
    }

    public static VersionRetriever getVersionsFromPlatform(Path bom) throws IOException {
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(bom.toFile()));
            Properties properties = model.getProperties();
            return new VersionRetriever(properties);
        } catch (XmlPullParserException e) {
            throw new RuntimeException("Failed to parse file " + bom, e);
        }
    }
}