package com.microsandbox.sdk;

import com.microsandbox.sdk.exception.MicrosandboxException;
import com.microsandbox.sdk.ffi.NativeBridge;
import com.microsandbox.sdk.model.*;
import jnr.ffi.Pointer;

import java.util.concurrent.TimeUnit;

/**
 * Represents a microsandbox sandbox and provides operations on it.
 *
 * <p>Thread-safe. JDK 8 compatible.</p>
 */
public class Sandbox implements AutoCloseable {

    private static final int DEFAULT_BUF_SIZE = 1024 * 1024; // 1 MB

    private final String name;
    private final Pointer handle;      // ← 改为 Pointer
    private volatile boolean closed = false;

    Sandbox(String name, Pointer handle) {   // ← 改为 Pointer
        this.name = name;
        this.handle = handle;
    }

    /** Returns the sandbox name. */
    public String getName() { return name; }

    /** Returns the raw native handle. */
    public Pointer getHandle() { return handle; }   // ← 改为 Pointer

    /** Returns true if this handle has been closed. */
    public boolean isClosed() { return closed; }

    private void checkNotClosed() throws MicrosandboxException {
        if (closed) {
            throw MicrosandboxException.invalidHandle("sandbox '" + name + "' is closed");
        }
    }

    // ── Command Execution ───────────────────────────────────────────────────


    /**
     * Returns SSH operations for this sandbox.
     */
    public SandboxSSH ssh() {
        return new SandboxSSH(handle);
    }


    public ExecOutput exec(String cmd, String... args) throws MicrosandboxException {
        return exec(cmd, null, args);
    }

