package io.quarkus.qe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Artifact {
    private HashMap<String, List<String>> duplicates;

    public Artifact(String location, String versions) {
        duplicates = new HashMap<>();
        List<String> locations = new ArrayList<>(Arrays.asList(location));
        duplicates.put(versions, locations);
    }

    public void addArtifactVersionAndLocation(String location, String versions) {
        if (duplicates.containsKey(versions)) {
            duplicates.get(versions).add(location);
        } else {
            List<String> locations = new ArrayList<>(Arrays.asList(location));
            duplicates.put(versions, locations);
        }
    }

    public List<String> getDifferentVersions() {
        return duplicates.keySet().stream().toList();
    }

    /**
     * Create string of affected location used in detailed report
     * @param version version string used as key in map
     * @return list of location
     */
    public String locationForVersion(String version) {
        return duplicates.get(version).stream()
                .map(String::valueOf)
                .collect(Collectors.joining("<br>"));
    }
}
