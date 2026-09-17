package com.microsandbox.sdk.ffi;

import jnr.ffi.Platform;
import jnr.ffi.Runtime;

import java.io.*;
import java.nio.file.*;
import java.util.Locale;
import java.io.*;
import java.nio.file.*;
import java.util.Locale;

/**
 * Custom library loader for the microsandbox native FFI library.
 * <p>
 * Handles platform detection, architecture detection, and multiple loading strategies
 * including embedded JAR resources, system PATH, and environment variable overrides.
 * <p>
 * Thread-safe singleton initialization with double-checked locking.
 * JDK 8 compatible.
 */
public final class LibraryLoader {

    /** Base name of the native library (without platform-specific prefix/suffix). */
    private static final String LIBRARY_BASE_NAME = "microsandbox_go_ffi";

    /** Environment variable for overriding the library path. */
    private static final String FFI_PATH_ENV_VAR = "MICROSANDBOX_FFI_PATH";

    /** Resource path prefix for embedded libraries in JAR. */
    private static final String NATIVE_RESOURCE_PREFIX = "/native/";

    /** Temp directory prefix for extracted libraries. */
    private static final String TEMP_DIR_PREFIX = "microsandbox_ffi_";

    /** File extension for shared libraries on Linux. */
    private static final String LINUX_EXT = ".so";

    /** File extension for shared libraries on macOS. */
    private static final String MACOS_EXT = ".dylib";

    /** File extension for shared libraries on Windows. */
    private static final String WINDOWS_EXT = ".dll";

    /** Synchronization lock for singleton initialization. */
    private static final Object LOCK = new Object();

    /** Volatile reference to the loaded library interface. */
    private static volatile MicrosandboxNative loadedLibrary;

    /** Volatile reference to the JNR runtime. */
    private static volatile Runtime loadedRuntime;

    /** Cached platform info. */
    private static volatile PlatformInfo platformInfo;

    /**
     * Private constructor to prevent instantiation.
     */
    private LibraryLoader() {
        throw new AssertionError("LibraryLoader is a static utility class and cannot be instantiated.");
    }

    /**
     * Returns the singleton instance of the native library interface.
     * Uses double-checked locking for thread safety.
     *
     * @return the loaded native library interface
     * @throws UnsatisfiedLinkError if the library cannot be loaded
     */
    public static MicrosandboxNative load() {
        if (loadedLibrary == null) {
            synchronized (LOCK) {
                if (loadedLibrary == null) {
                    loadedLibrary = loadLibraryInternal();
                }
            }
        }
        return loadedLibrary;
    }
    

    /**
     * Returns the JNR runtime associated with the loaded library.
     *
     * @return the JNR runtime
     * @throws IllegalStateException if the library has not been loaded yet
     */
    public static Runtime getRuntime() {
        if (loadedRuntime == null) {
            synchronized (LOCK) {
                if (loadedRuntime == null) {
                    // Ensure library is loaded first
                    load();
                }
            }
        }
        return loadedRuntime;
    }

    /**
     * Forces a reload of the library. Useful for testing or recovery.
     */
    public static void reload() {
        synchronized (LOCK) {
            loadedLibrary = null;
            loadedRuntime = null;
            loadedLibrary = loadLibraryInternal();
        }
    }

    /**
     * Internal method that performs the actual library loading.
     *
     * @return the loaded native library interface
     * @throws UnsatisfiedLinkError if the library cannot be loaded */
    
