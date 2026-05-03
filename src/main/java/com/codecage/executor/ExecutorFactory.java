package com.codecage.executor;

import com.codecage.sandbox.DockerAvailability;
import com.codecage.sandbox.DockerSandboxExecutor;
import java.util.Map;

public class ExecutorFactory {

    // ── Native executors (one shared instance each, they are stateless) ───────
    private static final Map<String, CodeExecutor> NATIVE = Map.of(
        "java",
        new JavaExecutor(),
        "python",
        new PythonExecutor(),
        "cpp",
        new CppExecutor(),
        "javascript",
        new JavaScriptExecutor(),
        "bash",
        new BashExecutor()
    );

    // ── Single shared Docker executor (handles all languages) ─────────────────
    private static final DockerSandboxExecutor DOCKER =
        new DockerSandboxExecutor();

    private ExecutorFactory() {}

    /**
     * Returns a native executor for the given language.
     * Throws if the language is not supported.
     */
    public static CodeExecutor getExecutor(String languageId) {
        return getExecutor(languageId, false);
    }

    /**
     * Returns a Docker sandbox executor when {@code sandboxed=true} and Docker
     * is available; falls back to the native executor otherwise.
     */
    public static CodeExecutor getExecutor(
        String languageId,
        boolean sandboxed
    ) {
        if (sandboxed && DockerAvailability.isAvailable()) {
            return DOCKER;
        }
        CodeExecutor executor = NATIVE.get(languageId.toLowerCase());
        if (executor == null) {
            throw new IllegalArgumentException(
                "No executor for language: " + languageId
            );
        }
        return executor;
    }

    public static boolean isSupported(String languageId) {
        return NATIVE.containsKey(languageId.toLowerCase());
    }
}
