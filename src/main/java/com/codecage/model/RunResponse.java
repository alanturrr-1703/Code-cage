package com.codecage.model;

/** Simplified API response wrapper, kept for OOD compatibility. */
public class RunResponse {
    private String output;
    private String error;
    private String status;   // "SUCCESS", "TLE", "RE", "CE"
    private long executionTime;

    public RunResponse() {}

    public RunResponse(ExecutionResult result) {
        this.output = result.getOutput();
        this.error = result.getError();
        this.status = result.getStatus().name();
        this.executionTime = result.getExecutionTimeMs();
    }

    public String getOutput()        { return output; }
    public String getError()         { return error; }
    public String getStatus()        { return status; }
    public long getExecutionTime()   { return executionTime; }

    public void setOutput(String output)             { this.output = output; }
    public void setError(String error)               { this.error = error; }
    public void setStatus(String status)             { this.status = status; }
    public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
}
