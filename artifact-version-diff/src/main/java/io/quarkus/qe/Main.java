package io.quarkus.qe;

import io.quarkus.qe.utils.Configuration;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.nio.file.Path;

public class Main {

    public static void main(String[] args) throws IOException, XmlPullParserException {
        Configuration config = new Configuration();
        Path quarkusRepoDirectory = PrepareOperation.prepareVersionPluginOutput(config);
        AllowedArtifacts allowedArtifactsFile = PrepareOperation.loadAllowedArtifactFile();
        GenerateVersionDiffReport report = new GenerateVersionDiffReport(quarkusRepoDirectory, allowedArtifactsFile, config);
        GeneratePomComparison pomComparison = new GeneratePomComparison(allowedArtifactsFile, config);
        report.generateReport();
        pomComparison.generatePomComparisonReport(config);
    }
}
