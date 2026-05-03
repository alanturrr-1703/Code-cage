package com.codecage.service;

import com.codecage.executor.CodeExecutor;
import com.codecage.executor.ExecutorFactory;
import com.codecage.model.ExecutionResult;
import com.codecage.model.ExecutionStatus;
import com.codecage.model.ProcessRecord;
import com.codecage.model.RunRequest;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

public class ExecutionService extends Service<ExecutionResult> {
    private final RunRequest request;
    private final ProcessRecord record;

    public ExecutionService(RunRequest request, ProcessRecord record) {
        this.request = request;
        this.record = record;
    }

    @Override
    protected Task<ExecutionResult> createTask() {
        return new Task<>() {
            @Override
            protected ExecutionResult call() {
                record.setStatus(ExecutionStatus.RUNNING);
                try {
                    CodeExecutor executor = ExecutorFactory.getExecutor(request.getLanguage());
                    ExecutionResult result = executor.execute(
                            request,
                            chunk -> record.appendOutput(chunk),
                            chunk -> record.appendError(chunk)
                    );
                    record.applyResult(result);
                    return result;
                } catch (Exception e) {
                    ExecutionResult err = ExecutionResult.internalError(e.getMessage());
                    record.applyResult(err);
                    return err;
                }
            }
        };
    }
}
