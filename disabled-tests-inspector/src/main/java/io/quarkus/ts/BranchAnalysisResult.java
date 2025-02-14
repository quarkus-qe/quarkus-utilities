package io.quarkus.ts;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class BranchAnalysisResult {

    @JsonProperty("branch_name")
    private String branch;

    @JsonProperty("disabled_tests")
    private List<DisabledTest> disabledTests;

    public BranchAnalysisResult(String branch, List<DisabledTest> disabledTests) {
        this.branch = branch;
        this.disabledTests = disabledTests;
    }
}
