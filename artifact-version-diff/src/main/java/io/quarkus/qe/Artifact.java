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

    // List of know artifact which differ between upstream and RHBQ
    public static List<String> alwaysDifferentArtifact = Arrays.asList("opentelemetry-instrumentation-annotations-support", "opentelemetry-instrumentation-api-semconv");

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
    public static boolean versionComparator(String artifact, String versionsTogether) {
        // separate upstream version from downstream
        String[] versions = versionsTogether.replaceAll("\\s+", "").split("->");

        // prod creating artifact in format major.minor.patch.redhat-NNNNN or major.minor.patch.suffix-redhat-NNNNN
        // Change from `-` to `.` is happening only when the number is before `-`
        String version = versions[0].replaceFirst("(?<=\\d)-", ".");

        // check if version is know to differ and strip it of know suffixes
        if (isAlwaysDifferent(artifact)) {
            version =stripAlphaSuffix(version);
        }

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

    /**
     * Check if artifact is known to differ between upstream and RHBQ
     *
     * @param artifact artifact to be checked
     * @return
     */
    public static boolean isAlwaysDifferent(String artifact) {
        return alwaysDifferentArtifact.stream().anyMatch(artifact::contains);
    }

    /**
     * Strip version of alpha suffix in know differences between upstream and downstream
     *
     * @param version version to be striped
     * @return striped version of alpha suffix
     */
    public static String stripAlphaSuffix(String version) {
        return version.replace(".alpha", "");
    }
}
