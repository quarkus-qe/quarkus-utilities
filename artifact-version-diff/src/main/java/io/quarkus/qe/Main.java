package io.quarkus.qe;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws IOException, XmlPullParserException {
        Path quarkusRepoDirectory = PrepareOperation.prepareVersionPluginOutput();
        AllowedArtifacts allowedArtifactsFile = PrepareOperation.loadAllowedArtifactFile();
        GenerateVersionDiffReport report = new GenerateVersionDiffReport(quarkusRepoDirectory, allowedArtifactsFile);
        GeneratePomComparison pomComparison = new GeneratePomComparison(allowedArtifactsFile);
        report.generateReport();
        pomComparison.generatePomComparisonReport();
    }
}
