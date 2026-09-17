package com.microsandbox.sdk;

import com.microsandbox.sdk.exception.MicrosandboxException;
import com.microsandbox.sdk.ffi.NativeBridge;
import com.microsandbox.sdk.model.LogEntry;
import jnr.ffi.Pointer;

/**
 * Handle for a live log stream from a sandbox.
 *
 * <p>Obtain via {@link Sandbox#logStream(String)} or
 * {@link SandboxHandle#logStream(String)}. Call {@link #close()} when done.</p>
 *
 * <p>JDK 8 compatible.</p>
 */
public class LogStream implements AutoCloseable {

    private static final int DEFAULT_BUF_SIZE = 1024 * 1024;

    private final NativeBridge bridge;
    private final Pointer streamHandle;
    private volatile boolean closed = false;

    LogStream(NativeBridge bridge, Pointer streamHandle) {
        this.bridge = bridge;
        this.streamHandle = streamHandle;
    }

    /**
     * Returns the raw stream handle.
     */
    public Pointer getHandle() { return streamHandle; }

    /**
     * Blocks until the next log entry arrives. Returns null when the stream
     * has ended (snapshot drained, until reached, or a terminal error).
     *
     * @throws MicrosandboxException on transport/runtime errors
     */
    public LogEntry recv() throws MicrosandboxException {
        if (closed) {
            throw MicrosandboxException.invalidHandle("log stream is closed");
        }
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_log_recv(
                    cancelId, streamHandle, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            // Empty response means stream ended
            if (json == null || json.isEmpty() || json.equals("{}")) {
                return null;
            }
            return LogEntry.fromExtendedJson(json);
        });
    }

    /**
     * Closes the stream and releases Rust-side resources.
     */
    @Override
    public void close() throws MicrosandboxException {
        if (closed) return;
        closed = true;
        try {
            bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_log_close(
                        streamHandle, buf, DEFAULT_BUF_SIZE);
                bridge.checkError(err, buf);
                return null;
            });
        } catch (MicrosandboxException e) {
            // Swallow close errors
        }
    }

    /**
     * Returns true if this stream has been closed.
     */
    public boolean isClosed() { return closed; }
}
