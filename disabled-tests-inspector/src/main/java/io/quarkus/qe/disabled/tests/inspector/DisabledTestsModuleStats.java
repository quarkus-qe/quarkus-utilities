package io.quarkus.qe.disabled.tests.inspector;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

import java.util.HashMap;
import java.util.Map;

public class DisabledTestsModuleStats {

    @JsonAnyGetter
    private final Map<String, Integer> annotationCounts = new HashMap<>();

    public void incrementAnnotation(String annotation) {
        annotationCounts.put(annotation, annotationCounts.getOrDefault(annotation, 0) + 1);
    }
}
