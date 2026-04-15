package io.quarkus.qe;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;


public class DiffReportTest {

    private static final Logger LOG = Logger.getLogger(DiffReportTest.class.getName());
    public static AllowedArtifacts allowedArtifactsFile;

    @BeforeAll
    public static void loadAllowedArtifact() {
        allowedArtifactsFile = PrepareOperation.loadAllowedArtifactFile();
        if (allowedArtifactsFile == null) {
            LOG.info("No allowed_artifacts file found");
        }
    }

    @Test
    public void checkForDifferentArtifactsVersions() throws IOException {
        // Hint to reuse data from previous execution and just focus on versions comparison
//        Path quarkusRepoDirectory = java.nio.file.Paths.get("/var/folders/mc/cbr401q11wx9xn8fxl5ttx6w0000gn/T/artifact-comparison-EMvNe/quarkus");
//        PrepareOperation.upstreamVersion = "3.27.3";
//        PrepareOperation.rhbqVersion = "3.27.3.redhat-00003";

        Path quarkusRepoDirectory = PrepareOperation.prepareVersionPluginOutput();

        GenerateVersionDiffReport report = new GenerateVersionDiffReport(quarkusRepoDirectory, allowedArtifactsFile);
        report.generateReport();
        // Just check to throw exception for jenkins to mark builds differently
        Assertions.assertFalse(report.isMajorMinorVersionDiffer(),
                "Version differs in major or minor version marking as failure");
        Assertions.assertFalse(report.isPatchOrOthersVersionDiffer(),
                "Version differs in patch version or others marking as unstable");
    }

    @Test
    public void checkForDifferentArtifactsInPlatformBoms() throws IOException, XmlPullParserException {
        GeneratePomComparison pomComparison = new GeneratePomComparison(allowedArtifactsFile);
        pomComparison.generatePomComparisonReport();
        Assertions.assertTrue(pomComparison.isAllArtifactAreAllowed(),
                "There are new missing/additional artifacts differ in platform bom. Check the output for more info. " +
                        "Marking as failure");
    }
}
