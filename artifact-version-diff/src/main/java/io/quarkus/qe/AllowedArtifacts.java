package io.quarkus.qe;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Datatype class for jackson serialization of yaml file
 */
public class AllowedArtifacts {

    @JsonProperty("allowed-artifacts-version-comparison")
    private List<AllowedArtifact> versionComparisonsArtifacts;
    @JsonProperty("allowed-missing-artifacts-bom-comparison")
    private List<String> bomComparisonsMissingArtifacts;
    @JsonProperty("allowed-extra-artifacts-bom-comparison")
    private List<String> bomComparisonsExtraArtifacts;

    public AllowedArtifacts() {
    }

    public List<AllowedArtifact> getVersionComparisonsArtifacts() {
        return versionComparisonsArtifacts;
    }

    public void setVersionComparisonsArtifacts(
            List<AllowedArtifact> versionComparisonsArtifacts) {
        this.versionComparisonsArtifacts = versionComparisonsArtifacts;
    }

    public List<String> getBomComparisonsMissingArtifacts() {
        return bomComparisonsMissingArtifacts;
    }

    public void setBomComparisonsMissingArtifacts(List<String> bomComparisonsMissingArtifacts) {
        this.bomComparisonsMissingArtifacts = bomComparisonsMissingArtifacts;
    }

    public List<String> getBomComparisonsExtraArtifacts() {
        return bomComparisonsExtraArtifacts;
    }

    public void setBomComparisonsExtraArtifacts(List<String> bomComparisonsExtraArtifacts) {
        this.bomComparisonsExtraArtifacts = bomComparisonsExtraArtifacts;
    }

    public static class AllowedArtifact {

        @JsonProperty("artifact")
        private String artifact;
        @JsonProperty("allowed-rhbq-versions")
        private List<String> rhbqVersions = new ArrayList<>();

        public AllowedArtifact() {
        }

        public String getArtifact() {
            return artifact;
        }

        public void setArtifact(String artifact) {
            this.artifact = artifact;
        }

        public List<String> getRhbqVersions() {
            return rhbqVersions;
        }

        public void setRhbqVersions(List<String> rhbqVersions) {
            this.rhbqVersions = rhbqVersions;
        }
    }
}
