package com.microsandbox.sdk;

import com.microsandbox.sdk.exception.MicrosandboxException;
import com.microsandbox.sdk.ffi.NativeBridge;
import jnr.ffi.Pointer;

/**
 * SSH operations for a sandbox.
 *
 * <p>Provides client (connect, exec, attach, SFTP) and server (prepare, serve)
 * SSH capabilities matching the Go SDK's SandboxSSHOps.</p>
 *
 * <p>JDK 8 compatible.</p>
 */
public class SandboxSSH {

    private static final int DEFAULT_BUF_SIZE = 1024 * 1024;

    private final Pointer sandboxHandle;

    SandboxSSH(Pointer sandboxHandle) {
        this.sandboxHandle = sandboxHandle;
    }

    /**
     * Opens a native in-process SSH client to this sandbox.
     *
     * @param optsJson JSON options for the SSH client (may be null)
     * @return an SSHClient handle
     */
    public SSHClient openClient(String optsJson) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        Pointer clientHandle = bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_ssh_connect(
                    cancelId, sandboxHandle,
                    optsJson != null ? optsJson : "{}",
                    buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return extractPointer(json, "client_handle");
        });
        return new SSHClient(bridge, clientHandle);
    }

    /**
     * Prepares a reusable SSH server endpoint for this sandbox.
     *
     * @param optsJson JSON options for the SSH server (may be null)
     * @return an SSHServer handle
     */
    public SSHServer prepareServer(String optsJson) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        Pointer serverHandle = bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_ssh_server(
                    cancelId, sandboxHandle,
                    optsJson != null ? optsJson : "{}",
                    buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return extractPointer(json, "server_handle");
        });
        return new SSHServer(bridge, serverHandle);
    }

    /**
     * SSH client session for exec, attach, and SFTP operations.
     */
    public static class SSHClient implements AutoCloseable {
        private final NativeBridge bridge;
        private final Pointer clientHandle;
        private volatile boolean closed = false;

        SSHClient(NativeBridge bridge, Pointer clientHandle) {
            this.bridge = bridge;
            this.clientHandle = clientHandle;
        }

        /**
         * Runs an SSH exec request and collects output.
         *
         * @param command  the command to execute
         * @param optsJson options (may be null)
         * @return raw JSON response with stdout, stderr, and status
         */
        public String exec(String command, String optsJson) throws MicrosandboxException {
            checkOpen();
            return bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_ssh_client_exec(
                        cancelId, clientHandle, command,
                        optsJson != null ? optsJson : "{}",
                        buf, DEFAULT_BUF_SIZE);
                return bridge.checkError(err, buf);
            });
        }

        /**
         * Runs an SSH exec request with default options.
         */
        public String exec(String command) throws MicrosandboxException {
            return exec(command, null);
        }

        /**
         * Attaches the local terminal to an interactive SSH shell.
         *
         * @param optsJson options (may be null)
         * @return the exit status
         */
        public int attach(String optsJson) throws MicrosandboxException {
            checkOpen();
            return bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_ssh_client_attach(
                        cancelId, clientHandle,
                        optsJson != null ? optsJson : "{}",
                        buf, DEFAULT_BUF_SIZE);
                String json = bridge.checkError(err, buf);
                return extractInt(json, "exit_code");
            });
        }

        /**
         * Opens an SFTP session over this SSH connection.
         *
         * @return an SFTPClient handle
         */
        public SFTPClient sftp() throws MicrosandboxException {
            checkOpen();
            Pointer sftpHandle = bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_ssh_client_sftp(
                        cancelId, clientHandle, buf, DEFAULT_BUF_SIZE);
                String json = bridge.checkError(err, buf);
                return extractPointer(json, "sftp_handle");
            });
            return new SFTPClient(bridge, sftpHandle);
        }

        @Override
        public void close() throws MicrosandboxException {
            if (closed) return;
            closed = true;
            try {
                bridge.withCancel(cancelId -> {
                    Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                    String err = bridge.nativeLib().msb_ssh_client_close(
                            cancelId, clientHandle, buf, DEFAULT_BUF_SIZE);
                    bridge.checkError(err, buf);
                    return null;
                });
            } catch (MicrosandboxException e) {
                // Swallow close errors
            }
        }

        private void checkOpen() throws MicrosandboxException {
            if (closed) {
                throw MicrosandboxException.invalidHandle("SSH client is closed");
            }
        }
    }

    /**
     * A prepared SSH server endpoint for a sandbox.
     */
    public static class SSHServer implements AutoCloseable {
        private final NativeBridge bridge;
        private final Pointer serverHandle;
        private volatile boolean closed = false;

        SSHServer(NativeBridge bridge, Pointer serverHandle) {
            this.bridge = bridge;
            this.serverHandle = serverHandle;
        }

        /**
         * Serves one SSH transport over this process's stdin/stdout.
         */
        public void serveConnection() throws MicrosandboxException {
            checkOpen();
            bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_ssh_server_serve_connection(
                        cancelId, serverHandle, buf, DEFAULT_BUF_SIZE);
                bridge.checkError(err, buf);
                return null;
            });
        }

        @Override
        public void close() throws MicrosandboxException {
            if (closed) return;
            closed = true;
            try {
                bridge.withCancel(cancelId -> {
                    Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                    String err = bridge.nativeLib().msb_ssh_server_close(
                            cancelId, serverHandle, buf, DEFAULT_BUF_SIZE);
                    bridge.checkError(err, buf);
                    return null;
                });
            } catch (MicrosandboxException e) {
                // Swallow close errors
            }
        }

        private void checkOpen() throws MicrosandboxException {
            if (closed) {
                throw MicrosandboxException.invalidHandle("SSH server is closed");
            }
        }
    }

    /**
     * SFTP client session for file operations over SSH.
     */
    public static class SFTPClient implements AutoCloseable {
        private final NativeBridge bridge;
        private final Pointer sftpHandle;
        private volatile boolean closed = false;

        SFTPClient(NativeBridge bridge, Pointer sftpHandle) {
            this.bridge = bridge;
            this.sftpHandle = sftpHandle;
        }

        /**
         * Reads a file into memory.
         *
         * @param path remote path
         * @return file contents as bytes
         */
        public byte[] read(String path) throws MicrosandboxException {
            checkOpen();
            return bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_sftp_read(
                        cancelId, sftpHandle, path, buf, DEFAULT_BUF_SIZE);
                String json = bridge.checkError(err, buf);
                String dataB64 = extractField(json, "data_b64");
                if (dataB64 == null) dataB64 = extractField(json, "data");
                return dataB64 != null ? NativeBridge.base64Decode(dataB64) : new byte[0];
            });
        }

        /**
         * Writes a file, creating or truncating it.
         *
         * @param path remote path
         * @param data file content
         */
        public void write(String path, byte[] data) throws MicrosandboxException {
            checkOpen();
            String b64 = NativeBridge.base64Encode(data);
            bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_sftp_write(
                        cancelId, sftpHandle, path, b64, buf, DEFAULT_BUF_SIZE);
                bridge.checkError(err, buf);
                return null;
            });
        }

        /**
         * Creates a directory.
         */
        public void mkdir(String path) throws MicrosandboxException {
            checkOpen();
            bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_sftp_mkdir(
                        cancelId, sftpHandle, path, buf, DEFAULT_BUF_SIZE);
                bridge.checkError(err, buf);
                return null;
            });
        }

        /**
         * Removes a file.
         */
        public void removeFile(String path) throws MicrosandboxException {
            checkOpen();
            bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_sftp_remove_file(
                        cancelId, sftpHandle, path, buf, DEFAULT_BUF_SIZE);
                bridge.checkError(err, buf);
                return null;
            });
        }

        /**
         * Removes an empty directory.
         */
        public void removeDir(String path) throws MicrosandboxException {
            checkOpen();
            bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_sftp_remove_dir(
                        cancelId, sftpHandle, path, buf, DEFAULT_BUF_SIZE);
                bridge.checkError(err, buf);
                return null;
            });
        }

        /**
         * Renames a file or directory.
         */
        public void rename(String oldPath, String newPath) throws MicrosandboxException {
            checkOpen();
            bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_sftp_rename(
                        cancelId, sftpHandle, oldPath, newPath, buf, DEFAULT_BUF_SIZE);
                bridge.checkError(err, buf);
                return null;
            });
        }

        /**
         * Resolves a path to its canonical absolute form.
         */
        public String realPath(String path) throws MicrosandboxException {
            checkOpen();
            return bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_sftp_real_path(
                        cancelId, sftpHandle, path, buf, DEFAULT_BUF_SIZE);
                String json = bridge.checkError(err, buf);
                return extractField(json, "path");
            });
        }

        /**
         * Reads a symlink target.
         */
        public String readLink(String path) throws MicrosandboxException {
            checkOpen();
            return bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_sftp_read_link(
                        cancelId, sftpHandle, path, buf, DEFAULT_BUF_SIZE);
                String json = bridge.checkError(err, buf);
                return extractField(json, "target");
            });
        }

        /**
         * Creates a symlink.
         */
        public void symlink(String target, String linkPath) throws MicrosandboxException {
            checkOpen();
            bridge.withCancel(cancelId -> {
                Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                String err = bridge.nativeLib().msb_sftp_symlink(
                        cancelId, sftpHandle, target, linkPath, buf, DEFAULT_BUF_SIZE);
                bridge.checkError(err, buf);
                return null;
            });
        }

        @Override
        public void close() throws MicrosandboxException {
            if (closed) return;
            closed = true;
            try {
                bridge.withCancel(cancelId -> {
                    Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
                    String err = bridge.nativeLib().msb_sftp_close(
                            cancelId, sftpHandle, buf, DEFAULT_BUF_SIZE);
                    bridge.checkError(err, buf);
                    return null;
                });
            } catch (MicrosandboxException e) {
                // Swallow close errors
            }
        }

        private void checkOpen() throws MicrosandboxException {
            if (closed) {
                throw MicrosandboxException.invalidHandle("SFTP client is closed");
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

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
