package io.quarkus.qe.disabled.tests.inspector;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;


@QuarkusMain
public class Main {
    public static void main(String... args) {
        Quarkus.run(AnalyserApp.class, args);
    }

    public static class AnalyserApp implements QuarkusApplication {
        @Inject
        GitHubAnalysisResource gitHubAnalysisResource;

        @Override
        public int run(String... args) {
            gitHubAnalysisResource.startAnalysis();
            return 0;
        }
    }
}
