package com.microsandbox.sdk;

import com.microsandbox.sdk.exception.MicrosandboxException;
import com.microsandbox.sdk.ffi.NativeBridge;
import com.microsandbox.sdk.model.Metrics;
import jnr.ffi.Pointer;

/**
 * Handle for a streaming metrics subscription from a sandbox.
 *
 * <p>Obtain via {@link Sandbox#metricsStream(int)}. Call {@link #close()}
 * when done to release Rust-side resources.</p>
 *
 * <p>JDK 8 compatible.</p>
 */
public class MetricsStream implements AutoCloseable {

    private static final int DEFAULT_BUF_SIZE = 1024 * 1024;

    private final NativeBridge bridge;
    private final Pointer streamHandle;
    private volatile boolean closed = false;

    MetricsStream(NativeBridge bridge, Pointer streamHandle) {
        this.bridge = bridge;
        this.streamHandle = streamHandle;
    }

    /**
     * Returns the raw stream handle.
     */
    public Pointer getHandle() { return streamHandle; }

    /**
     * Blocks until the next metrics snapshot arrives. Returns null when the
     * stream has ended (sandbox exited).
     *
     * @throws MicrosandboxException on transport/runtime errors
     */
    public Metrics recv() throws MicrosandboxException {
        if (closed) {
            throw MicrosandboxException.invalidHandle("metrics stream is closed");
        }
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_metrics_recv(
                    cancelId, streamHandle, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            if (json == null || json.isEmpty() || json.equals("{}")) {
                return null;
            }
            return Metrics.fromJson(json);
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
                String err = bridge.nativeLib().msb_metrics_close(
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
