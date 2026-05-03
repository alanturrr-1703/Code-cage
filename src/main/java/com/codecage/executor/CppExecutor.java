package com.codecage.executor;

import com.codecage.model.ExecutionResult;
import com.codecage.model.RunRequest;

import java.nio.file.*;
import java.util.List;
import java.util.function.Consumer;

public class CppExecutor extends BaseExecutor {

    @Override
    protected ExecutionResult doExecute(RunRequest request, Path workDir,
                                        Consumer<String> outCb, Consumer<String> errCb)
            throws Exception {

        Path src = workDir.resolve("solution.cpp");
        Path bin = workDir.resolve("solution");
        Files.writeString(src, request.getCode());

        // Compile with g++
        String compileError = compile(
                List.of("g++", "-O2", "-std=c++17", "-o", bin.toString(), src.toString()),
                workDir);
        if (compileError != null) return ExecutionResult.compilationError(compileError);

        ProcessRef ref = new ProcessRef();
        return runProcess(List.of(bin.toString()), workDir, request.getInput(),
                request.getTimeLimitMs(), outCb, errCb, ref);
    }
}
