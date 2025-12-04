package io.quarkus.qe.test.stats.analyzer.recipe;

import org.openrewrite.ExecutionContext;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import java.util.List;
import java.util.Set;

public final class MakeCallbacksAndTestsEmpty extends Recipe {

    public MakeCallbacksAndTestsEmpty() {
        super();
    }

    @Override
    public String getDisplayName() {
        return "Prepare dry run";
    }

    @Override
    public String getDescription() {
        return "Makes all JUnit test methods and callbacks empty so that we can only detect which tests are run without executing them.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return MakeMethodBodyEmptyVisitor.INSTANCE;
    }

    private static final class MakeMethodBodyEmptyVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final Set<String> METHOD_ANNOTATIONS = Set.of("org.junit.jupiter.api.Test",
                "org.junit.jupiter.params.ParameterizedTest", "org.junit.jupiter.api.BeforeEach",
                "org.junit.jupiter.api.AfterEach", "org.junit.jupiter.api.BeforeAll",
                "org.junit.jupiter.api.AfterAll", "org.junit.jupiter.api.RepeatedTest",
                "io.quarkus.test.scenarios.TestQuarkusCli");
        private static final UsesTypeImpl[] TEST_CLASS_VISITOR = METHOD_ANNOTATIONS.stream()
                .map(UsesTypeImpl::new).toArray(UsesTypeImpl[]::new);
        private static final MakeMethodBodyEmptyVisitor INSTANCE = new MakeMethodBodyEmptyVisitor();

        @Override
        public boolean isAcceptable(SourceFile sourceFile, ExecutionContext executionContext) {
            for (UsesTypeImpl usesType : TEST_CLASS_VISITOR) {
                if (usesType.isAcceptable(sourceFile, executionContext)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
            boolean isTestableOrCallback = method.getLeadingAnnotations().stream()
                    .anyMatch(a -> METHOD_ANNOTATIONS.stream().anyMatch(fullName -> fullName.contains(a.getSimpleName())));
            J.MethodDeclaration visitedMethod = super.visitMethodDeclaration(method, executionContext);
            if (isTestableOrCallback && method.getBody() != null && !method.getBody().getStatements().isEmpty()) {
                visitedMethod = visitedMethod.withBody(J.Block.createEmptyBlock());
                var removeMethodParameters = method.getParameters().stream()
                        .map(s -> {
                            PrintOutputCapture<Integer> outputCapture = new PrintOutputCapture<>(0);
                            (new JavaPrinter<Integer>()).visit(s, outputCapture);
                            return outputCapture.getOut();
                        })
                        .anyMatch(s -> s.contains("Vertx") || s.contains("OpenShiftClient")
                                || s.contains("QuarkusVersionAwareCliClient"));
                if (removeMethodParameters) {
                    // in theory this could be an issue if we use Vert.x extension with parametrized test, but we don't
                    visitedMethod = visitedMethod.withParameters(List.of());
                }
            }
            return visitedMethod;
        }
    }

    private static final class UsesTypeImpl extends UsesType<ExecutionContext> {
        private UsesTypeImpl(String fullyQualifiedType) {
            super(fullyQualifiedType, true);
        }
    }
}
