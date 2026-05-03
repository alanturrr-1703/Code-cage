package com.codecage.executor;

import java.util.Map;

public class ExecutorFactory {

    private static final Map<String, CodeExecutor> EXECUTORS = Map.of(
            "java",       new JavaExecutor(),
            "python",     new PythonExecutor(),
            "cpp",        new CppExecutor(),
            "javascript", new JavaScriptExecutor(),
            "bash",       new BashExecutor()
    );

    private ExecutorFactory() {}

    public static CodeExecutor getExecutor(String languageId) {
        CodeExecutor executor = EXECUTORS.get(languageId.toLowerCase());
        if (executor == null) {
            throw new IllegalArgumentException("No executor for language: " + languageId);
        }
        return executor;
    }

    public static boolean isSupported(String languageId) {
        return EXECUTORS.containsKey(languageId.toLowerCase());
    }
}
