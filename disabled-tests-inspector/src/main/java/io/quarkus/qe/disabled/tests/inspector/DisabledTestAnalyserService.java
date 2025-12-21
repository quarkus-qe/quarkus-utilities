package io.quarkus.qe.disabled.tests.inspector;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHTree;
import org.kohsuke.github.GHTreeEntry;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class DisabledTestAnalyserService {

    private static final Logger LOG = Logger.getLogger(DisabledTestAnalyserService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Pattern CLASS_DECLARATION_PATTERN = Pattern.compile(
            "(?:public|private|protected)?\\s*(?:\\w+\\s+)*class\\s+(\\w+)"
    );

    private static final Pattern TEST_METHOD_PATTERN = Pattern.compile(
            "(?:public|protected)?\\s*void\\s+(\\w+)\\s*\\("
    );

    private static final Pattern DISABLED_ANNOTATION_PATTERN = Pattern.compile(
            "@((?:Disabled|Enabled)\\w*)(?:\\s*\\((.*))?"
    );

    private static final Pattern REASON_PATTERN = Pattern.compile(
            "(?:reason|disabledReason)\\s*=\\s*\"([^\"]+)\"(?:\\s*\\+\\s*)?"
    );

    private static final Pattern EXPLICIT_REASON_PATTERN = Pattern.compile(
            "^\\s*\"([^\"]+)\"(?:\\s*\\+\\s*)?\\s*$"
    );

    private static final Pattern NEXT_LINE_STRING_PATTERN = Pattern.compile(
            "^\\s*\"([^\"]+)\""
    );

    private static final Pattern ISSUE_URL_PATTERN = Pattern.compile(
            "(https://(?:github\\.com/[^/]+/[^/]+/issues/\\d+|issues\\.redhat\\.com/browse/[A-Z]+-\\d+))"
    );

    private static final Pattern ISSUE_ID_PATTERN = Pattern.compile(
            "([A-Z]+-\\d+|QUARKUS-\\d+)"
    );

    private static final String RED_HAT_ISSUE_TRACKER_URL = "https://issues.redhat.com/browse/";

    private static final Set<String> LITE_MODE_ALWAYS_SKIP = Set.of(
            "DisabledForJreRange",
            "DisabledIfSystemProperty",
            "DisabledOnQuarkusSnapshot",
            "EnabledOnNative",
            "EnabledIfSystemProperty",
            "EnabledWhenLinuxContainersAvailable",
            "EnabledIfPostgresImageCommunity",
            "EnabledIfPostgresImageProduct",
            "EnabledOnQuarkusVersion",
            "EnabledOnQuarkusVersions"
    );

    // Annotations skipped in lite mode only if they do not have an issue link
    private static final Set<String> LITE_MODE_CONDITIONAL_SKIP = Set.of(
            "DisabledOnNative",
            "DisabledOnSemeruJdk",
            "DisabledOnOs"
    );

    public void analyzeRepository(String repoOwner, String repoName, List<String> branches,
                                  String baseOutputFileName, boolean liteMode) throws IOException {
        if (liteMode) {
            LOG.info("Lite report mode ENABLED");
        }

        GitHub github = GitHub.connect();
        GHRepository repo = github.getRepository(repoOwner + "/" + repoName);

        for (String branch : branches) {
            LOG.info("Fetching tree for branch: " + branch);
            GHTree tree = repo.getTreeRecursive(branch, 1);
            List<GHTreeEntry> entries = tree.getTree();

            List<DisabledTest> disabledTests = new ArrayList<>();
            Map<String, DisabledTestsModuleStats> moduleStats = new HashMap<>();

            int processedFileCount = 0;
            int totalFiles = entries.size();
            LOG.info("Starting analysis of " + totalFiles + " files");

            for (GHTreeEntry entry : entries) {
                String filePath = entry.getPath();

                processedFileCount++;
                if (processedFileCount % 100 == 0) {
                    LOG.info(String.format("Analyzed %d / %d files (branch: '%s')", processedFileCount, totalFiles, branch));
                }

                if ((filePath.contains("/test/") || filePath.contains("testsuite/")) && filePath.endsWith(".java")) {
                    GHContent testClassContent = repo.getFileContent(filePath, branch);
                    TestClassData data = new TestClassData(
                            testClassContent.getHtmlUrl(),
                            filePath,
                            readFileContent(testClassContent.read())
                    );
                    disabledTests.addAll(extractDisabledTests(data, moduleStats, liteMode));
                }
            }

            BranchAnalysisResult result = new BranchAnalysisResult(branch, disabledTests);
            String testListFile = getTestFileName(baseOutputFileName, branch);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(testListFile), result);

            String statsListFile = getStatsFileName(baseOutputFileName, branch);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(new File(statsListFile), moduleStats);

            LOG.info("Finished analysis for branch: " + branch);
        }
    }

    List<DisabledTest> extractDisabledTests(TestClassData testClassData,
                                            Map<String, DisabledTestsModuleStats> moduleStats,
                                            boolean liteMode) {

        List<DisabledTest> disabledTests = new ArrayList<>();
        String[] lines = testClassData.content().split("\n");

        String currentClass = null;
        String currentTestMethod = null;

        List<String> annotationTypes = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        List<String> issueLinks = new ArrayList<>();

        String lastComment = null;
        boolean insideBlockComment = false;

        for (int i = 0; i < lines.length; i++) {
            String trimmedLine = lines[i].trim();
            if (trimmedLine.isEmpty()) continue;

            // handle block comments
            if (insideBlockComment) {
                if (trimmedLine.contains("*/")) {
                    insideBlockComment = false;
                }
                continue;
            }
            if (trimmedLine.startsWith("/*")) {
                if (!trimmedLine.contains("*/")) {
                    insideBlockComment = true;
                }
                continue;
            }

            String inlineComment = null;
            String lineWithoutComment = trimmedLine;

            int commentIndex = trimmedLine.indexOf("//");
            // If // is preceded by :, it's likely a URL (https://), so it is skipped and looks further
            while (commentIndex > 0 && trimmedLine.charAt(commentIndex - 1) == ':') {
                commentIndex = trimmedLine.indexOf("//", commentIndex + 2);
            }

            if (commentIndex != -1) {
                inlineComment = trimmedLine.substring(commentIndex + 2).trim();
                lineWithoutComment = trimmedLine.substring(0, commentIndex).trim();
            }

            if (lineWithoutComment.isEmpty()) {
                lastComment = inlineComment;
                continue;
            }

            // class detection
            Matcher classMatcher = CLASS_DECLARATION_PATTERN.matcher(lineWithoutComment);
            if (classMatcher.find()) {
                currentClass = classMatcher.group(1);
                flushAnnotations(disabledTests, moduleStats, annotationTypes, reasons, issueLinks,
                        currentClass, "All tests in class", testClassData, liteMode);
                lastComment = null;
                continue;
            }

            // method detection
            Matcher methodMatcher = TEST_METHOD_PATTERN.matcher(lineWithoutComment);
            if (methodMatcher.find()) {
                currentTestMethod = methodMatcher.group(1);
                if (!currentTestMethod.equals(currentClass)) {
                    flushAnnotations(disabledTests, moduleStats, annotationTypes, reasons, issueLinks,
                            currentClass, currentTestMethod, testClassData, liteMode);
                }
                lastComment = null;
                continue;
            }

            // annotation detection
            Matcher disabledMatcher = DISABLED_ANNOTATION_PATTERN.matcher(lineWithoutComment);

            if (disabledMatcher.find()) {
                String annotationType = disabledMatcher.group(1);
                String annotationContent = disabledMatcher.group(2);

                String reason = null;
                boolean isMultiline = false;

                if (annotationContent != null) {
                    // remove the closing parenthesis of the annotation if present
                    annotationContent = annotationContent.trim();
                    if (annotationContent.endsWith(")")) {
                        annotationContent = annotationContent.substring(0, annotationContent.length() - 1).trim();
                    }

                    Matcher propertyMatcher = REASON_PATTERN.matcher(annotationContent);
                    if (propertyMatcher.find()) {
                        reason = propertyMatcher.group(1);
                        if ((propertyMatcher.groupCount() > 1 && propertyMatcher.group(2) != null)
                                || annotationContent.trim().endsWith("+")) {
                            isMultiline = true;
                        }
                    } else {
                        Matcher valueMatcher = EXPLICIT_REASON_PATTERN.matcher(annotationContent);
                        if (valueMatcher.find()) {
                            reason = valueMatcher.group(1);
                            if (annotationContent.trim().endsWith("+")) {
                                isMultiline = true;
                            }
                        }
                    }
                }

                // multiline reason builder
                if (isMultiline && (i + 1 < lines.length)) {
                    String nextLine = lines[i+1].trim();
                    Matcher multilineMatcher = NEXT_LINE_STRING_PATTERN.matcher(nextLine);
                    if (multilineMatcher.find()) {
                        reason += multilineMatcher.group(1);
                    }
                }

                if (reason == null && inlineComment != null) {
                    reason = inlineComment;
                }

                if (reason == null && lastComment != null) {
                    reason = lastComment;
                }

                String issueLink = extractIssueLink(reason);
                if (issueLink == null && inlineComment != null) {
                    issueLink = extractIssueLink(inlineComment);
                }
                if (issueLink == null && lastComment != null) {
                    issueLink = extractIssueLink(lastComment);
                }

                if (issueLink == null) {
                    String text = (reason != null) ? reason : ((inlineComment != null) ? inlineComment : lastComment);
                    issueLink = tryBuildIssueLink(text);
                }

                annotationTypes.add(annotationType);
                reasons.add(reason);
                issueLinks.add(issueLink);

                lastComment = null;
            } else {
                lastComment = null;
            }
        }
        return disabledTests;
    }

    private void flushAnnotations(List<DisabledTest> tests, Map<String, DisabledTestsModuleStats> moduleStats,
                                  List<String> types, List<String> reasons, List<String> issueLinks,
                                  String className, String testName, TestClassData data, boolean liteMode) {

        if (types.isEmpty()) return;

        for (int i = 0; i < types.size(); i++) {
            String type = types.get(i);
            String reason = reasons.get(i);
            String issueLink = issueLinks.get(i);

            // Lite mode filtering
            if (liteMode) {
                if (LITE_MODE_ALWAYS_SKIP.contains(type)) {
                    continue;
                }
                if (LITE_MODE_CONDITIONAL_SKIP.contains(type)) {
                    if (issueLink == null || issueLink.isEmpty()) {
                        continue;
                    }
                }
            }

            boolean isClosed = isGitHubIssueClosed(issueLink);

            tests.add(new DisabledTest(testName, className, type, reason, issueLink, data.fileUrl(), isClosed));

            String moduleName = extractModuleName(data.filePath());
            moduleStats.putIfAbsent(moduleName, new DisabledTestsModuleStats());
            moduleStats.get(moduleName).incrementAnnotation(type);
        }

        types.clear();
        reasons.clear();
        issueLinks.clear();
    }

    private String extractIssueLink(String text) {
        if (text == null) return null;
        Matcher issueMatcher = ISSUE_URL_PATTERN.matcher(text);
        return issueMatcher.find() ? issueMatcher.group(1) : null;
    }

    private String tryBuildIssueLink(String text) {
        if (text == null) return null;
        Matcher idMatcher = ISSUE_ID_PATTERN.matcher(text);
        if (idMatcher.find()) {
            return RED_HAT_ISSUE_TRACKER_URL + idMatcher.group(1);
        }
        return null;
    }

    private boolean isGitHubIssueClosed(String issueLink) {
        if (issueLink != null && issueLink.contains("github.com")) {
            try {
                String[] parts = issueLink.split("/");
                // Format check: .../owner/repo/issues/number
                if (parts.length < 4) return false;

                String repo = parts[parts.length - 4] + "/" + parts[parts.length - 3];
                String issueNumStr = parts[parts.length - 1];

                // Remove anchor (#issuecomment-1234) if present
                if (issueNumStr.contains("#")) {
                    issueNumStr = issueNumStr.substring(0, issueNumStr.indexOf("#"));
                }

                int issueNumber = Integer.parseInt(issueNumStr);

                GitHub github = GitHub.connect();
                GHIssue issue = github.getRepository(repo).getIssue(issueNumber);
                return issue.getState() == GHIssueState.CLOSED;
            } catch (Exception e) {
                LOG.error("Failed to check issue state for " + issueLink, e);
                return false;
            }
        }
        return false;
    }

    private String readFileContent(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private String extractModuleName(String filePath) {
        return filePath.substring(0, filePath.indexOf("/src"));
    }

    private String getTestFileName(String fileName, String branchName) {
        return getFileName(fileName, branchName, "");
    }

    private String getStatsFileName(String fileName, String branchName) {
        return getFileName(fileName, branchName, "-stats");
    }

    private String getFileName(String fileName, String branchName, String fileNameSuffix) {
        return fileName.replaceFirst("(\\.json)?$", "-" + branchName + fileNameSuffix + ".json");
    }
}
