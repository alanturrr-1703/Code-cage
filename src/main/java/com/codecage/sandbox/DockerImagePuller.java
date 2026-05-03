package com.codecage.sandbox;

import javafx.application.Platform;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Pulls every language Docker image in a single daemon thread so the first
 * sandboxed run doesn't stall waiting for a multi-hundred-MB download.
 * Images that are already cached are skipped instantly.
 */
public final class DockerImagePuller {

    private static final Logger LOG = Logger.getLogger(DockerImagePuller.class.getName());

    /** Canonical image for each language ID. */
    public static final Map<String, String> IMAGES = Map.of(
            "java",       "openjdk:21-slim",
            "python",     "python:3.12-slim",
            "cpp",        "gcc:13",
            "javascript", "node:20-slim",
            "bash",       "bash:5.2"
    );

    private DockerImagePuller() {}

    /**
     * Starts a daemon thread that pulls all images sequentially.
     * {@code onStatus} is called on the JavaFX Application Thread with progress messages.
     */
    public static void pullAllAsync(Consumer<String> onStatus) {
        if (!DockerAvailability.isAvailable()) {
            Platform.runLater(() -> onStatus.accept("⚠ Docker daemon not reachable"));
            return;
        }

        Thread t = new Thread(() -> {
            int total   = IMAGES.size();
            int current = 0;
            for (Map.Entry<String, String> entry : IMAGES.entrySet()) {
                current++;
                String lang  = entry.getKey();
                String image = entry.getValue();
                int seq = current;

                if (DockerAvailability.isImagePulled(image)) {
                    Platform.runLater(() -> onStatus.accept(
                            "🔒 [" + seq + "/" + total + "] " + lang + " cached (" + image + ")"));
                    continue;
                }

                Platform.runLater(() -> onStatus.accept(
                        "🔒 [" + seq + "/" + total + "] Pulling " + image + "…"));

                try {
                    ProcessBuilder pb = new ProcessBuilder("docker", "pull", image);
                    pb.redirectErrorStream(true);
                    Process p = pb.start();

                    // drain output so the process doesn't block on a full pipe
                    new Thread(() -> {
                        try { p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream()); }
                        catch (Exception ignored) {}
                    }).start();

                    boolean done = p.waitFor(10, TimeUnit.MINUTES);
                    if (done && p.exitValue() == 0) {
                        Platform.runLater(() -> onStatus.accept(
                                "🔒 [" + seq + "/" + total + "] " + image + " ready"));
                    } else {
                        Platform.runLater(() -> onStatus.accept(
                                "⚠ [" + seq + "/" + total + "] Failed to pull " + image));
                    }
                } catch (Exception ex) {
                    LOG.warning("Pull failed for " + image + ": " + ex.getMessage());
                    Platform.runLater(() -> onStatus.accept(
                            "⚠ [" + seq + "/" + total + "] " + image + ": " + ex.getMessage()));
                }
            }
            Platform.runLater(() -> onStatus.accept("🔒 Docker sandbox ready"));
        }, "docker-image-puller");

        t.setDaemon(true);
        t.start();
    }
}
