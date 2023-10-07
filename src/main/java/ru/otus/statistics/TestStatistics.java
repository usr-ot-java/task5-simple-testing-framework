package ru.otus.statistics;

import java.util.List;

public class TestStatistics {
    private final List<TestCase> successes;
    private final List<TestCase> failures;

    public TestStatistics(List<TestCase> successes, List<TestCase> failures) {
        this.successes = successes;
        this.failures = failures;
    }

    public int getSuccessesTotal() {
        return successes.size();
    }

    public int getFailuresTotal() {
        return failures.size();
    }

    public List<TestCase> getSuccesses() {
        return successes;
    }

    public List<TestCase> getFailures() {
        return failures;
    }
}
