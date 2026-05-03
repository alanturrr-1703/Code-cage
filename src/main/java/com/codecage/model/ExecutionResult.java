package com.codecage.model;

public class ExecutionResult {
    private final String output;
    private final String error;
    private final ExecutionStatus status;
    private final long executionTimeMs;
    private final int exitCode;

    public ExecutionResult(String output, String error, ExecutionStatus status,
                           long executionTimeMs, int exitCode) {
        this.output = output != null ? output : "";
        this.error = error != null ? error : "";
        this.status = status;
        this.executionTimeMs = executionTimeMs;
        this.exitCode = exitCode;
    }

    public static ExecutionResult internalError(String message) {
        return new ExecutionResult("", "Internal error: " + message,
                ExecutionStatus.RE, 0, -1);
    }

    public static ExecutionResult compilationError(String errorOutput) {
        return new ExecutionResult("", errorOutput, ExecutionStatus.CE, 0, 1);
    }

    public String getOutput()           { return output; }
    public String getError()            { return error; }
    public ExecutionStatus getStatus()  { return status; }
    public long getExecutionTimeMs()    { return executionTimeMs; }
    public int getExitCode()            { return exitCode; }

    @Override
    public String toString() {
        return "ExecutionResult{status=" + status + ", time=" + executionTimeMs + "ms}";
    }
}
