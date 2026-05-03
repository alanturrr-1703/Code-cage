package com.codecage.sandbox;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Checks once at class-load time whether Docker is installed and the daemon
 * is reachable, then caches the result for the lifetime of the JVM.
 */
public final class DockerAvailability {

    private static final Logger LOG = Logger.getLogger(DockerAvailability.class.getName());

    private static final boolean AVAILABLE;

    static {
        boolean found = false;
        try {
            Process p = new ProcessBuilder("docker", "info")
                    .redirectErrorStream(true)
                    .start();
            found = p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            LOG.info("Docker not available: " + e.getMessage());
        }
        AVAILABLE = found;
        LOG.info("Docker daemon reachable: " + AVAILABLE);
    }

    private DockerAvailability() {}

    public static boolean isAvailable() { return AVAILABLE; }

    /**
     * Returns true if the image is already present in the local Docker cache
     * (no network call needed for the actual run).
     */
    public static boolean isImagePulled(String image) {
        try {
            Process p = new ProcessBuilder("docker", "image", "inspect", image)
                    .redirectErrorStream(true)
                    .start();
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
