package com.codecage.executor;

import com.codecage.model.ExecutionResult;
import com.codecage.model.ExecutionStatus;
import com.codecage.model.RunRequest;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Abstract base executor that handles process lifecycle:
 * temp-dir management, streaming I/O, time-limit enforcement,
 * and clean-up. Subclasses implement doExecute().
 */
public abstract class BaseExecutor implements CodeExecutor {

    protected static final Logger LOG = Logger.getLogger(BaseExecutor.class.getName());

    @Override
    public ExecutionResult execute(RunRequest request) {
        return execute(request, null, null);
    }

    @Override
    public ExecutionResult execute(RunRequest request,
                                   Consumer<String> outputCallback,
                                   Consumer<String> errorCallback) {
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("codecage_");
            return doExecute(request, workDir, outputCallback, errorCallback);
        } catch (Exception e) {
            LOG.warning("Executor error: " + e.getMessage());
            return ExecutionResult.internalError(e.getMessage());
        } finally {
            if (workDir != null) deleteTempDir(workDir);
        }
    }

    /**
     * Template method: subclasses place their compile+run logic here.
     * The workDir is a freshly-created temp directory, deleted automatically.
     */
    protected abstract ExecutionResult doExecute(RunRequest request,
                                                  Path workDir,
                                                  Consumer<String> outCb,
                                                  Consumer<String> errCb) throws Exception;

    // -----------------------------------------------------------------------
    // Helper: run an OS command and capture its output
    // -----------------------------------------------------------------------

    /**
     * Runs an OS process, feeds stdin, streams stdout/stderr through
     * the provided callbacks, and enforces the time limit.
     *
     * @param command      command + arguments
     * @param workDir      working directory for the process
     * @param input        stdin text (may be empty)
     * @param timeLimitMs  maximum wall-clock time in milliseconds
     * @param outCb        called for each stdout chunk (may be null)
     * @param errCb        called for each stderr chunk (may be null)
     * @return             final ExecutionResult
     */
    protected ExecutionResult runProcess(List<String> command,
                                         Path workDir,
                                         String input,
                                         int timeLimitMs,
                                         Consumer<String> outCb,
                                         Consumer<String> errCb,
                                         ProcessRef processRef) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(false);

        long startTime = System.currentTimeMillis();
        Process process = pb.start();
        if (processRef != null) processRef.set(process);

        // Feed stdin
        try (OutputStream os = process.getOutputStream()) {
            if (input != null && !input.isEmpty()) {
                os.write(input.getBytes());
            }
        }

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        // Read stdout asynchronously
        Thread stdoutReader = new Thread(() -> {
            try (BufferedReader br =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String l = line + "\n";
                    stdout.append(l);
                    if (outCb != null) outCb.accept(l);
                }
            } catch (IOException ignored) {}
        }, "stdout-reader");

        // Read stderr asynchronously
        Thread stderrReader = new Thread(() -> {
            try (BufferedReader br =
                         new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String l = line + "\n";
                    stderr.append(l);
                    if (errCb != null) errCb.accept(l);
                }
            } catch (IOException ignored) {}
        }, "stderr-reader");

        stdoutReader.setDaemon(true);
        stderrReader.setDaemon(true);
        stdoutReader.start();
        stderrReader.start();

        boolean finished = process.waitFor(timeLimitMs, TimeUnit.MILLISECONDS);
        long elapsed = System.currentTimeMillis() - startTime;

        if (!finished) {
            process.destroyForcibly();
            stdoutReader.join(1000);
            stderrReader.join(1000);
            return new ExecutionResult(stdout.toString(), stderr.toString(),
                    ExecutionStatus.TLE, elapsed, -1);
        }

        stdoutReader.join(3000);
        stderrReader.join(3000);

        int exitCode = process.exitValue();
        ExecutionStatus status = (exitCode == 0) ? ExecutionStatus.SUCCESS : ExecutionStatus.RE;
        return new ExecutionResult(stdout.toString(), stderr.toString(),
                status, elapsed, exitCode);
    }

    /** Simple container so subclasses can expose the Process handle for killing. */
    public static class ProcessRef {
        private volatile Process process;
        public void set(Process p)    { this.process = p; }
        public Process get()          { return process; }
    }

    /** Compile-only helper: runs a command, returns null on success or an error string. */
    protected String compile(List<String> cmd, Path workDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(workDir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();

        StringBuilder out = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) out.append(line).append("\n");
        }

        boolean done = p.waitFor(30, TimeUnit.SECONDS);
        if (!done || p.exitValue() != 0) {
            return out.toString().isBlank() ? "Compilation failed (exit " + (done ? p.exitValue() : "timeout") + ")" : out.toString();
        }
        return null;  // success
    }

    private void deleteTempDir(Path dir) {
        try {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException ignored) {}
    }
}
