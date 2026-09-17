package com.microsandbox.sdk;

import com.microsandbox.sdk.exception.MicrosandboxException;
import com.microsandbox.sdk.ffi.NativeBridge;
import com.microsandbox.sdk.model.ExecOutput;
import com.microsandbox.sdk.model.LogEntry;
import com.microsandbox.sdk.model.Metrics;
import com.microsandbox.sdk.model.SandboxInfo;
import com.microsandbox.sdk.model.SandboxStatus;
import jnr.ffi.Pointer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A lightweight reference to a sandbox's persisted state.
 *
 * <p>Provides methods to connect, start, stop, kill, or remove the sandbox.
 * Obtain via {@link Microsandbox#getSandbox(String)}.</p>
 *
 * <p>JDK 8 compatible.</p>
 */
public class SandboxHandle {

    private static final int DEFAULT_BUF_SIZE = 1024 * 1024;

    private final String name;
    private final String id;
    private final SandboxStatus status;
    private final String backendKind;
    private final String configJson;
    private final Long createdAtUnix;
    private final Long updatedAtUnix;

    SandboxHandle(String name, String id, SandboxStatus status, String backendKind,
                  String configJson, Long createdAtUnix, Long updatedAtUnix) {
        this.name = name;
        this.id = id;
        this.status = status;
        this.backendKind = backendKind;
        this.configJson = configJson;
        this.createdAtUnix = createdAtUnix;
        this.updatedAtUnix = updatedAtUnix;
    }

    /**
     * Returns the sandbox name.
     */
    public String getName() { return name; }

    /**
     * Returns the stable identity of this persisted sandbox.
     */
    public String getId() { return id; }

    /**
     * Returns the sandbox's last-known lifecycle status.
     */
    public SandboxStatus getStatus() { return status; }

    /**
     * Returns the backend kind (e.g. "local", "cloud").
     */
    public String getBackendKind() { return backendKind; }

    /**
     * Returns the raw JSON configuration stored for this sandbox.
     */
    public String getConfigJson() { return configJson; }

    /**
     * Returns the creation time as a Unix timestamp, or null if unknown.
     */
    public Long getCreatedAtUnix() { return createdAtUnix; }

    /**
     * Returns the last-updated time as a Unix timestamp, or null if unknown.
     */
    public Long getUpdatedAtUnix() { return updatedAtUnix; }

    // ── Lifecycle operations ──────────────────────────────────────────────

    /**
     * Reattaches to the running sandbox and returns a live handle.
     */
    public Sandbox connect() throws MicrosandboxException {
        Pointer inner = lifecycleLive("connect", "{}", false);
        return new Sandbox(name, inner);
    }

    /**
     * Boots the sandbox (if stopped) and returns a live handle.
     */
    public Sandbox start() throws MicrosandboxException {
        Pointer inner = lifecycleLive("start", "{}", false);
        return new Sandbox(name, inner);
    }

    /**
     * Boots the sandbox in detached mode.
     */
    public Sandbox startDetached() throws MicrosandboxException {
        Pointer inner = lifecycleLive("start", "{\"detached\":true}", false);
        return new Sandbox(name, inner);
    }

    /**
     * Connects when this exact sandbox is running, waits while it is starting,
     * or starts it when it is created, stopped, or crashed.
     */
    public Sandbox connectOrStart() throws MicrosandboxException {
        Pointer inner = lifecycleLive("connect_or_start", "{}", false);
        return new Sandbox(name, inner);
    }

    /**
     * Connects or starts in detached mode.
     */
    public Sandbox connectOrStartDetached() throws MicrosandboxException {
        Pointer inner = lifecycleLive("connect_or_start", "{\"detached\":true}", false);
        return new Sandbox(name, inner);
    }

    /**
     * Gracefully stops the sandbox with the given timeout and waits until stopped.
     */
    public void stop(long timeout, TimeUnit unit) throws MicrosandboxException {
        long timeoutMs = unit.toMillis(timeout);
        lifecycleVoid("stop", "{\"timeout_ms\":" + timeoutMs + "}");
    }

    /**
     * Requests graceful shutdown and returns once the request is sent.
     */
    public void requestStop() throws MicrosandboxException {
        lifecycleVoid("request_stop", "{}");
    }

    /**
     * Force-kills the sandbox with the given timeout and waits until stopped.
     */
    public void kill(long timeout, TimeUnit unit) throws MicrosandboxException {
        long timeoutMs = unit.toMillis(timeout);
        lifecycleVoid("kill", "{\"timeout_ms\":" + timeoutMs + "}");
    }

    /**
     * Requests force termination and returns once the request is sent.
     */
    public void requestKill() throws MicrosandboxException {
        lifecycleVoid("request_kill", "{}");
    }

    /**
     * Starts a streaming log subscription for this sandbox handle.
     * Works without starting or connecting to the sandbox.
     *
     * @param optsJson JSON string of LogStreamOptions (may be null)
     * @return a LogStream handle
     */
    public LogStream logStream(String optsJson) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_handle_log_stream(
                    cancelId, name,
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

    /**
     * Modifies this sandbox by name.
     *
     * @param optsJson JSON string of modification options
     * @return the modification plan JSON
     */
    public String modify(String optsJson) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_handle_modify(
                    cancelId, name,
                    optsJson != null ? optsJson : "{}",
                    buf, DEFAULT_BUF_SIZE);
            return bridge.checkError(err, buf);
        });
    }

    /**
     * Captures a snapshot of this stopped sandbox under a bare name.
     *
     * @param snapshotName the snapshot name
     * @return raw JSON response with snapshot metadata
     */
    public String snapshot(String snapshotName) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_handle_snapshot(
                    cancelId, name, snapshotName, buf, DEFAULT_BUF_SIZE);
            return bridge.checkError(err, buf);
        });
    }

    /**
     * Requests graceful drain and returns once the request is sent.
     */
    public void requestDrain() throws MicrosandboxException {
        lifecycleVoid("request_drain", "{}");
    }

    /**
     * Waits until this sandbox is observed in terminal state.
     */
    public void waitUntilStopped() throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_handle_wait_until_stopped(
                    cancelId, name, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Removes the sandbox's persisted state. The sandbox must be stopped.
     */
    public void remove() throws MicrosandboxException {
        lifecycleVoid("remove", "{}");
    }

    /**
     * Checks whether agentd is reachable without refreshing idle activity.
     */
    public boolean ping() throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_handle_ping(
                    cancelId, name, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return json != null && !json.isEmpty() && json.indexOf("\"kind\"") < 0;
        });
    }

    /**
     * Explicitly refreshes this sandbox's idle activity timer.
     */
    public void touch() throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_handle_touch(
                    cancelId, name, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Returns metrics for this sandbox by name.
     */
    public Metrics metrics() throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_handle_metrics(
                    cancelId, name, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return Metrics.fromJson(json);
        });
    }

    /**
     * Reads persisted output for this sandbox handle without starting it.
     */
    public List<LogEntry> logs(String optsJson) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_handle_logs(
                    cancelId, name,
                    optsJson != null ? optsJson : "{}",
                    buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return parseLogEntries(json);
        });
    }

    /**
     * Reads persisted output for this sandbox handle with default options.
     */
    public List<LogEntry> logs() throws MicrosandboxException {
        return logs(null);
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private Pointer lifecycleLive(String operation, String optsJson, boolean detached)
            throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_handle_lifecycle(
                    cancelId, name, id, operation, optsJson, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return extractPointer(json, "handle");
        });
    }

    private void lifecycleVoid(String operation, String optsJson) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_handle_lifecycle(
                    cancelId, name, id, operation, optsJson, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Creates a SandboxHandle from a sandbox info JSON.
     */
    static SandboxHandle fromJson(String json) {
        String name = extractField(json, "name");
        String id = extractField(json, "id");
        String statusStr = extractField(json, "status");
        SandboxStatus status = SandboxStatus.fromWireValue(statusStr);
        String backendKind = extractField(json, "backend_kind");
        if (backendKind == null) backendKind = "unknown";
        String config = extractField(json, "config_json");
        Long createdAt = extractNullableLong(json, "created_at_unix");
        Long updatedAt = extractNullableLong(json, "updated_at_unix");
        return new SandboxHandle(name, id, status, backendKind, config, createdAt, updatedAt);
    }

    /**
     * Parses a list of SandboxHandle from a JSON array.
     */
    static List<SandboxHandle> parseList(String json) {
        List<SandboxHandle> result = new ArrayList<SandboxHandle>();
        int arrStart = json.indexOf('[');
        if (arrStart < 0) return result;
        int arrEnd = json.lastIndexOf(']');
        if (arrEnd < 0) return result;
        String arr = json.substring(arrStart + 1, arrEnd);

        int depth = 0;
        int objStart = -1;
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    String obj = arr.substring(objStart, i + 1);
                    result.add(fromJson(obj));
                    objStart = -1;
                }
            }
        }
        return result;
    }

    static List<LogEntry> parseLogEntries(String json) {
        List<LogEntry> result = new ArrayList<LogEntry>();
        int arrStart = json.indexOf('[');
        if (arrStart < 0) return result;
        int arrEnd = json.lastIndexOf(']');
        if (arrEnd < 0) return result;
        String arr = json.substring(arrStart + 1, arrEnd);

        int depth = 0;
        int objStart = -1;
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart >= 0) {
                    String obj = arr.substring(objStart, i + 1);
                    result.add(LogEntry.fromExtendedJson(obj));
                    objStart = -1;
                }
            }
        }
        return result;
    }

    // ── JSON helpers ──────────────────────────────────────────────────────

    static String extractField(String json, String key) {
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

    static Long extractNullableLong(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return null;
        if (json.startsWith("null", start)) return null;
        int end = start;
        while (end < json.length()
                && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        if (end == start) return null;
        try { return Long.valueOf(json.substring(start, end)); }
        catch (NumberFormatException e) { return null; }
    }

    static Pointer extractPointer(String json, String key) {
        return NativeBridge.extractPointer(json, key);
    }
}
