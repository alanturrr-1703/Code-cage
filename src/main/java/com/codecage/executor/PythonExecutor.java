package com.codecage.executor;

import com.codecage.model.ExecutionResult;
import com.codecage.model.RunRequest;

import java.nio.file.*;
import java.util.List;
import java.util.function.Consumer;

public class PythonExecutor extends BaseExecutor {

    @Override
    protected ExecutionResult doExecute(RunRequest request, Path workDir,
                                        Consumer<String> outCb, Consumer<String> errCb)
            throws Exception {

        Path script = workDir.resolve("solution.py");
        Files.writeString(script, request.getCode());

        // Try python3 first, then python
        String pythonCmd = resolveInterpreter();
        List<String> cmd = List.of(pythonCmd, "-u", script.toString());

        ProcessRef ref = new ProcessRef();
        return runProcess(cmd, workDir, request.getInput(),
                request.getTimeLimitMs(), outCb, errCb, ref);
    }

    private String resolveInterpreter() {
        for (String candidate : List.of("python3", "python")) {
            try {
                Process p = new ProcessBuilder(candidate, "--version")
                        .redirectErrorStream(true).start();
                if (p.waitFor() == 0) return candidate;
            } catch (Exception ignored) {}
        }
        return "python3"; // best guess
    }
}
