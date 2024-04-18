package io.quarkus.qe;

import java.io.IOException;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws IOException {
        Path quarkusRepoDirectory = PrepareOperation.prepareVersionPluginOutput();
        GenerateReport report = new GenerateReport(quarkusRepoDirectory);
        report.generateReport();
    }
}
