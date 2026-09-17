package com.microsandbox.sdk.exception;

/**
 * ErrorKind identifies the specific type of microsandbox error.
 *
 * <p>The set of kinds is aligned with the Go, Node.js, and Python SDKs so that
 * portable code written against one SDK compiles against another. Some kinds
 * are reserved for error paths that the current Rust runtime does not yet emit;
 * switch statements should include a default case.</p>
 */
public enum ErrorKind {

    /** Fallback when the runtime reports an unrecognized kind. */
    UNKNOWN("unknown"),

    /** The requested sandbox does not exist. */
    SANDBOX_NOT_FOUND("sandbox_not_found"),

    /** The sandbox exists but is not running. */
    SANDBOX_NOT_RUNNING("sandbox_not_running"),

    /** A sandbox with the given name already exists. */
    SANDBOX_ALREADY_EXISTS("sandbox_already_exists"),

    /** The sandbox is still running and cannot be removed. */
    SANDBOX_STILL_RUNNING("sandbox_still_running"),

    /** A sandbox was replaced by a new identity. */
    SANDBOX_REPLACED("sandbox_replaced"),

    /** The requested volume does not exist. */
    VOLUME_NOT_FOUND("volume_not_found"),

    /** A volume with the given name already exists. */
    VOLUME_ALREADY_EXISTS("volume_already_exists"),

    /** A command execution exceeded its timeout. */
    EXEC_TIMEOUT("exec_timeout"),

    /** A command failed for a reason other than timeout or non-zero exit. */
    EXEC_FAILED("exec_failed"),

    /** A filesystem operation inside the sandbox failed. */
    FILESYSTEM("filesystem"),

    /** A sandbox filesystem path does not exist. */
    PATH_NOT_FOUND("path_not_found"),

    /** The OCI image reference could not be resolved. */
    IMAGE_NOT_FOUND("image_not_found"),

    /** The image cannot be removed because sandboxes still reference it. */
    IMAGE_IN_USE("image_in_use"),

    /** An image pull failed after resolution succeeded. */
    IMAGE_PULL_FAILED("image_pull_failed"),

    /** The requested snapshot artifact or index entry does not exist. */
    SNAPSHOT_NOT_FOUND("snapshot_not_found"),

    /** A snapshot destination already exists. */
    SNAPSHOT_ALREADY_EXISTS("snapshot_already_exists"),

    /** Snapshotting was requested for a sandbox that is still running. */
    SNAPSHOT_SANDBOX_RUNNING("snapshot_sandbox_running"),

    /** The image pinned by a snapshot is not present in the local cache. */
    SNAPSHOT_IMAGE_MISSING("snapshot_image_missing"),

    /** Snapshot verification failed. */
    SNAPSHOT_INTEGRITY("snapshot_integrity"),

    /** An adjacent-release snapshot migration is blocked. */
    SNAPSHOT_MIGRATION("snapshot_migration"),

    /** A rootfs patch could not be applied before the VM booted. */
    PATCH_FAILED("patch_failed"),

    /** A network policy configuration or runtime violation. */
    NETWORK_POLICY("network_policy"),

    /** A secret was sent to a disallowed host. */
    SECRET_VIOLATION("secret_violation"),

    /** A TLS interception error. */
    TLS("tls"),

    /** A host-side I/O error. */
    IO("io"),

    /** The sandbox or volume configuration was rejected by the runtime. */
    INVALID_CONFIG("invalid_config"),

    /** A malformed argument was passed across the FFI boundary. */
    INVALID_ARGUMENT("invalid_argument"),

    /** The sandbox handle is stale, closed, or was never valid. */
    INVALID_HANDLE("invalid_handle"),

    /** The FFI response exceeded the fixed output buffer. */
    BUFFER_TOO_SMALL("buffer_too_small"),

    /** The operation was cancelled by the caller. */
    CANCELLED("cancelled"),

    /** The microsandbox library has not been loaded. */
    LIBRARY_NOT_LOADED("library_not_loaded"),

    /** The runtime could not be found or installed. */
    RUNTIME_NOT_INSTALLED("runtime_not_installed"),

    /** The native FFI library could not be loaded. */
    FFI_LOAD_ERROR("ffi_load_error"),

    /** An error occurred within the native FFI layer. */
    FFI_ERROR("ffi_error"),

    /** Metrics sampling is disabled for this sandbox. */
    METRICS_DISABLED("metrics_disabled"),

    /** Metrics have no current sample for this sandbox. */
    METRICS_UNAVAILABLE("metrics_unavailable"),

    /** The sandbox runtime is too old for the requested feature. */
    UNSUPPORTED_OPERATION("unsupported_operation"),

    /** Every other error from the runtime. */
    INTERNAL("internal"),

    /** Neither the effective entrypoint nor CMD is executable. */
    NO_DEFAULT_COMMAND("no_default_command"),

    /** Timeout error (alias for EXEC_TIMEOUT in SDK context). */
    TIMEOUT("timeout");

    private final String wireValue;

    ErrorKind(String wireValue) {
        this.wireValue = wireValue;
    }

    /**
     * Returns the wire-format string sent over FFI.
     */
    public String wireValue() {
        return wireValue;
    }

    /**
     * Maps a wire-format string to the corresponding ErrorKind.
     *
     * @param wireValue the string received from the Rust FFI
     * @return the matching ErrorKind, or {@link #UNKNOWN} if unrecognized
     */
    public static ErrorKind fromWireValue(String wireValue) {
        if (wireValue == null) {
            return UNKNOWN;
        }
        for (ErrorKind kind : values()) {
            if (kind.wireValue.equals(wireValue)) {
                return kind;
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return wireValue;
    }
}
