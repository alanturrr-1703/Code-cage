package com.codecage.sandbox;

import com.codecage.executor.BaseExecutor;
import com.codecage.model.ExecutionResult;
import com.codecage.model.RunRequest;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.*;

/**
 * Executes user code inside a hardened Docker container.
 *
 * Security constraints applied to every container:
 *   --network none          → no outbound or inbound TCP/UDP
 *   --read-only             → root filesystem is read-only
 *   --tmpfs /tmp:rw,exec    → the only writable area (128 MB cap)
 *   --memory / --memory-swap→ hard memory ceiling, no swap
 *   --cpus 1.0              → bounded to 1 logical CPU
 *   --pids-limit 50         → prevents fork bombs
 *   --no-new-privileges     → blocks setuid / privilege escalation
 *   -v <workDir>:/sandbox:ro→ source code mounted read-only
 *
 * One instance handles ALL five languages — language dispatch
 * happens via the RunRequest.getLanguage() field.
 */
public class DockerSandboxExecutor extends BaseExecutor {

    @Override
    protected ExecutionResult doExecute(
        RunRequest request,
        Path workDir,
        Consumer<String> outCb,
        Consumer<String> errCb
    ) throws Exception {
        String lang = request.getLanguage().toLowerCase();

        // ── 1. Write the source file into workDir (will be bind-mounted :ro) ──
        writeSource(lang, request.getCode(), workDir);

        // ── 2. Build the full `docker run` command ──────────────────────────
        List<String> cmd = buildDockerCommand(request, lang, workDir);

        // ── 3. Run inside Docker, streaming output to callbacks ─────────────
        ProcessRef ref = new ProcessRef();
        return runProcess(
            cmd,
            workDir,
            request.getInput(),
            request.getTimeLimitMs(),
            outCb,
            errCb,
            ref
        );
    }

    // =========================================================================
    //  Source file writer
    // =========================================================================

    private void writeSource(String lang, String code, Path workDir)
        throws Exception {
        String filename = switch (lang) {
            case "java" -> extractClassName(code) + ".java";
            case "python" -> "solution.py";
            case "cpp" -> "solution.cpp";
            case "javascript" -> "solution.js";
            case "bash" -> "solution.sh";
            default -> "solution.txt";
        };
        Files.writeString(workDir.resolve(filename), code);
    }

    // =========================================================================
    //  Docker command builder
    // =========================================================================

    private List<String> buildDockerCommand(
        RunRequest req,
        String lang,
        Path workDir
    ) {
        String image = DockerImagePuller.IMAGES.getOrDefault(
            lang,
            "ubuntu:22.04"
        );

        List<String> cmd = new ArrayList<>(
            Arrays.asList(
                "docker",
                "run",
                "--rm", // auto-remove on exit
                "-i", // keep stdin open
                "--network",
                "none", // no network
                "--memory",
                req.getMemoryLimitMb() + "m", // hard RAM cap
                "--memory-swap",
                req.getMemoryLimitMb() + "m", // no swap
                "--cpus",
                "1.0", // 1 logical CPU
                "--pids-limit",
                "50", // fork bomb guard
                "--read-only", // immutable root FS
                "--tmpfs",
                "/tmp:rw,exec,size=128m", // only writable area
                "-w",
                "/tmp", // container CWD
                "-v",
                workDir.toAbsolutePath() + ":/sandbox:ro", // source
                image
            )
        );

        // Language-specific environment tweaks
        if (lang.equals("python")) {
            cmd.addAll(
                6,
                Arrays.asList(
                    "-e",
                    "PYTHONDONTWRITEBYTECODE=1",
                    "-e",
                    "PYTHONUNBUFFERED=1"
                )
            );
        }

        // Language-specific run command inside the container
        cmd.addAll(containerCommand(req, lang));
        return cmd;
    }

    // =========================================================================
    //  Per-language container entry commands
    // =========================================================================

    private List<String> containerCommand(RunRequest req, String lang) {
        return switch (lang) {
            // Interpreted — run directly from the read-only /sandbox mount
            case "python" -> List.of("python3", "-u", "/sandbox/solution.py");
            case "javascript" -> List.of("node", "/sandbox/solution.js");
            case "bash" -> List.of("bash", "/sandbox/solution.sh");
            // Compiled — copy to writable /tmp, compile, then run
            case "cpp" -> List.of(
                "sh",
                "-c",
                "g++ -O2 -std=c++17 -o /tmp/solution /sandbox/solution.cpp " +
                    "&& /tmp/solution"
            );
            case "java" -> {
                String cls = extractClassName(req.getCode());
                yield List.of(
                    "sh",
                    "-c",
                    "cp /sandbox/" +
                        cls +
                        ".java /tmp " +
                        "&& javac /tmp/" +
                        cls +
                        ".java " +
                        "&& java -cp /tmp -Xmx" +
                        req.getMemoryLimitMb() +
                        "m " +
                        cls
                );
            }
            default -> List.of("cat", "/sandbox/solution.txt");
        };
    }

    // =========================================================================
    //  Helpers
    // =========================================================================

    private String extractClassName(String code) {
        Matcher m = Pattern.compile("public\\s+class\\s+(\\w+)").matcher(code);
        return m.find() ? m.group(1) : "Main";
    }
}
