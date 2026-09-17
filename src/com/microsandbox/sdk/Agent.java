package com.microsandbox.sdk;

import com.microsandbox.sdk.exception.MicrosandboxException;
import com.microsandbox.sdk.ffi.NativeBridge;
import jnr.ffi.Pointer;
import jnr.ffi.Runtime;

/**
 * Low-level raw client for talking to agentd through the sandbox relay socket.
 *
 * <p>JDK 8 compatible.</p>
 */
public final class Agent {

    private Agent() {} // utility class

    // Protocol frame flags
    public static final int FLAG_TERMINAL = 0b0000_0001;
    public static final int FLAG_SESSION_START = 0b0000_0010;
    public static final int FLAG_SHUTDOWN = 0b0000_0100;

    /**
     * Connects to a running sandbox's agentd by name.
     */
    public static AgentClient connectSandbox(String name) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        Pointer clientHandle = bridge.withCancel(cancelId -> {
            Pointer outHandle = bridge.allocateBuffer(8);
            String err = bridge.nativeLib().msb_agent_open_sandbox(
                    cancelId, name, 10000, outHandle);
            bridge.checkError(err, null);
            return readPointer(outHandle);
        });
        return new AgentClient(bridge, clientHandle);
    }

    /**
     * Connects to an agentd relay socket by path.
     */
    public static AgentClient connectPath(String path) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        Pointer clientHandle = bridge.withCancel(cancelId -> {
            Pointer outHandle = bridge.allocateBuffer(8);
            String err = bridge.nativeLib().msb_agent_open_path(
                    cancelId, path, 10000, outHandle);
            bridge.checkError(err, null);
            return readPointer(outHandle);
        });
        return new AgentClient(bridge, clientHandle);
    }

    /**
     * Returns the host-side filesystem path of a sandbox's agentd relay socket.
     */
    public static String socketPath(String name) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        Pointer buf = bridge.allocateBuffer(4096);
        String err = bridge.nativeLib().msb_agent_socket_path(name, buf, 4096);
        String json = bridge.checkError(err, buf);
        return extractField(json, "path");
    }

    /**
     * Client handle for the agent protocol.
     */
    public static class AgentClient implements AutoCloseable {
        private final NativeBridge bridge;
        private final Pointer clientHandle;
        private volatile boolean closed = false;

        AgentClient(NativeBridge bridge, Pointer clientHandle) {
            this.bridge = bridge;
            this.clientHandle = clientHandle;
        }

        /**
         * Sends one raw frame and awaits one response frame.
         *
         * @param flags frame flags
         * @param body  CBOR-encoded message body
         * @return a RawFrame response
         */
        public RawFrame request(int flags, byte[] body) throws MicrosandboxException {
            checkOpen();
            return bridge.withCancel(cancelId -> {
                Pointer outId = bridge.allocateBuffer(4);
                Pointer outFlags = bridge.allocateBuffer(4);
                Pointer outBodyPtr = bridge.allocateBuffer(8);
                Pointer outBodyLen = bridge.allocateBuffer(8);

                Pointer bodyPtr = bridge.allocateBuffer(Math.max(body.length, 1));
                bodyPtr.put(0, body, 0, body.length);

                String err = bridge.nativeLib().msb_agent_request(
                        cancelId, clientHandle, flags,
                        bodyPtr, body.length,
                        outId, outFlags, outBodyPtr, outBodyLen);
                bridge.checkError(err, null);

                int id = outId.getInt(0);
                int outFlagsVal = outFlags.getInt(0);
                long bodyPtrVal = outBodyPtr.getLong(0);
                long bodyLen = outBodyLen.getLong(0);

                byte[] respBody = new byte[0];
                if (bodyPtrVal != 0 && bodyLen > 0 && bodyLen < Integer.MAX_VALUE) {
                    Pointer respPtr = Pointer.wrap(Runtime.getSystemRuntime(), bodyPtrVal);
                    respBody = new byte[(int) bodyLen];
                    respPtr.get(0, respBody, 0, (int) bodyLen);
                    bridge.nativeLib().msb_agent_free_bytes(respPtr, bodyLen);
                }
                return new RawFrame(id, outFlagsVal, respBody);
            });
        }

        /**
         * Opens a raw streaming session.
         *
         * @param flags frame flags
         * @param body  CBOR-encoded message body
         * @return an AgentStream handle
         */
        public AgentStream stream(int flags, byte[] body) throws MicrosandboxException {
            checkOpen();
            return bridge.withCancel(cancelId -> {
                Pointer outId = bridge.allocateBuffer(4);
                Pointer outStream = bridge.allocateBuffer(8);
                Pointer bodyPtr = bridge.allocateBuffer(Math.max(body.length, 1));
                bodyPtr.put(0, body, 0, body.length);

                String err = bridge.nativeLib().msb_agent_stream_open(
                        cancelId, clientHandle, flags,
                        bodyPtr, body.length,
                        outId, outStream);
                bridge.checkError(err, null);

                int id = outId.getInt(0);
                Pointer streamHandle = readPointer(outStream);
                return new AgentStream(bridge, clientHandle, streamHandle, id);
            });
        }

        /**
         * Sends a follow-up frame on an existing correlation id.
         */
        public void send(int id, int flags, byte[] body) throws MicrosandboxException {
            checkOpen();
            bridge.withCancel(cancelId -> {
                Pointer bodyPtr = bridge.allocateBuffer(Math.max(body.length, 1));
                bodyPtr.put(0, body, 0, body.length);
                String err = bridge.nativeLib().msb_agent_send(
                        cancelId, clientHandle, id, flags, bodyPtr, body.length);
                bridge.checkError(err, null);
                return null;
            });
        }

        /**
         * Returns the cached handshake ready CBOR body.
         */
        public byte[] readyBytes() throws MicrosandboxException {
            checkOpen();
            return bridge.withCancel(cancelId -> {
                Pointer outBodyPtr = bridge.allocateBuffer(8);
                Pointer outBodyLen = bridge.allocateBuffer(8);
                String err = bridge.nativeLib().msb_agent_ready_bytes(
                        clientHandle, outBodyPtr, outBodyLen);
                bridge.checkError(err, null);
                long bodyPtrVal = outBodyPtr.getLong(0);
                long bodyLen = outBodyLen.getLong(0);
                if (bodyPtrVal == 0 || bodyLen <= 0 || bodyLen >= Integer.MAX_VALUE) {
                    return new byte[0];
                }
                Pointer respPtr = Pointer.wrap(Runtime.getSystemRuntime(), bodyPtrVal);
                byte[] result = new byte[(int) bodyLen];
                respPtr.get(0, result, 0, (int) bodyLen);
                bridge.nativeLib().msb_agent_free_bytes(respPtr, bodyLen);
                return result;
            });
        }

        @Override
        public void close() throws MicrosandboxException {
            if (closed) return;
            closed = true;
            try {
                bridge.withCancel(cancelId -> {
                    String err = bridge.nativeLib().msb_agent_close(cancelId, clientHandle);
                    bridge.checkError(err, null);
                    return null;
                });
            } catch (MicrosandboxException e) {
                // Swallow
            }
        }

        private void checkOpen() throws MicrosandboxException {
            if (closed) {
                throw MicrosandboxException.invalidHandle("agent client is closed");
            }
        }
    }

    /**
     * An open raw streaming session.
     */
    public static class AgentStream implements AutoCloseable {
        private final NativeBridge bridge;
        private final Pointer clientHandle;
        private final Pointer streamHandle;
        private final int id;
        private volatile boolean closed = false;

        AgentStream(NativeBridge bridge, Pointer clientHandle, Pointer streamHandle, int id) {
            this.bridge = bridge;
            this.clientHandle = clientHandle;
            this.streamHandle = streamHandle;
            this.id = id;
        }

        /**
         * Returns the protocol correlation id for this stream.
         */
        public int getId() { return id; }

        /**
         * Pulls the next frame from the stream. Returns null at EOF.
         */
        public RawFrame next() throws MicrosandboxException {
            checkOpen();
            return bridge.withCancel(cancelId -> {
                Pointer outPresent = bridge.allocateBuffer(4);
                Pointer outId = bridge.allocateBuffer(4);
                Pointer outFlags = bridge.allocateBuffer(4);
                Pointer outBodyPtr = bridge.allocateBuffer(8);
                Pointer outBodyLen = bridge.allocateBuffer(8);

                String err = bridge.nativeLib().msb_agent_stream_next(
                        cancelId, clientHandle, streamHandle,
                        outPresent, outId, outFlags, outBodyPtr, outBodyLen);
                bridge.checkError(err, null);

                int present = outPresent.getInt(0);
                if (present == 0) return null;

                int id = outId.getInt(0);
                int flags = outFlags.getInt(0);
                long bodyPtrVal = outBodyPtr.getLong(0);
                long bodyLen = outBodyLen.getLong(0);

                byte[] body = new byte[0];
                if (bodyPtrVal != 0 && bodyLen > 0 && bodyLen < Integer.MAX_VALUE) {
                    Pointer respPtr = Pointer.wrap(Runtime.getSystemRuntime(), bodyPtrVal);
                    body = new byte[(int) bodyLen];
                    respPtr.get(0, body, 0, (int) bodyLen);
                    bridge.nativeLib().msb_agent_free_bytes(respPtr, bodyLen);
                }
                return new RawFrame(id, flags, body);
            });
        }

        @Override
        public void close() throws MicrosandboxException {
            if (closed) return;
            closed = true;
            try {
                bridge.withCancel(cancelId -> {
                    String err = bridge.nativeLib().msb_agent_stream_close(
                            cancelId, clientHandle, streamHandle);
                    bridge.checkError(err, null);
                    return null;
                });
            } catch (MicrosandboxException e) {
                // Swallow
            }
        }

        private void checkOpen() throws MicrosandboxException {
            if (closed) {
                throw MicrosandboxException.invalidHandle("agent stream is closed");
            }
        }
    }

    /**
     * A raw agent protocol frame.
     */
    public static class RawFrame {
        private final int id;
        private final int flags;
        private final byte[] body;

        RawFrame(int id, int flags, byte[] body) {
            this.id = id;
            this.flags = flags;
            this.body = body != null ? body : new byte[0];
        }

        public int getId() { return id; }
        public int getFlags() { return flags; }
        public byte[] getBody() { return body; }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static Pointer readPointer(Pointer buf) {
        if (buf == null) return null;
        long val = buf.getLong(0);
        if (val == 0) return null;
        return Pointer.wrap(Runtime.getSystemRuntime(), val);
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
