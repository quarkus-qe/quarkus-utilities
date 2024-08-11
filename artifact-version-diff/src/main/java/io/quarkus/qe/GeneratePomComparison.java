package io.quarkus.qe;

import org.apache.maven.model.Dependency;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

public class GeneratePomComparison {

    private final PomComparator pomComparator;
    private final AllowedArtifacts allowedArtifactsFile;
    private boolean allArtifactAreAllowed = true;

    public static final String HTML_BASE_START = """
            <!DOCTYPE html>
            <html>
            <style>
                table, td, th {
                    border: 1px solid;
                    vertical-align: top;
                    text-align: left;
                    padding: 5px;
                }

                table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .not-allowed {
                    background-color: #EE9999;
                }
            </style>
            <body>
            <div>""";

    public static final String HTML_BASE_END = """
            </div>
            </body>
            </html>""";
    public String template = """
                <tr>
                    <td class="not-allowed">substitute-artifact</td>
                </tr>
                """;
    private static final Logger LOG = Logger.getLogger(GeneratePomComparison.class.getName());

    public GeneratePomComparison(AllowedArtifacts allowedArtifactsFile) throws IOException, XmlPullParserException {
        pomComparator = new PomComparator();
        pomComparator.comparePoms();
        this.allowedArtifactsFile = allowedArtifactsFile;
    }


    /**
     * Generate report of platform bom comparison of artifacts
     * The output is saved to platformBomComparison.html
     * There is need to check if `allowedArtifactsFile` is not null otherwise the exception is thrown.
     */
    public void generatePomComparisonReport() {
        LOG.info("Generating platform bom comparison report");
        try(FileWriter fw = new FileWriter("platformBomComparison.html")) {
            fw.write(HTML_BASE_START);

            fw.write("<h1>RHBQ BOM - missing artifacts</h1>\n<table>");
            fw.write(generateDifferArtifacts(pomComparator.getMissingDependencies(),
                    allowedArtifactsFile != null ? allowedArtifactsFile.getBomComparisonsMissingArtifacts() : null));
            fw.write("</table>");

            fw.write("<h1>RHBQ BOM - extra artifacts</h1>\n<table>");
            fw.write(generateDifferArtifacts(pomComparator.getExtraDependencies(),
                    allowedArtifactsFile != null ? allowedArtifactsFile.getBomComparisonsExtraArtifacts() : null));
            fw.write("</table>");

            fw.write(HTML_BASE_END);
        } catch (IOException e) {
            throw new RuntimeException("Unable to save output file. Log: " + e);
        }
    }

    /**
     * Transform different artifacts list to simple html table. Check for each artifact if is in allow list
     * @param differentArtifact List which will be transformed
     * @param allowedArtifacts List of allowed artifacts as string which will be mark as allowed
     * @return HTML table as string
     */
    public String generateDifferArtifacts(List<Dependency> differentArtifact, List<String> allowedArtifacts) {
        StringBuilder missingArtifactsString = new StringBuilder();

        differentArtifact.sort(new DependencyComparator());

        for (Dependency dependency : differentArtifact) {
            String artifact = dependency.getGroupId() + ":" + dependency.getArtifactId();
            String stringTemplate = template.replace("substitute-artifact", artifact);
            if (allowedArtifacts != null && allowedArtifacts.contains(artifact)) {
                stringTemplate = stringTemplate.replace("not-allowed", "");
            } else {
                allArtifactAreAllowed = false;
            }
            missingArtifactsString.append(stringTemplate);
        }
        return missingArtifactsString.toString();
    }

    public boolean isAllArtifactAreAllowed() {
        return allArtifactAreAllowed;
    }

    /**
     * Comparator class used to sort list containing Dependencies
     */
    static class DependencyComparator implements Comparator<Dependency> {
        @Override
        public int compare(Dependency dep1, Dependency dep2) {
            int groupIdComparison = dep1.getGroupId().compareTo(dep2.getGroupId());
            if (groupIdComparison != 0) {
                return groupIdComparison;
            } else {
                return dep1.getArtifactId().compareTo(dep2.getArtifactId());
            }
        }
    }
}
