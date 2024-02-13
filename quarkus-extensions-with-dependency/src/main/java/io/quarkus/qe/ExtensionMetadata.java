package io.quarkus.qe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

// Metadata class for nested JSON object
// Only fields mentioned in this class are deserialized, others are ignored
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExtensionMetadata {

    private List<String> keywords;
    private List<String> extensionDependencies;
    private List<String> status;
    private String builtWithQuarkusCore;
    private String scmUrl;
    private String guide;
    private List<String> categories;
    private String minimumJavaVersion;


    @JsonProperty("keywords")
    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    @JsonProperty("extension-dependencies")
    public List<String> getExtensionDependencies() {
        return extensionDependencies;
    }

    public void setExtensionDependencies(List<String> extensionDependencies) {
        this.extensionDependencies = extensionDependencies;
    }

    @JsonProperty("status")
    public List<String> getStatus() {
        return status;
    }

    public void setStatus(JsonNode statusNode) {
        status = new ArrayList<>();

        if (statusNode.isArray()) {
            for (JsonNode element : statusNode) {
                status.add(element.asText());
            }
        } else if (statusNode.isTextual()) {
            status.add(statusNode.asText());
        }
    }

    @JsonProperty("built-with-quarkus-core")
    public String getBuiltWithQuarkusCore() {
        return builtWithQuarkusCore;
    }

    public void setBuiltWithQuarkusCore(String builtWithQuarkusCore) {
        this.builtWithQuarkusCore = builtWithQuarkusCore;
    }

    @JsonProperty("scm-url")
    public String getScmUrl() {
        return scmUrl;
    }

    public void setScmUrl(String scmUrl) {
        this.scmUrl = scmUrl;
    }

    @JsonProperty("guide")
    public String getGuide() {
        return guide;
    }

    public void setGuide(String guide) {
        this.guide = guide;
    }

    @JsonProperty("categories")
    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    @JsonProperty("minimum-java-version")
    public String getMinimumJavaVersion() {
        return minimumJavaVersion;
    }

    public void setMinimumJavaVersion(String minimumJavaVersion) {
        this.minimumJavaVersion = minimumJavaVersion;
    }
}
