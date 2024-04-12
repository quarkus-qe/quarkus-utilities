package io.quarkus.qe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    /**
     * Comparing version if they are the same.
     * Also modify them as they are differences between upstream and productized artifact
     * @param versionsTogether versions to be compared in format <upstream_version> -> <downstream_version>
     * @return false if the version are not the same
     */
    public static boolean versionComparator(String versionsTogether) {
        // separate upstream version from downstream
        String[] versions = versionsTogether.replaceAll("\\s+", "").split("->");

        // prod creating artifact in format major.minor.patch.redhat-NNNNN or major.minor.patch.suffix-redhat-NNNNN
        String version = versions[0].replaceFirst("-", ".");

        // match any artifact which have only major and minor version
        Pattern pattern = Pattern.compile("^(\\d+\\.\\d+$)");
        Matcher matcher = pattern.matcher(version);
        // match any artifact which have only major and minor version and some suffix
        Pattern pattern2 = Pattern.compile("^(\\d+\\.\\d+((\\.)\\D))");
        Matcher matcher2 = pattern2.matcher(version);
        // transform upstream version from major.minor to major.minor.0
        if (matcher.find()) {
            version += ".0";
        }
        // transform upstream version from major.minor-<suffix> to major.minor.0-suffix
        // eg 1.77.alpha to 1.77.0.alpha
        if (matcher2.find()) {
            String versionHelper = matcher2.group();
            version = version.substring(0, versionHelper.length() -2) + ".0" + version.substring(versionHelper.length() -2);
        }
        return versions[1].contains(version);
    }
}
