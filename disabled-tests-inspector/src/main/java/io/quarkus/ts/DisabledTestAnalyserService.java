package io.quarkus.ts;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class DisabledTestAnalyserService {

    private static final Pattern CLASS_DECLARATION_PATTERN = Pattern.compile(
            "public\\s+(?:\\w+\\s+)*class\\s+(\\w+)"
    );

    private static final Pattern TEST_METHOD_PATTERN = Pattern.compile(
            "public\\s+void\\s+(\\w+)\\s*\\("
    );

    private static final Pattern DISABLED_ANNOTATION_PATTERN = Pattern.compile(
            "@(Disabled\\w*)\\s*(?:\\((.*)\\))?"
    );

    private static final Pattern REASON_PATTERN = Pattern.compile(
            "reason\\s*=\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern EXPLICIT_REASON_PATTERN = Pattern.compile(
            "\"(.*)\""
    );

    private static final Pattern ISSUE_PATTERN = Pattern.compile(
    "(https://(?:github\\.com/.+?/issues/\\d+|issues\\.redhat\\.com/browse/\\w+-\\d+))"
    );


    public void analyzeRepository(String repoOwner, String repoName, List<String> branches, String baseOutputFileName) throws IOException {
        GitHub github = GitHub.connect();
        GHRepository repo = github.getRepository(repoOwner + "/" + repoName);

        for (String branch : branches) {
            GHTree tree = repo.getTreeRecursive(branch, 1);
            List<DisabledTest> disabledTests = new ArrayList<>();
            Map<String, DisabledTestsModuleStats> moduleStats = new HashMap<>();

            for (GHTreeEntry entry : tree.getTree()) {
                String filePath = entry.getPath();
                if (entry.getPath().contains("/test/") && filePath.endsWith(".java")) {
                    GHContent fileContent = repo.getFileContent(filePath, branch);
                    disabledTests.addAll(extractDisabledTests(fileContent, moduleStats));
                }
            }

            BranchAnalysisResult result = new BranchAnalysisResult(branch, disabledTests);
            String testListFile = getStatsOrTestFileName(baseOutputFileName, branch, false);
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File(testListFile), result);

            String statsListFile = getStatsOrTestFileName(baseOutputFileName, branch, true);
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File(statsListFile), moduleStats);
        }
    }

    private List<DisabledTest> extractDisabledTests(GHContent ghContent,
                                                    Map<String, DisabledTestsModuleStats> moduleStats) throws IOException {

        String fileUrl = ghContent.getHtmlUrl();
        String fileContent = ghContent.getContent();

        List<DisabledTest> disabledTests = new ArrayList<>();
        String[] lines = fileContent.split("\n");

        String previousLine = "";
        String currentClass = "UnknownClass";
        String currentTestMethod = null;
        List<String> annotationTypes = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        List<String> issueLinks = new ArrayList<>();
        boolean insideDisabledBlock = false;

        for (String currentLine : lines) {
            currentLine = currentLine.trim();

            Matcher classMatcher = CLASS_DECLARATION_PATTERN.matcher(currentLine);
            if (classMatcher.find()) {
                currentClass = classMatcher.group(1);
            }

            Matcher testMethodMatcher = TEST_METHOD_PATTERN.matcher(currentLine);
            if (testMethodMatcher.find()) {
                currentTestMethod = testMethodMatcher.group(1);
            }

            Matcher disabledMatcher = DISABLED_ANNOTATION_PATTERN.matcher(currentLine);
            if (disabledMatcher.find()) {
                String reason = null;
                String issueLink = null;
                insideDisabledBlock = true;
                String annotationType = disabledMatcher.group(1);

                String annotationContent = disabledMatcher.group(2);
                if (annotationContent != null) {
                    Matcher reasonMatcher = REASON_PATTERN.matcher(annotationContent);

                    if (reasonMatcher.find()) {
                        reason = reasonMatcher.group(1);
                    }

                    if (reason == null) {
                        Matcher explicitReasonmatcher = EXPLICIT_REASON_PATTERN.matcher(annotationContent);
                        if (explicitReasonmatcher.find()) {
                            reason = explicitReasonmatcher.group(1);
                        }
                    }
                    issueLink = extractIssueLink(annotationContent);
                }

                if (issueLink == null && previousLine.startsWith("//")) {
                    issueLink = extractIssueLink(previousLine);
                }

                if (issueLink == null && currentLine.startsWith("//")) {
                    issueLink = extractIssueLink(currentLine);
                }

                if (reason == null) {
                    reason = getDisablingReasonComment(previousLine);
                }

                if (reason == null) {
                    reason = getDisablingReasonComment(currentLine);
                }

                annotationTypes.add(annotationType);
                reasons.add(reason);
                issueLinks.add(issueLink);
            }

            if (insideDisabledBlock && (testMethodMatcher.lookingAt() || (classMatcher.lookingAt()))) {
                for (int i = 0; i < annotationTypes.size(); i++) {
                    disabledTests.add(new DisabledTest(
                            currentTestMethod != null ? currentTestMethod : "All methods",
                            currentClass,
                            annotationTypes.get(i),
                            reasons.get(i),
                            issueLinks.get(i),
                            fileUrl,
                            isGitHubIssueClosed(issueLinks.get(i))
                    ));

                    String moduleName = extractModuleName(ghContent.getPath());
                    moduleStats.putIfAbsent(moduleName, new DisabledTestsModuleStats());
                    moduleStats.get(moduleName).incrementAnnotation(annotationTypes.get(i));
                }

                annotationTypes.clear();
                reasons.clear();
                issueLinks.clear();
                insideDisabledBlock = false;
            }
            previousLine = currentLine;
        }
        return disabledTests;
    }

    private String extractIssueLink(String text) {
        Matcher issueMatcher = ISSUE_PATTERN.matcher(text);
        return issueMatcher.find() ? issueMatcher.group(1) : null;
    }

    private String getDisablingReasonComment(String line) {
        if (line.contains("//")) {
            return line.substring(line.indexOf("//") + 2).trim();
        }
        return null;
    }

    private boolean isGitHubIssueClosed(String issueLink) {
        if (issueLink != null && issueLink.contains("github.com")) {
            try {
                String[] parts = issueLink.split("/");
                String repo = parts[parts.length - 4] + "/" + parts[parts.length - 3];
                int issueNumber = Integer.parseInt(parts[parts.length - 1]);

                GitHub github = GitHub.connect();
                GHIssue issue = github.getRepository(repo).getIssue(issueNumber);
                return issue.getState() == GHIssueState.CLOSED;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private String extractModuleName(String filePath) {
        return filePath.substring(0, filePath.indexOf("/src"));
    }

    private String getStatsOrTestFileName(String fileName, String branchName, boolean isStats) {
        String statsSuffix = isStats ? "-stats" : "";
        return fileName.replaceFirst("(\\.json)?$", "-" + branchName + statsSuffix + ".json");
    }
}
