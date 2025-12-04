package io.quarkus.qe.test.stats.analyzer;

import io.quarkus.qe.test.stats.analyzer.recipe.MakeCallbacksAndTestsEmpty;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

public class MakeCallbacksAndTestsEmptyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
                .recipe(new MakeCallbacksAndTestsEmpty())
                .typeValidationOptions(TypeValidation.all());
    }

    @Test
    void verifyTestMethodBlockIsEmpty() {
        //language=java
        rewriteRun(java(
                """
                package io.quarkus.qe.test.stats.analyzer;

                import org.junit.jupiter.api.Test;

                public class MyTest {
                    @Test
                    public void getUserName() {
                        int a = 0;
                    }
                }
                """,
                """
                package io.quarkus.qe.test.stats.analyzer;

                import org.junit.jupiter.api.Test;

                public class MyTest {
                    @Test
                    public void getUserName(){}
                }
                """));
    }

    @Test
    void verifyParametrizedTestMethodBlockIsEmpty() {
        //language=java
        rewriteRun(java(
                """
                package io.quarkus.qe.test.stats.analyzer;

                import org.junit.jupiter.params.ParameterizedTest;
                import org.junit.jupiter.params.provider.MethodSource;

                public class MyTest {
                    
                    @MethodSource("srcMethod")
                    @ParameterizedTest
                    public void getUserName(final int idx) {
                        int a = 0;
                    }
                    
                }
                """,
                """
                package io.quarkus.qe.test.stats.analyzer;

                import org.junit.jupiter.params.ParameterizedTest;
                import org.junit.jupiter.params.provider.MethodSource;

                public class MyTest {
                    
                    @MethodSource("srcMethod")
                    @ParameterizedTest
                    public void getUserName(final int idx){}
                    
                }
                """));
    }

    @Test
    void verifyCallbackMethodsAreEmpty() {
        //language=java
        rewriteRun(java(
                """
                package io.quarkus.qe;
                
                import org.junit.jupiter.api.AfterAll;
                import org.junit.jupiter.api.AfterEach;
                import org.junit.jupiter.api.BeforeAll;
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.Test;
                
                public class CallbacksTest {
                
                    @Test
                    public void testCallbacks() {
                        int a = 1;
                    }
                
                    @BeforeEach
                    public void before() {
                        int b = 0;
                    }
                
                    @AfterEach
                    public void after() {
                        int c = 0;
                    }
                
                    @BeforeAll
                    public static void beforeAll() {
                        int d = 0;
                    }
                
                    @AfterAll
                    public static void afterAll() {
                        int e = 0;
                    }
                
                }""",
                """
                package io.quarkus.qe;
                
                import org.junit.jupiter.api.AfterAll;
                import org.junit.jupiter.api.AfterEach;
                import org.junit.jupiter.api.BeforeAll;
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.Test;
                
                public class CallbacksTest {
                
                    @Test
                    public void testCallbacks(){}
                
                    @BeforeEach
                    public void before(){}
                
                    @AfterEach
                    public void after(){}
                
                    @BeforeAll
                    public static void beforeAll(){}
                
                    @AfterAll
                    public static void afterAll(){}
                
                }"""));
    }

    @Test
    void verifyVertxTestParametersAreRemoved() {
        //language=java
        rewriteRun(java(
                """
                package io.quarkus.qe;
                
                import org.junit.jupiter.api.AfterAll;
                import org.junit.jupiter.api.AfterEach;
                import org.junit.jupiter.api.BeforeAll;
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.Test;
                
                public class VertxParamsTest {
                
                    public record Vertx() {}
                
                    @Test
                    public void testVertx(Vertx vertx) {
                        int a = 1;
                    }
                
                }""",
                """
                package io.quarkus.qe;
                
                import org.junit.jupiter.api.AfterAll;
                import org.junit.jupiter.api.AfterEach;
                import org.junit.jupiter.api.BeforeAll;
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.Test;
                
                public class VertxParamsTest {
                
                    public record Vertx() {}
                
                    @Test
                    public void testVertx(){}
                
                }"""));
    }

    @Test
    void verifyRepeatedTestMethodBlockIsEmpty() {
        //language=java
        rewriteRun(java(
                """
                package io.quarkus.qe.test.stats.analyzer;

                import org.junit.jupiter.api.RepeatedTest;

                public class MyTest {
                    
                    @RepeatedTest
                    public void getUserName(final int idx) {
                        int a = 0;
                    }
                    
                }
                """,
                """
                package io.quarkus.qe.test.stats.analyzer;

                import org.junit.jupiter.api.RepeatedTest;

                public class MyTest {
                    
                    @RepeatedTest
                    public void getUserName(final int idx){}
                    
                }
                """));
    }
}
