package com.codecage.executor;

import com.codecage.model.ExecutionResult;
import com.codecage.model.ExecutionStatus;
import com.codecage.model.RunRequest;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.*;

public class JavaExecutor extends BaseExecutor {

    @Override
    protected ExecutionResult doExecute(RunRequest request, Path workDir,
                                        Consumer<String> outCb, Consumer<String> errCb)
            throws Exception {

        String className = extractPublicClassName(request.getCode());
        if (className == null) className = "Main";

        Path src = workDir.resolve(className + ".java");
        Files.writeString(src, request.getCode());

        // Compile
        String compileError = compile(List.of("javac", src.toString()), workDir);
        if (compileError != null) return ExecutionResult.compilationError(compileError);

        // Run
        List<String> cmd = new ArrayList<>(List.of(
                "java",
                "-Xmx" + request.getMemoryLimitMb() + "m",
                "-cp", workDir.toString(),
                className
        ));

        ProcessRef ref = new ProcessRef();
        ExecutionResult result = runProcess(cmd, workDir, request.getInput(),
                request.getTimeLimitMs(), outCb, errCb, ref);
        return result;
    }

    private String extractPublicClassName(String code) {
        Matcher m = Pattern.compile("public\\s+class\\s+(\\w+)").matcher(code);
        return m.find() ? m.group(1) : null;
    }
}
