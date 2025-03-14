package io.quarkus.qe.disabled.tests.inspector;

import java.util.Objects;

public record TestClassData(String fileUrl, String filePath, String content) {

    public TestClassData(String fileUrl, String filePath, String content) {
        this.fileUrl = Objects.requireNonNull(fileUrl);
        this.filePath = Objects.requireNonNull(filePath);
        this.content = Objects.requireNonNull(content);
    }

}
