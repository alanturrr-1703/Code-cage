package com.codecage.executor;

import com.codecage.model.ExecutionResult;
import com.codecage.model.RunRequest;

import java.nio.file.*;
import java.util.List;
import java.util.function.Consumer;

public class JavaScriptExecutor extends BaseExecutor {

    @Override
    protected ExecutionResult doExecute(RunRequest request, Path workDir,
                                        Consumer<String> outCb, Consumer<String> errCb)
            throws Exception {

        Path script = workDir.resolve("solution.js");
        Files.writeString(script, request.getCode());

        // Requires Node.js installed
        List<String> cmd = List.of("node", script.toString());
        ProcessRef ref = new ProcessRef();
        return runProcess(cmd, workDir, request.getInput(),
                request.getTimeLimitMs(), outCb, errCb, ref);
    }
}
