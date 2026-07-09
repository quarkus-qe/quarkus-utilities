package io.quarkus.qe.utils;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Predicate;

public enum Repository {
    PLATFORM("platform", "https://github.com/quarkusio/quarkus-platform.git", (path) -> true),
    CORE("quarkus", "https://github.com/quarkusio/quarkus.git", new CoreFilter("quarkus")),
    MCP("mcp", "https://github.com/quarkiverse/quarkus-mcp-server.git", new MCPFilter("mcp")),
    LANGCHAIN4J("langchain", "https://github.com/quarkiverse/quarkus-langchain4j.git", new LangChainFilter("langchain"));

    private final String name;
    private final String url;
    private final Predicate<Path> foldersToMonitor;

    Repository(String name, String url, Predicate<Path> foldersToMonitor) {
        this.name = name;
        this.url = url;
        this.foldersToMonitor = foldersToMonitor;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public Predicate<Path> folders() {
        return foldersToMonitor;
    }
}

class CoreFilter implements Predicate<Path> {
    private final String prefix;

    CoreFilter(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean test(Path path) {
        String absolute = path.toAbsolutePath().toString();
        return absolute.contains(prefix + File.separator + "bom") ||
                absolute.contains(prefix + File.separator + "core") ||
                absolute.contains(prefix + File.separator + "extensions") ||
                absolute.contains(prefix + File.separator + "test-framework");
    }
}

class MCPFilter implements Predicate<Path> {
    private final String prefix;

    MCPFilter(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean test(Path path) {
        String absolute = path.toAbsolutePath().toString();
        return absolute.contains(prefix + File.separator + "core") ||
                absolute.contains(prefix + File.separator + "cli-adapter") ||
                absolute.contains(prefix + File.separator + "devtools") ||
                absolute.contains(prefix + File.separator + "hibernate-validator") ||
                absolute.contains(prefix + File.separator + "oidc") ||
                absolute.contains(prefix + File.separator + "schema-validator") ||
                absolute.contains(prefix + File.separator + "sse-client") ||
                absolute.contains(prefix + File.separator + "transports");
    }
}

class LangChainFilter implements Predicate<Path> {
    private final String prefix;

    LangChainFilter(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean test(Path path) {
        String absolute = path.toAbsolutePath().toString();
        return absolute.contains(prefix + File.separator + "core") ||
                absolute.contains(prefix + File.separator + "mcp") ||
                absolute.contains(prefix + File.separator + "mcp-auth-providers") ||
                absolute.contains(prefix + File.separator + "memory-stores") ||
                absolute.contains(prefix + File.separator + "model-auth-providers") ||
                absolute.contains(prefix + File.separator + "model-providers" + File.separator + "openai") ||
                absolute.contains(prefix + File.separator + "quarkus-integrations") ||
                absolute.contains(prefix + File.separator + "testing") ||
                absolute.contains(prefix + File.separator + "tools");
    }
}