    public ExecOutput exec(String cmd, ExecOptions options, String... args) throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();
        String optsJson = buildAttachOptionsJson(options, args);

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_exec(
                    cancelId, handle, cmd, optsJson, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return ExecOutput.fromJson(json);
        });
    }

    public ExecOutput execDefault() throws MicrosandboxException {
        return execDefault(null);
    }

    public ExecOutput execDefault(ExecOptions options) throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();
        String optsJson = options != null ? options.toJson() : "{}";

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_exec_default(
                    cancelId, handle, optsJson, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return ExecOutput.fromJson(json);
        });
    }

    public ExecOutput shell(String command) throws MicrosandboxException {
        return exec("/bin/sh",
                ExecOptions.builder().args(new String[]{"-c", command}).build());
    }

    // ── Streaming Execution ─────────────────────────────────────────────────

    public ExecStream execStream(String cmd, String... args) throws MicrosandboxException {
        return execStream(cmd, null, args);
    }

    public ExecStream execStream(String cmd, ExecOptions options, String... args)
            throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();
        String optsJson = buildAttachOptionsJson(options, args);

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_exec_stream(
                    cancelId, handle, cmd, optsJson, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            Pointer execHandle = extractPointer(json, "exec_handle");  // ← Pointer
            return new ExecStream(bridge, execHandle);
        });
    }

    public ExecStream execDefaultStream() throws MicrosandboxException {
        return execDefaultStream(null);
    }

    public ExecStream execDefaultStream(ExecOptions options) throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();
        String optsJson = options != null ? options.toJson() : "{}";

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_exec_default_stream(
                    cancelId, handle, optsJson, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            Pointer execHandle = extractPointer(json, "exec_handle");  // ← Pointer
            return new ExecStream(bridge, execHandle);
        });
    }

    // ── Filesystem Operations ───────────────────────────────────────────────

    public SandboxFs fs() {
        return new SandboxFs(this);
    }

    // ── Metrics ─────────────────────────────────────────────────────────────

    public Metrics metrics() throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_metrics(
                    cancelId, handle, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return Metrics.fromJson(json);
        });
    }

    // ── Lifecycle ───────────────────────────────────────────────────────────

    public void stop(long timeout, TimeUnit unit) throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();
        long timeoutMs = unit.toMillis(timeout);

        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_stop(
                    cancelId, handle, timeoutMs, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    public void requestStop() throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();

        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_request_stop(
                    cancelId, handle, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    public void kill(long timeout, TimeUnit unit) throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();
        long timeoutMs = unit.toMillis(timeout);

        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_kill(
                    cancelId, handle, timeoutMs, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Requests force termination and returns once the request is sent.
     */
    public void requestKill() throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();

        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_request_kill(
                    cancelId, handle, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Requests graceful drain and returns once the request is sent.
     */
    public void requestDrain() throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();

        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_request_drain(
                    cancelId, handle, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Initiates graceful drain and waits for it to complete.
     */
    public void drain() throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();

        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_drain(
                    cancelId, handle, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    public void detach() throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();

        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_detach(
                    cancelId, handle, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    public boolean ping() throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_ping(
                    cancelId, handle, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return json != null && !json.isEmpty() && json.indexOf("\"kind\"") < 0;
        });
    }

    public void touch() throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();

        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_touch(
                    cancelId, handle, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    public void waitUntilStopped() throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();

        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_wait_until_stopped(
                    cancelId, handle, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Reports whether this handle owns the VM process.
     *
     * @return true if closing or stopping this handle terminates the sandbox
     */
    public boolean ownsLifecycle() throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_owns_lifecycle(
                    handle, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return json.contains("true");
        });
    }

    // ── Attach (interactive PTY) ──────────────────────────────────────────

    /**
     * Starts an interactive PTY session running cmd.
     *
     * @param cmd  the command to run
     * @param args optional arguments
     * @return the exit code
     */
    public int attach(String cmd, String... args) throws MicrosandboxException {
        return attach(cmd, null, args);
    }

    /**
     * Starts an interactive PTY session running cmd with the given options.
     *
     * @param cmd     the command to run
     * @param options attach options (may be null)
     * @param args    optional arguments
     * @return the exit code
     */
    public int attach(String cmd, ExecOptions options, String... args)
            throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();
        String optsJson = buildAttachOptionsJson(options, args);

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_attach(
                    cancelId, handle, cmd, optsJson, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return extractInt(json, "exit_code");
        });
    }

    /**
     * Starts an interactive PTY session for the effective OCI entrypoint and CMD.
     *
     * @return the exit code
     */
    public int attachDefault() throws MicrosandboxException {
        return attachDefault(null);
    }

    /**
     * Starts an interactive PTY session for the effective OCI entrypoint and CMD.
     *
     * @param options attach options (may be null)
     * @return the exit code
     */
    public int attachDefault(ExecOptions options) throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();
        String optsJson = options != null ? options.toJson() : "{}";

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_attach_default(
                    cancelId, handle, optsJson, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return extractInt(json, "exit_code");
        });
    }

    /**
     * Starts an interactive PTY session in the sandbox's default shell.
     *
     * @return the exit code
     */
    public int attachShell() throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_attach_shell(
                    cancelId, handle, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return extractInt(json, "exit_code");
        });
    }

    // ── Logs ───────────────────────────────────────────────────────────────

    /**
     * Reads persisted output logs for this sandbox.
     *
     * @param optsJson JSON string of LogOptions (may be null for defaults)
     * @return list of log entries
     */
    public java.util.List<com.microsandbox.sdk.model.LogEntry> logs(String optsJson) throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_logs(
                    cancelId, handle,
                    optsJson != null ? optsJson : "{}",
                    buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return com.microsandbox.sdk.SandboxHandle.parseLogEntries(json);
        });
    }

    /**
     * Reads persisted output logs with default options.
     */
    public java.util.List<com.microsandbox.sdk.model.LogEntry> logs() throws MicrosandboxException {
        return logs(null);
    }

    /**
     * Starts a streaming log subscription.
     *
     * @param optsJson JSON string of LogStreamOptions (may be null)
     * @return a LogStream handle
     */
    public LogStream logStream(String optsJson) throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_log_stream(
                    cancelId, handle,
                    optsJson != null ? optsJson : "{}",
                    buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            Pointer streamHandle = extractPointer(json, "stream_handle");
            return new LogStream(bridge, streamHandle);
        });
    }

    /**
     * Starts a streaming log subscription with default options.
     */
    public LogStream logStream() throws MicrosandboxException {
        return logStream(null);
    }

    // ── Metrics Stream ─────────────────────────────────────────────────────

    /**
     * Starts a streaming metrics subscription.
     *
     * @param intervalMs the interval between metrics snapshots
     * @return a MetricsStream handle
     */
    public MetricsStream metricsStream(int intervalMs) throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_metrics_stream(
                    cancelId, handle, intervalMs, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            Pointer streamHandle = extractPointer(json, "stream_handle");
            return new MetricsStream(bridge, streamHandle);
        });
    }

    // ── Modify ─────────────────────────────────────────────────────────────

    /**
     * Plans or applies a sandbox modification.
     *
     * @param optsJson JSON string of modification options
     * @return the modification plan JSON
     */
    public String modify(String optsJson) throws MicrosandboxException {
        checkNotClosed();
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_modify(
                    cancelId, handle,
                    optsJson != null ? optsJson : "{}",
                    buf, DEFAULT_BUF_SIZE);
            return bridge.checkError(err, buf);
        });
    }

    // ── Close / AutoCloseable ──────────────────────────────────────────────

    @Override
    public void close() throws MicrosandboxException {
        if (closed) return;
        closed = true;
        NativeBridge bridge = NativeBridge.getInstance();

        try {
            bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_sandbox_close(
                        cancelId, handle, buf, DEFAULT_BUF_SIZE);
                bridge.checkError(err, buf);
                return null;
            });
        } catch (MicrosandboxException e) {
            // Swallow close errors
        }
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private static String buildAttachOptionsJson(ExecOptions options, String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        if (args != null && args.length > 0) {
            sb.append("\"args\":[");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(',');
                sb.append('"').append(args[i].replace("\\", "\\\\")
                        .replace("\"", "\\\"")).append('"');
            }
            sb.append(']');
            first = false;
        }
        if (options != null) {
            String optsJson = options.toJson();
            // Strip opening/closing braces and append the fields
            String inner = optsJson;
            if (inner.startsWith("{")) inner = inner.substring(1);
            if (inner.endsWith("}")) inner = inner.substring(0, inner.length() - 1);
            if (inner.trim().length() > 0) {
                if (!first) sb.append(',');
                sb.append(inner);
                first = false;
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static int extractInt(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return -1;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return -1;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return -1;
        if (json.startsWith("null", start)) return -1;
        int end = start;
        while (end < json.length()
                && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        if (end == start) return -1;
        try { return Integer.parseInt(json.substring(start, end)); }
        catch (NumberFormatException e) { return -1; }
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    /**
     * 从 JSON 字段中提取指针（如 "handle":"0x7f..." 或 "handle":"12345"）。
     * 若字段不是数字字符串，则返回 null。
     */
    private static Pointer extractPointer(String json, String key) {
        return NativeBridge.extractPointer(json, key);
    }

    private static String extractField(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int qStart = json.indexOf('"', colon + 1);
        if (qStart < 0) return null;
        int qEnd = qStart + 1;
        while (qEnd < json.length()) {
            char c = json.charAt(qEnd);
            if (c == '\\') { qEnd += 2; continue; }
            if (c == '"') break;
            qEnd++;
        }
        return json.substring(qStart + 1, qEnd);
    }
}