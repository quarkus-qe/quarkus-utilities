package io.quarkus.qe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jboss.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class DependencyFilter {

    private static final Logger LOGGER = Logger.getLogger(DependencyFilter.class);


    public static void main(String[] args) throws IOException {
        String currentWorkingDir = System.getProperty("user.dir");
        File inputFile = new File(currentWorkingDir + "/quarkus-extensions.json");


        String searchedDependency = Objects.requireNonNull(System.getProperty("searched.dependency"),
                "System property 'searched.dependency' expected");

        String outputFileName = Objects.requireNonNullElse(System.getProperty("output.file.name"),
                "Quarkus-extensions-with-" + searchedDependency + ".pdf");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(inputFile);
        JsonNode extensionsNode = jsonNode.get("extensions");

        if (extensionsNode != null && extensionsNode.isArray()) {
            Iterator<JsonNode> iterator = extensionsNode.iterator();

            List<Extension> resultExtensions = new ArrayList<>();

            while (iterator.hasNext()) {
                JsonNode extensionNode = iterator.next();
                JsonNode metadataNode = extensionNode.get("metadata");
                if (metadataNode == null) {
                    continue;
                }
                JsonNode dependenciesNode = metadataNode.get("extension-dependencies");

                if (dependenciesNode != null && dependenciesNode.isArray()) {
                    for (JsonNode dependency : dependenciesNode) {
                        if (dependency.isTextual() && dependency.asText().contains(searchedDependency)) {
                            Extension extension = objectMapper.treeToValue(extensionNode, Extension.class);
                            resultExtensions.add(extension);
                            break;
                        }
                    }
                }
            }

            if (!resultExtensions.isEmpty()) {
                PDFUtils.writeToPDF(resultExtensions, outputFileName);
            }
            else {
                LOGGER.info("No extension found with given dependency " + searchedDependency);
            }
        }
    }
}