    private static MicrosandboxNative loadLibraryInternal() {
        PlatformInfo info = getPlatformInfo();
        System.out.println("[LibraryLoader] Detected platform: " + info.os + "/" + info.arch);

        // Try loading strategies in order of preference
        UnsatisfiedLinkError lastError = null;

        // Strategy 1: Load from MICROSANDBOX_FFI_PATH environment variable
        try {
            String envPath = System.getenv(FFI_PATH_ENV_VAR);
            if (envPath != null && !envPath.isEmpty()) {
                System.out.println("[LibraryLoader] Attempting to load from " + FFI_PATH_ENV_VAR + ": " + envPath);
                return loadFromPath(envPath, info);
            }
        } catch (UnsatisfiedLinkError e) {
            lastError = e;
            System.out.println("[LibraryLoader] Failed to load from env var: " + e.getMessage());
        }

        // Strategy 2: Load from embedded JAR resource
        try {
            System.out.println("[LibraryLoader] Attempting to load from JAR resources...");
            return loadFromJarResource(info);
        } catch (UnsatisfiedLinkError e) {
            lastError = e;
            System.out.println("[LibraryLoader] Failed to load from JAR: " + e.getMessage());
        }

        // Strategy 3: Load from system PATH using JNR's default mechanism
        try {
            System.out.println("[LibraryLoader] Attempting to load from system PATH...");
            return loadFromSystemPath(info);
        } catch (UnsatisfiedLinkError e) {
            lastError = e;
            System.out.println("[LibraryLoader] Failed to load from system PATH: " + e.getMessage());
        }

        // All strategies failed
        String errorMsg = "Failed to load native library " + LIBRARY_BASE_NAME + ". "
                + "Platform: " + info.os + "/" + info.arch + ". "
                + "Last error: " + (lastError != null ? lastError.getMessage() : "unknown");
        throw new UnsatisfiedLinkError(errorMsg);
    }

    /**
     * Loads the library from a specific file path.
     *
     * @param path the path to the library file
     * @param info platform information
     * @return the loaded native library interface
     */
    private static MicrosandboxNative loadFromPath(String path, PlatformInfo info) {
        Path libPath = Paths.get(path);
        if (Files.exists(libPath)) {
            return loadFromFile(libPath.toFile());
        }

        // Try with platform-specific extension
        Path withExt = Paths.get(path + info.extension);
        if (Files.exists(withExt)) {
            return loadFromFile(withExt.toFile());
        }

        // Try as a directory containing the library
        if (Files.isDirectory(libPath)) {
            Path libInDir = libPath.resolve(info.libraryFileName);
            if (Files.exists(libInDir)) {
                return loadFromFile(libInDir.toFile());
            }
        }

        throw new UnsatisfiedLinkError("Library not found at path: " + path);
    }

    /**
     * Loads the library from a JAR embedded resource.
     * Extracts to a temp directory if necessary.
     *
     * @param info platform information
     * @return the loaded native library interface
     */
    private static MicrosandboxNative loadFromJarResource(PlatformInfo info) {
        String resourcePath = info.libraryFileName;
        InputStream is = LibraryLoader.class.getResourceAsStream(resourcePath);

        if (is == null) {
            throw new UnsatisfiedLinkError("Native library resource not found: " + resourcePath);
        }

        try {
            // Create temp directory for extraction
            Path tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
            Path tempLib = tempDir.resolve(info.libraryFileName);

            // Extract the library
            OutputStream out = Files.newOutputStream(tempLib);
            try {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.close();
            }

            // Make executable on Unix-like systems
            if (!info.os.equals(OsType.WINDOWS)) {
                tempLib.toFile().setExecutable(true);
            }

            System.out.println("[LibraryLoader] Extracted library to: " + tempLib.toAbsolutePath());

            // Load the extracted library
            return loadFromFile(tempLib.toFile());

        } catch (IOException e) {
            throw new UnsatisfiedLinkError("Failed to extract native library: " + e.getMessage());
        } finally {
            closeQuietly(is);
        }
    }

    /**
     * Loads the library from the system PATH using JNR's default mechanism.
     *
     * @param info platform information
     * @return the loaded native library interface
     */
    private static MicrosandboxNative loadFromSystemPath(PlatformInfo info) {
        return jnr.ffi.LibraryLoader.create(MicrosandboxNative.class)
                .load(info.libraryBaseName);
    }

