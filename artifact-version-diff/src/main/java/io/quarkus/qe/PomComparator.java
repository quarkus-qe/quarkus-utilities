package io.quarkus.qe;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PomComparator {

    private Path upstreamPlatformBom;
    private Path rhbqPlatformBom;
    private Map<String, Dependency> rhbqPlatformBomDependencies;
    private List<Dependency> missingDependencies = new ArrayList<>();

    private static final Logger LOG = Logger.getLogger(PomComparator.class.getName());

    public PomComparator() throws IOException {
        preparePlatformBoms();
    }

    public void preparePlatformBoms() throws IOException {
        upstreamPlatformBom = PrepareOperation.getUpstreamBom();
        rhbqPlatformBom = PrepareOperation.getRHBQBom();
        if (!upstreamPlatformBom.toFile().exists() || !rhbqPlatformBom.toFile().exists()) {
            throw new FileNotFoundException("The upstream or RHBQ platform bom doesn't exist");
        }
    }

    /**
     * Load upstream and RHBQ platform boms and compare them. It creates List for RHBQ missing upstream defined artifacts,
     * List for RHBQ additional/extra artifacts and List for artifacts with different versions.
     * The different version should be same as version comparison in GenerateVersionDiffReport.
     * However, this check is not that thorough as in GenerateVersionDiffReport
     * @throws IOException
     * @throws XmlPullParserException
     */
    public void comparePoms() throws IOException, XmlPullParserException {
        LOG.info("Upstream platform bom location: " + upstreamPlatformBom.toAbsolutePath());
        LOG.info("RHBQ platform bom location: " + rhbqPlatformBom.toAbsolutePath());
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model upstreamPlatformBomModel = reader.read(new FileReader(upstreamPlatformBom.toFile()));
        Model rhbqPlatformBomModel = reader.read(new FileReader(rhbqPlatformBom.toFile()));

        // Load upstream bom and remove duplicate artifacts as we don't want to check for classifier. Example:
        // `netty-transport-native-unix-common` have multiple classifier like osx-x86_64, linux-x86_64
        List<Dependency> upstreamDependencies = upstreamPlatformBomModel.getDependencyManagement().getDependencies().stream()
                .collect(Collectors.toMap(
                        dep -> dep.getGroupId() + ":" + dep.getArtifactId(), dep -> dep, (dep1, dep2) -> dep2))
                .values().stream().toList();

        // Load RHBQ bom and change it to map for easier comparing
        List<Dependency> rhbqDependencies = rhbqPlatformBomModel.getDependencyManagement().getDependencies();
        rhbqPlatformBomDependencies = rhbqDependencies.stream().collect(Collectors.toMap(dep -> dep.getGroupId() + ":" + dep.getArtifactId(), Function.identity(), (artifact1, artifact2) -> artifact1));

        for (Dependency dependency : upstreamDependencies) {
            String key = dependency.getGroupId() + ":" + dependency.getArtifactId();
            // As RHBQ rename platform groupId from io.quarkus to com.redhat.quarkus this need to changed
            // as groupId:artifactId is used as key. In case of RHBQ it can't find these relocated artifacts
            if (key.contains("quarkus-bom-quarkus-platform")) {
                key = key.replace("io.quarkus", "com.redhat.quarkus");
            }
            if (!rhbqPlatformBomDependencies.containsKey(key)) {
                missingDependencies.add(dependency);
            } else {
                rhbqPlatformBomDependencies.remove(key);
            }
        }
    }

    /**
     *
     * @return mutable List of dependencies created from Map
     */
    public List<Dependency> getExtraDependencies() {
        return new ArrayList<>(rhbqPlatformBomDependencies.values());
    }

    public List<Dependency> getMissingDependencies() {
        return missingDependencies;
    }
}
