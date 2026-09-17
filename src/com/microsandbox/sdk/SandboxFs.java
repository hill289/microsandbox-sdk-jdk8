package com.microsandbox.sdk;

import com.microsandbox.sdk.exception.MicrosandboxException;
import com.microsandbox.sdk.ffi.NativeBridge;
import jnr.ffi.Pointer;

import java.util.ArrayList;
import java.util.List;

/**
 * Filesystem operations for a sandbox.
 *
 * <p>Obtained via {@link Sandbox#fs()}. All operations target the guest filesystem.</p>
 *
 * <p>JDK 8 compatible.</p>
 */
public class SandboxFs {

    private static final int DEFAULT_BUF_SIZE = 1024 * 1024;

    private final Sandbox sandbox;

    SandboxFs(Sandbox sandbox) {
        this.sandbox = sandbox;
    }

    /**
     * Reads a file from the guest filesystem (returns base64-encoded content).
     *
     * @param path absolute path in the guest
     * @return file contents as bytes
     */
    public byte[] readBytes(String path) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_fs_read(
                    cancelId, sandbox.getHandle(), path, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            String b64 = extractField(json, "data");
            return b64 != null ? NativeBridge.base64Decode(b64) : new byte[0];
        });
    }

    /**
     * Reads a file as a UTF-8 string.
     */
    public String readString(String path) throws MicrosandboxException {
        byte[] data = readBytes(path);
        return new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Writes data to a file in the guest filesystem.
     *
     * @param path absolute path in the guest
     * @param data file content bytes
     */
    public void writeBytes(String path, byte[] data) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        String b64 = NativeBridge.base64Encode(data);

        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_fs_write(
                    cancelId, sandbox.getHandle(), path, b64, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Writes a string to a file in the guest filesystem (UTF-8).
     */
    public void writeString(String path, String content) throws MicrosandboxException {
        writeBytes(path, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Lists directory entries.
     *
     * @param path directory path in the guest
     * @return list of entry names
     */
    public List<String> list(String path) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_fs_list(
                    cancelId, sandbox.getHandle(), path, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return parsePathArray(json);
        });
    }

    /**
     * Creates a directory (including parents).
     */
    public void mkdir(String path) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_fs_mkdir(
                    cancelId, sandbox.getHandle(), path, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Removes a file.
     */
    public void remove(String path) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_fs_remove(
                    cancelId, sandbox.getHandle(), path, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Removes a directory and its contents.
     */
    public void removeDir(String path) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_fs_remove_dir(
                    cancelId, sandbox.getHandle(), path, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Copies a file or directory within the guest filesystem.
     */
    public void copy(String src, String dst) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_fs_copy(
                    cancelId, sandbox.getHandle(), src, dst, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Renames/moves a file or directory within the guest filesystem.
     */
    public void rename(String src, String dst) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_fs_rename(
                    cancelId, sandbox.getHandle(), src, dst, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Copies a file from the host into the guest filesystem.
     *
     * @param hostPath  absolute path on the host
     * @param guestPath absolute path in the guest
     */
    public void copyFromHost(String hostPath, String guestPath) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_fs_copy_from_host(
                    cancelId, sandbox.getHandle(), hostPath, guestPath,
                    buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Copies a file from the guest filesystem to the host.
     *
     * @param guestPath absolute path in the guest
     * @param hostPath  absolute path on the host
     */
    public void copyToHost(String guestPath, String hostPath) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_fs_copy_to_host(
                    cancelId, sandbox.getHandle(), guestPath, hostPath,
                    buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Checks if a path exists in the guest filesystem.
     */
    public boolean exists(String path) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_fs_exists(
                    cancelId, sandbox.getHandle(), path, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return json.contains("true");
        });
    }


    /**
     * Returns file metadata for a path in the guest filesystem.
     *
     * @param path absolute path in the guest
     * @return a FsStat object with size, mode, mod time, and isDir flags
     */
    public FsStat stat(String path) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_fs_stat(
                    cancelId, sandbox.getHandle(), path, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return FsStat.fromJson(json);
        });
    }

    /**
     * Opens a streaming read from a guest file.
     *
     * @param path absolute path in the guest
     * @return a FsReadStream handle; must call close() when done
     */
    public FsReadStream readStream(String path) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        Pointer streamHandle = bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_fs_read_stream(
                    cancelId, sandbox.getHandle(), path, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return NativeBridge.extractPointer(json, "stream_handle");
        });
        return new FsReadStream(bridge, streamHandle);
    }

    /**
     * Opens a streaming write to a guest file.
     *
     * @param path absolute path in the guest
     * @return a FsWriteStream handle; must call close() when done to finalize
     */
    public FsWriteStream writeStream(String path) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        Pointer streamHandle = bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_fs_write_stream(
                    cancelId, sandbox.getHandle(), path, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return NativeBridge.extractPointer(json, "stream_handle");
        });
        return new FsWriteStream(bridge, streamHandle);
    }

    /**
     * A file metadata result.
     */
    public static class FsStat {
        private String path;
        private long size;
        private long mode;
        private long modTimeMs;
        private boolean isDir;

        public String getPath() { return path; }
        public long getSize() { return size; }
        public long getMode() { return mode; }
        public long getModTimeMs() { return modTimeMs; }
        public boolean isDir() { return isDir; }

        public void setPath(String v) { this.path = v; }
        public void setSize(long v) { this.size = v; }
        public void setMode(long v) { this.mode = v; }
        public void setModTimeMs(long v) { this.modTimeMs = v; }
        public void setDir(boolean v) { this.isDir = v; }

        static FsStat fromJson(String json) {
            FsStat stat = new FsStat();
            stat.path = extractField(json, "path");
            stat.size = extractLong(json, "size");
            stat.mode = extractLong(json, "mode");
            stat.modTimeMs = extractLong(json, "modified_unix") * 1000L;
            stat.isDir = json.contains("\"kind\":\"directory\"");
            return stat;
        }
    }

    /**
     * Streaming read handle for a guest file.
     */
    public static class FsReadStream implements AutoCloseable {
        private final NativeBridge bridge;
        private final Pointer streamHandle;
        private volatile boolean closed = false;

        FsReadStream(NativeBridge bridge, Pointer streamHandle) {
            this.bridge = bridge;
            this.streamHandle = streamHandle;
        }

        /**
         * Returns the next chunk of file data. Returns null at EOF.
         */
        public byte[] recv() throws MicrosandboxException {
            if (closed) {
                throw MicrosandboxException.invalidHandle("read stream is closed");
            }
            return bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_fs_read_stream_recv(
                        cancelId, streamHandle, buf, DEFAULT_BUF_SIZE);
                String json = bridge.checkError(err, buf);
                if (json == null || json.isEmpty()) return null;
                if (json.contains("\"done\":true")) return null;
                String chunkB64 = extractField(json, "chunk_b64");
                if (chunkB64 == null) return null;
                return NativeBridge.base64Decode(chunkB64);
            });
        }

        @Override
        public void close() throws MicrosandboxException {
            if (closed) return;
            closed = true;
            try {
                bridge.withCancel(cancelId -> {
                    Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                    String err = bridge.nativeLib().msb_fs_read_stream_close(
                            streamHandle, buf, DEFAULT_BUF_SIZE);
                    bridge.checkError(err, buf);
                    return null;
                });
            } catch (MicrosandboxException e) {
                // Swallow close errors
            }
        }
    }

    /**
     * Streaming write handle for a guest file.
     */
    public static class FsWriteStream implements AutoCloseable {
        private final NativeBridge bridge;
        private final Pointer streamHandle;
        private volatile boolean closed = false;

        FsWriteStream(NativeBridge bridge, Pointer streamHandle) {
            this.bridge = bridge;
            this.streamHandle = streamHandle;
        }

        /**
         * Writes a chunk of data to the guest file.
         */
        public void write(byte[] data) throws MicrosandboxException {
            if (closed) {
                throw MicrosandboxException.invalidHandle("write stream is closed");
            }
            String b64 = NativeBridge.base64Encode(data);
            bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_fs_write_stream_write(
                        cancelId, streamHandle, b64, buf, DEFAULT_BUF_SIZE);
                bridge.checkError(err, buf);
                return null;
            });
        }

        /**
         * Finalizes the write (sends EOF marker) and waits for confirmation.
         */
        @Override
        public void close() throws MicrosandboxException {
            if (closed) return;
            closed = true;
            bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_fs_write_stream_close(
                        cancelId, streamHandle, buf, DEFAULT_BUF_SIZE);
                bridge.checkError(err, buf);
                return null;
            });
        }
    }

    private static long extractLong(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return 0;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return 0;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length()
                && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        if (end == start) return 0;
        try { return Long.parseLong(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0; }
    }

    // ── JSON parsing helpers ────────────────────────────────────────────────

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

    /**
     * Parses the {@code msb_fs_list} response — a bare JSON array of entry
     * objects, each shaped like {@code {"kind":"directory","path":"/etc",...}}.
     */
    private static List<String> parsePathArray(String json) {
        List<String> result = new ArrayList<String>();
        int arrStart = json.indexOf('[');
        if (arrStart < 0) return result;
        int arrEnd = json.lastIndexOf(']');
        if (arrEnd < 0 || arrEnd <= arrStart) return result;
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
                    String path = extractField(obj, "path");
                    if (path != null) result.add(path);
                    objStart = -1;
                }
            }
        }
        return result;
    }
}