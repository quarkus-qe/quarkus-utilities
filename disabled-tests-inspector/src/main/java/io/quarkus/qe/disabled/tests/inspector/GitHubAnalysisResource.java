package io.quarkus.qe.disabled.tests.inspector;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.Arrays;

import org.jboss.logging.Logger;

@ApplicationScoped
public class GitHubAnalysisResource {

    private static final Logger LOG = Logger.getLogger(GitHubAnalysisResource.class);

    @Inject
    DisabledTestAnalyserService analyserService;

    public void startAnalysis() {
        String repoOwner = System.getProperty("repoOwner", "org");
        String repoName = System.getProperty("repoName", "repo-name");
        String branches = System.getProperty("branches", "main");
        String baseOutputFileName = System.getProperty("baseOutputFileName", "disabled-tests-report.json");

        try {
            LOG.info("Starting analysis for " + repoOwner + "/" + repoName + " on branches: " + branches);
            analyserService.analyzeRepository(repoOwner, repoName, Arrays.asList(branches.split(",")), baseOutputFileName);
            LOG.info("Analysis finished, see reports in created JSON files.");
        } catch (IOException e) {
            LOG.error("Error creating YAML file: ", e);
        }
    }
}
