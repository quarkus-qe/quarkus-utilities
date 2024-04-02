package io.quarkus.qe;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Datatype class for jackson serialization of yaml file
 */
public class AllowedArtifacts {

    @JsonProperty("allowed-artifacts")
    private List<AllowedArtifact> allowedArtifact;

    public AllowedArtifacts() {
    }

    public List<AllowedArtifact> getAllowedArtifact() {
        return allowedArtifact;
    }

    public void setAllowedArtifact(List<AllowedArtifact> allowedArtifact) {
        this.allowedArtifact = allowedArtifact;
    }

    public static class AllowedArtifact {

        @JsonProperty("artifact")
        private String artifact;
        @JsonProperty("allowed-rhbq-versions")
        private List<String> rhbqVersions = new ArrayList<String>();

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