    /**
     * Loads the library from a specific file.
     *
     * @param libFile the library file
     * @return the loaded native library interface
     */
    private static MicrosandboxNative loadFromFile(File libFile) {
        System.out.println("[LibraryLoader] Loading library from file: " + libFile.getAbsolutePath());

        jnr.ffi.LibraryLoader<MicrosandboxNative> loader =
                jnr.ffi.LibraryLoader.create(MicrosandboxNative.class);

        MicrosandboxNative lib = loader.load(libFile.getAbsolutePath());

        // Capture the runtime
        loadedRuntime = Runtime.getRuntime(lib);

        System.out.println("[LibraryLoader] Library loaded successfully.");
        return lib;
    }

    /**
     * Gets or initializes platform information.
     *
     * @return platform information
     */
    private static PlatformInfo getPlatformInfo() {
        if (platformInfo == null) {
            synchronized (LOCK) {
                if (platformInfo == null) {
                    platformInfo = detectPlatform();
                }
            }
        }
        return platformInfo;
    }

    /**
     * Detects the current platform and architecture.
     *
     * @return platform information
     */
    private static PlatformInfo detectPlatform() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String osArch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

        OsType os;
        if (osName.contains("linux")) {
            os = OsType.LINUX;
        } else if (osName.contains("mac") || osName.contains("darwin")) {
            os = OsType.MACOS;
        } else if (osName.contains("win")) {
            os = OsType.WINDOWS;
        } else {
            os = OsType.UNKNOWN;
        }

        ArchType arch;
        if (osArch.equals("amd64") || osArch.equals("x86_64") || osArch.equals("x64")) {
            arch = ArchType.X86_64;
        } else if (osArch.equals("aarch64") || osArch.equals("arm64")) {
            arch = ArchType.AARCH64;
        } else if (osArch.contains("arm")) {
            arch = ArchType.ARM;
        } else {
            arch = ArchType.UNKNOWN;
        }

        String extension;
        String libraryBaseName;
        String libraryFileName;

        switch (os) {
            case LINUX:
                extension = LINUX_EXT;
                libraryBaseName = "lib" + LIBRARY_BASE_NAME;
                libraryFileName = libraryBaseName + extension;
                break;
            case MACOS:
                extension = MACOS_EXT;
                libraryBaseName = "lib" + LIBRARY_BASE_NAME;
                libraryFileName = libraryBaseName + extension;
                break;
            case WINDOWS:
                extension = WINDOWS_EXT;
                libraryBaseName = LIBRARY_BASE_NAME;
                libraryFileName = libraryBaseName + extension;
                break;
            default:
                extension = LINUX_EXT;
                libraryBaseName = "lib" + LIBRARY_BASE_NAME;
                libraryFileName = libraryBaseName + extension;
                System.out.println("[LibraryLoader] Unknown OS '" + osName + "', assuming Linux conventions.");
                break;
        }

        System.out.println("[LibraryLoader] OS: " + os + ", Arch: " + arch
                + ", Library: " + libraryFileName);

        return new PlatformInfo(os, arch, extension, libraryBaseName, libraryFileName);
    }

    /**
     * Closes an InputStream without throwing exceptions.
     *
     * @param is the stream to close
     */
    private static void closeQuietly(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException ignored) {
                // Ignore close errors
            }
        }
    }

    // ========================================================================
    // Inner types
    // ========================================================================

    /**
     * Operating system type enumeration.
     */
    enum OsType {
        LINUX, MACOS, WINDOWS, UNKNOWN
    }

    /**
     * CPU architecture type enumeration.
     */
    enum ArchType {
        X86_64, AARCH64, ARM, UNKNOWN
    }

    /**
     * Platform information holder.
     */
    static final class PlatformInfo {
        final OsType os;
        final ArchType arch;
        final String extension;
        final String libraryBaseName;
        final String libraryFileName;

        PlatformInfo(OsType os, ArchType arch, String extension,
                     String libraryBaseName, String libraryFileName) {
            this.os = os;
            this.arch = arch;
            this.extension = extension;
            this.libraryBaseName = libraryBaseName;
            this.libraryFileName = libraryFileName;
        }

        @Override
        public String toString() {
            return "PlatformInfo{os=" + os + ", arch=" + arch
                    + ", library=" + libraryFileName + "}";
        }
    }
}
