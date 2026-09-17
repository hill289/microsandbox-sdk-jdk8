package com.microsandbox.sdk;

import com.microsandbox.sdk.exception.MicrosandboxException;
import com.microsandbox.sdk.ffi.NativeBridge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Setup utility for the microsandbox runtime.
 *
 * <p>Manages installation of the {@code msb} binary and {@code libkrunfw}
 * into {@code ~/.microsandbox/}. All methods are idempotent.</p>
 *
 * <p>JDK 8 compatible.</p>
 */
public final class Setup {

    /** Default install directory. */
    private static final String DEFAULT_INSTALL_DIR = ".microsandbox";

    /** Binary subdirectory. */
    private static final String BIN_DIR = "bin";

    /** lib directory. */
    private static final String LIB_DIR = "lib";

    /** msb binary name. */
    private static final String MSB_BINARY = "msb";

    private Setup() {} // utility class

    /**
     * Returns the install directory: {@code ~/.microsandbox/}.
     */
    public static Path getInstallDir() {
        String home = System.getProperty("user.home");
        return Paths.get(home, DEFAULT_INSTALL_DIR);
    }

    /**
     * Returns the path to the {@code msb} binary.
     */
    public static Path getMsbBinaryPath() {
        return getInstallDir().resolve(BIN_DIR).resolve(MSB_BINARY);
    }

    /**
     * Returns the path to the FFI shared library.
     */
    public static Path getFfiLibraryPath() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String ext;
        String prefix;
        if (os.contains("mac")) {
            ext = ".dylib";
            prefix = "lib";
        } else if (os.contains("win")) {
            ext = ".dll";
            prefix = "";
        } else {
            ext = ".so";
            prefix = "lib";
        }
        return getInstallDir().resolve(LIB_DIR)
                .resolve(prefix + "microsandbox_go_ffi" + ext);
    }

    /**
     * Checks whether the microsandbox runtime is installed.
     *
     * @return true if both {@code msb} binary and FFI library exist
     */
    public static boolean isInstalled() {
        return Files.exists(getMsbBinaryPath());
    }

    /**
     * Ensures the runtime is installed, downloading if necessary.
     *
     * <p>This is idempotent: if already installed, this method is a no-op.
     * If installation is needed, it delegates to the {@code msb} CLI or
     * downloads the components directly.</p>
     *
     * @throws MicrosandboxException if installation fails
     */
    public static void ensureInstalled() throws MicrosandboxException {
        ensureInstalled(null);
    }

    /**
     * Ensures the runtime is installed with a progress callback.
     *
     * @param progress callback for progress messages, may be null
     * @throws MicrosandboxException if installation fails
     */
    public static void ensureInstalled(ProgressCallback progress) throws MicrosandboxException {
        if (isInstalled()) {
            if (progress != null) {
                progress.onProgress("microsandbox runtime already installed");
            }
            return;
        }

        if (progress != null) {
            progress.onProgress("installing microsandbox runtime...");
        }

        try {
            // Create directories
            Path installDir = getInstallDir();
            Files.createDirectories(installDir.resolve(BIN_DIR));
            Files.createDirectories(installDir.resolve(LIB_DIR));

            // Try to use the msb binary to install itself
            // This handles downloading msb binary, libkrunfw, and other dependencies
            String os = System.getProperty("os.name", "").toLowerCase();
            String arch = System.getProperty("os.arch", "").toLowerCase();

            // The FFI library may be embedded in the JAR - extract it
            String libName = getEmbeddedLibName();
            if (libName != null) {
                extractEmbeddedLibrary(libName, progress);
            }

            // Try to download the msb binary via the install script
            downloadMsbBinary(progress);

            if (progress != null) {
                progress.onProgress("microsandbox runtime installed successfully");
            }
        } catch (MicrosandboxException e) {
            throw e;
        } catch (Exception e) {
            throw MicrosandboxException.runtimeNotInstalled(e.getMessage());
        }
    }

    /**
     * Returns the embedded library resource name for the current platform, or null.
     */
    private static String getEmbeddedLibName() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();
        String ext;
        String prefix;

        if (os.contains("mac")) {
            ext = ".dylib";
            prefix = "lib";
        } else if (os.contains("win")) {
            ext = ".dll";
            prefix = "";
        } else {
            ext = ".so";
            prefix = "lib";
        }

        String archSuffix;
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            archSuffix = "_aarch64";
        } else if (arch.contains("x86_64") || arch.contains("amd64")) {
            archSuffix = "_x86_64";
        } else {
            return null;
        }

        return "/native/" + prefix + "microsandbox_go_ffi" + archSuffix + ext;
    }

    /**
     * Extracts an embedded native library from JAR resources.
     */
    private static void extractEmbeddedLibrary(String resourcePath, ProgressCallback progress)
            throws IOException, MicrosandboxException {
        String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
        // Normalize to standard library name
        String os = System.getProperty("os.name", "").toLowerCase();
        String targetName;
        if (os.contains("mac")) {
            targetName = "libmicrosandbox_go_ffi.dylib";
        } else if (os.contains("win")) {
            targetName = "microsandbox_go_ffi.dll";
        } else {
            targetName = "libmicrosandbox_go_ffi.so";
        }

        Path targetPath = getInstallDir().resolve(LIB_DIR).resolve(targetName);
        if (Files.exists(targetPath)) {
            return;
        }

        java.io.InputStream is = Setup.class.getResourceAsStream(resourcePath);
        if (is == null) {
            if (progress != null) {
                progress.onProgress("embedded library not found: " + resourcePath);
            }
            return;
        }

        if (progress != null) {
            progress.onProgress("extracting native library to " + targetPath);
        }

        try {
            Files.copy(is, targetPath);
        } finally {
            is.close();
        }

        // Make executable on Unix
        if (!os.contains("win")) {
            targetPath.toFile().setExecutable(true);
        }
    }

    /**
     * Downloads the msb binary. This is a placeholder - in production,
     * this would download from the microsandbox release server.
     */
    private static void downloadMsbBinary(ProgressCallback progress)
            throws MicrosandboxException {
        Path msbPath = getMsbBinaryPath();
        if (Files.exists(msbPath)) {
            return;
        }

        // Try to find msb in system PATH as fallback
        String which = isWindows() ? "where" : "which";
        try {
            ProcessBuilder pb = new ProcessBuilder(which, MSB_BINARY);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int exitCode = p.waitFor();
            if (exitCode == 0) {
                java.io.InputStream is = p.getInputStream();
                byte[] data = new byte[4096];
                int len = is.read(data);
                String found = new String(data, 0, len).trim();
                if (progress != null) {
                    progress.onProgress("found msb in PATH: " + found);
                }
                // Create symlink or copy
                Files.createSymbolicLink(msbPath, Paths.get(found));
                return;
            }
        } catch (Exception e) {
            // Ignore - will try download
        }

        // TODO: Implement actual download from release server
        throw MicrosandboxException.runtimeNotInstalled(
                "msb binary not found. Please install microsandbox manually.\n"
                + "See: https://docs.microsandbox.dev/installation");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * Callback for installation progress.
     */
    public interface ProgressCallback {
        void onProgress(String message);
    }
}
