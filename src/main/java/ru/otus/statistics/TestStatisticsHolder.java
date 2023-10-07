package ru.otus.statistics;

import java.util.ArrayList;
import java.util.List;

public class TestStatisticsHolder {
    private final List<TestCase> successes = new ArrayList<>();
    private final List<TestCase> failures = new ArrayList<>();

    public void addTestCase(TestCase testCase) {
        if (testCase.isSucceeded()) {
            successes.add(testCase);
        } else {
            failures.add(testCase);
        }
    }

    public List<TestCase> getSuccesses() {
        return successes;
    }

    public List<TestCase> getFailures() {
        return failures;
    }
}
