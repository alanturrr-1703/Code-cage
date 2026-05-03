package com.codecage.executor;

import com.codecage.model.ExecutionResult;
import com.codecage.model.RunRequest;
import java.util.function.Consumer;

/**
 * Core OOD interface for language-specific code executors.
 * Supports both blocking and streaming (real-time output) execution.
 */
public interface CodeExecutor {

    /** Blocking execution — returns when the process completes. */
    ExecutionResult execute(RunRequest request);

    /**
     * Streaming execution — invokes callbacks as stdout/stderr lines arrive,
     * then returns the final result. Default falls back to blocking execution.
     */
    default ExecutionResult execute(RunRequest request,
                                    Consumer<String> outputCallback,
                                    Consumer<String> errorCallback) {
        return execute(request);
    }
}
