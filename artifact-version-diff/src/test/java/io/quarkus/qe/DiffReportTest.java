package io.quarkus.qe;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;


public class DiffReportTest {

    @Test
    public void checkNoArtifactHave() throws IOException {
        Path quarkusRepoDirectory = PrepareOperation.prepareVersionPluginOutput();
        GenerateReport report = new GenerateReport(quarkusRepoDirectory);
        report.generateReport();
        // Just check to throw exception for jenkins to mark builds differently
        Assertions.assertFalse(report.isMajorMinorVersionDiffer(),
                "Version differs in major or minor version marking as failure");
        Assertions.assertFalse(report.isPatchOrOthersVersionDiffer(),
                "Version differs in patch version or others marking as unstable");
    }
}
