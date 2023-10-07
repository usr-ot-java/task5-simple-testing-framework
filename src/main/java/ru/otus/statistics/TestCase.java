package ru.otus.statistics;

public class TestCase {

    private final String name;
    private final boolean succeeded;
    private final long executionTime;

    public TestCase(String name, boolean succeeded, long executionTime) {
        this.name = name;
        this.succeeded = succeeded;
        this.executionTime = executionTime;
    }

    public boolean isSucceeded() {
        return succeeded;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public String getName() {
        return name;
    }
}
