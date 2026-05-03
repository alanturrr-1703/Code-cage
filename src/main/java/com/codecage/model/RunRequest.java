package com.codecage.model;

public class RunRequest {

    private String code;
    private String language; // "java", "python", "cpp", "javascript", "bash"
    private int timeLimitMs;
    private int memoryLimitMb;
    private String input;
    private boolean sandboxed = false;

    public RunRequest() {}

    public RunRequest(
        String code,
        String language,
        int timeLimitMs,
        int memoryLimitMb,
        String input,
        boolean sandboxed
    ) {
        this.code = code;
        this.language = language;
        this.timeLimitMs = timeLimitMs;
        this.memoryLimitMb = memoryLimitMb;
        this.input = input != null ? input : "";
        this.sandboxed = sandboxed;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String code = "";
        private String language = "python";
        private int timeLimitMs = 5000;
        private int memoryLimitMb = 256;
        private String input = "";
        private boolean sandboxed = false;

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder timeLimitMs(int t) {
            this.timeLimitMs = t;
            return this;
        }

        public Builder memoryLimitMb(int m) {
            this.memoryLimitMb = m;
            return this;
        }

        public Builder input(String input) {
            this.input = input;
            return this;
        }

        public Builder sandboxed(boolean s) {
            this.sandboxed = s;
            return this;
        }

        public RunRequest build() {
            return new RunRequest(
                code,
                language,
                timeLimitMs,
                memoryLimitMb,
                input,
                sandboxed
            );
        }
    }

    public String getCode() {
        return code;
    }

    public String getLanguage() {
        return language;
    }

    public int getTimeLimitMs() {
        return timeLimitMs;
    }

    public int getMemoryLimitMb() {
        return memoryLimitMb;
    }

    public String getInput() {
        return input;
    }

    public boolean isSandboxed() {
        return sandboxed;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setTimeLimitMs(int t) {
        this.timeLimitMs = t;
    }

    public void setMemoryLimitMb(int m) {
        this.memoryLimitMb = m;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public void setSandboxed(boolean sandboxed) {
        this.sandboxed = sandboxed;
    }
}
