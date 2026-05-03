package com.codecage.model;

public enum ExecutionStatus {
    PENDING("Pending", "#888888"),
    RUNNING("Running", "#3794FF"),
    SUCCESS("Success", "#4EC9B0"),
    TLE("Time Limit", "#CE9178"),
    RE("Runtime Err", "#F44747"),
    CE("Compile Err", "#FF8C00"),
    KILLED("Killed", "#FF6B6B");

    private final String label;
    private final String color;

    ExecutionStatus(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() { return label; }
    public String getColor() { return color; }
}
