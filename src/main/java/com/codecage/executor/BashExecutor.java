package com.codecage.executor;

import com.codecage.model.ExecutionResult;
import com.codecage.model.RunRequest;

import java.nio.file.*;
import java.util.List;
import java.util.function.Consumer;

public class BashExecutor extends BaseExecutor {

    @Override
    protected ExecutionResult doExecute(RunRequest request, Path workDir,
                                        Consumer<String> outCb, Consumer<String> errCb)
            throws Exception {

        Path script = workDir.resolve("solution.sh");
        Files.writeString(script, request.getCode());
        // Make executable
        script.toFile().setExecutable(true);

        List<String> cmd = List.of("bash", script.toString());
        ProcessRef ref = new ProcessRef();
        return runProcess(cmd, workDir, request.getInput(),
                request.getTimeLimitMs(), outCb, errCb, ref);
    }
}
