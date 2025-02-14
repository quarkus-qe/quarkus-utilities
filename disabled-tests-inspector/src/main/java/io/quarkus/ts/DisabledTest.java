package io.quarkus.ts;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DisabledTest {

    @JsonProperty("test_name")
    private String testName;

    @JsonProperty("class_name")
    private String className;

    @JsonProperty("annotation_type")
    private String annotationType;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("issue_link")
    private String issueLink;

    @JsonProperty("file_url")
    private String fileURL;

    @JsonProperty("issue_closed")
    private boolean issueClosed;

    public DisabledTest(String testName, String className, String annotationType, String reason, String issueLink, String fileURL, boolean issueClosed) {
        this.testName = testName;
        this.className = className;
        this.annotationType = annotationType;
        this.reason = reason;
        this.issueLink = issueLink;
        this.fileURL = fileURL;
        this.issueClosed = issueClosed;
    }
}
