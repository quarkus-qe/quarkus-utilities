package io.quarkus.qe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Extension {

    private String name;
    private String description;
    private ExtensionMetadata metadata;
    private String artifact;
    private List<String> origins;


    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("metadata")
    public ExtensionMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ExtensionMetadata metadata) {
        this.metadata = metadata;
    }

    @JsonProperty("artifact")
    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    @JsonProperty("origins")
    public List<String> getOrigins() {
        return origins;
    }

    public void setOrigins(List<String> origins) {
        this.origins = origins;
    }
}