package com.microsandbox.sdk.exception;

/**
 * Checked exception for microsandbox SDK errors.
 *
 * <p>Wraps an {@link ErrorKind} so callers can programmatically inspect
 * the error category without string-matching on messages.</p>
 *
 * <p>JDK 8 compatible.</p>
 */
public class MicrosandboxException extends Exception {

    private static final long serialVersionUID = 1L;

    private final ErrorKind kind;

    /**
     * Creates a new exception with the given kind and message.
     */
    public MicrosandboxException(ErrorKind kind, String message) {
        super(message);
        this.kind = kind;
    }

    /**
     * Creates a new exception with the given kind, message, and cause.
     */
    public MicrosandboxException(ErrorKind kind, String message, Throwable cause) {
        super(message, cause);
        this.kind = kind;
    }

    /**
     * Returns the error kind.
     */
    public ErrorKind getKind() {
        return kind;
    }

    // ── Static factory methods ──────────────────────────────────────────────

    public static MicrosandboxException sandboxNotFound(String name) {
        return new MicrosandboxException(ErrorKind.SANDBOX_NOT_FOUND,
                "sandbox not found: " + name);
    }

    public static MicrosandboxException sandboxAlreadyExists(String name) {
        return new MicrosandboxException(ErrorKind.SANDBOX_ALREADY_EXISTS,
                "sandbox already exists: " + name);
    }

    public static MicrosandboxException sandboxNotRunning(String name) {
        return new MicrosandboxException(ErrorKind.SANDBOX_NOT_RUNNING,
                "sandbox not running: " + name);
    }

    public static MicrosandboxException runtimeNotInstalled(String detail) {
        return new MicrosandboxException(ErrorKind.RUNTIME_NOT_INSTALLED,
                "runtime not installed: " + detail);
    }

    public static MicrosandboxException ffiLoadError(String detail) {
        return new MicrosandboxException(ErrorKind.FFI_LOAD_ERROR,
                "FFI library load error: " + detail);
    }

    public static MicrosandboxException ffiError(String detail) {
        return new MicrosandboxException(ErrorKind.FFI_ERROR,
                "FFI error: " + detail);
    }

    public static MicrosandboxException cancelled() {
        return new MicrosandboxException(ErrorKind.CANCELLED, "operation cancelled");
    }

    public static MicrosandboxException cancelled(Throwable cause) {
        return new MicrosandboxException(ErrorKind.CANCELLED, "operation cancelled", cause);
    }

    public static MicrosandboxException timeout(String detail) {
        return new MicrosandboxException(ErrorKind.TIMEOUT, "timeout: " + detail);
    }

    public static MicrosandboxException ioError(String detail) {
        return new MicrosandboxException(ErrorKind.IO, "I/O error: " + detail);
    }

    public static MicrosandboxException invalidArgument(String detail) {
        return new MicrosandboxException(ErrorKind.INVALID_ARGUMENT,
                "invalid argument: " + detail);
    }

    public static MicrosandboxException invalidHandle(String detail) {
        return new MicrosandboxException(ErrorKind.INVALID_HANDLE,
                "invalid handle: " + detail);
    }

    public static MicrosandboxException bufferTooSmall() {
        return new MicrosandboxException(ErrorKind.BUFFER_TOO_SMALL,
                "FFI response exceeded fixed output buffer");
    }

    public static MicrosandboxException noDefaultCommand() {
        return new MicrosandboxException(ErrorKind.NO_DEFAULT_COMMAND,
                "no default command (entrypoint/CMD not executable)");
    }

    /**
     * Creates an exception from a raw FFI error response.
     * Attempts to parse the JSON {@code {"kind":"...","message":"..."}} format;
     * falls back to {@link ErrorKind#FFI_ERROR}.
     */
    public static MicrosandboxException fromFfiError(String errorJson) {
        if (errorJson == null || errorJson.isEmpty()) {
            return ffiError("unknown FFI error");
        }
        try {
            // Simple JSON parsing without external dependency
            String kind = extractJsonString(errorJson, "kind");
            String message = extractJsonString(errorJson, "message");
            ErrorKind ek = ErrorKind.fromWireValue(kind);
            return new MicrosandboxException(ek,
                    message != null ? message : errorJson);
        } catch (Exception e) {
            return ffiError(errorJson);
        }
    }

    // ── Minimal JSON helpers (avoid Gson dependency in exception layer) ─────

    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) {
            return null;
        }
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) {
            return null;
        }
        int quoteStart = json.indexOf('"', colon + 1);
        if (quoteStart < 0) {
            return null;
        }
        int quoteEnd = findUnescapedQuote(json, quoteStart + 1);
        if (quoteEnd < 0) {
            return json.substring(quoteStart + 1);
        }
        return json.substring(quoteStart + 1, quoteEnd);
    }

    private static int findUnescapedQuote(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++; // skip escaped char
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }
}
