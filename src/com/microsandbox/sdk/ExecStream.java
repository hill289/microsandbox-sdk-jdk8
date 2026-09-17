package com.microsandbox.sdk;

import com.microsandbox.sdk.exception.MicrosandboxException;
import com.microsandbox.sdk.ffi.NativeBridge;
import com.microsandbox.sdk.model.ExecEvent;
import com.microsandbox.sdk.model.ExecOutput;
import jnr.ffi.Pointer;

/**
 * Handle for a streaming exec session.
 *
 * <p>JDK 8 compatible.</p>
 */
public class ExecStream implements AutoCloseable {

    private static final int DEFAULT_BUF_SIZE = 1024 * 1024;

    private final NativeBridge bridge;
    private final Pointer execHandle;          // ← 改为 Pointer
    private volatile boolean closed = false;

    ExecStream(NativeBridge bridge, Pointer execHandle) {   // ← 改为 Pointer
        this.bridge = bridge;
        this.execHandle = execHandle;
    }

    /**
     * Returns the exec session handle.
     */
    public Pointer getExecHandle() { return execHandle; }   // ← 改为 Pointer

    public ExecEvent recv() throws MicrosandboxException {
        if (closed) {
            throw MicrosandboxException.invalidHandle("exec stream is closed");
        }
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_exec_recv(
                    cancelId, execHandle, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return ExecEvent.fromJson(json);
        });
    }

    public ExecOutput collect() throws MicrosandboxException {
        if (closed) {
            throw MicrosandboxException.invalidHandle("exec stream is closed");
        }
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_exec_collect(
                    cancelId, execHandle, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return ExecOutput.fromJson(json);
        });
    }

    public int waitForExit() throws MicrosandboxException {      // 建议重命名，见下方说明
        if (closed) {
            throw MicrosandboxException.invalidHandle("exec stream is closed");
        }
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_exec_wait(
                    cancelId, execHandle, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return extractInt(json, "exit_code");
        });
    }

    public void signal(int signal) throws MicrosandboxException {
        if (closed) {
            throw MicrosandboxException.invalidHandle("exec stream is closed");
        }
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_exec_signal(
                    cancelId, execHandle, signal, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    public void kill() throws MicrosandboxException {
        if (closed) {
            throw MicrosandboxException.invalidHandle("exec stream is closed");
        }
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_exec_kill(
                    cancelId, execHandle, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    public void resize(short rows, short cols) throws MicrosandboxException {
        if (closed) {
            throw MicrosandboxException.invalidHandle("exec stream is closed");
        }
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_exec_resize(
                    cancelId, execHandle, rows, cols, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    public void stdinWrite(byte[] data) throws MicrosandboxException {
        if (closed) {
            throw MicrosandboxException.invalidHandle("exec stream is closed");
        }
        String b64 = NativeBridge.base64Encode(data);
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_exec_stdin_write(
                    cancelId, execHandle, b64, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    public void stdinClose() throws MicrosandboxException {
        if (closed) {
            throw MicrosandboxException.invalidHandle("exec stream is closed");
        }
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_exec_stdin_close(
                    cancelId, execHandle, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    public String getId() throws MicrosandboxException {
        if (closed) {
            throw MicrosandboxException.invalidHandle("exec stream is closed");
        }
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            // 注意：msb_exec_id 没有 cancelId 参数
            String err = bridge.nativeLib().msb_exec_id(
                    execHandle, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return extractField(json, "id");
        });
    }

    @Override
    public void close() throws MicrosandboxException {
        if (closed) return;
        closed = true;
        try {
            bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_exec_close(
                        cancelId, execHandle, buf, DEFAULT_BUF_SIZE);
                bridge.checkError(err, buf);
                return null;
            });
        } catch (MicrosandboxException e) {
            // Swallow close errors
        }
    }

    // ── JSON helpers ────────────────────────────────────────────────────────

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
}