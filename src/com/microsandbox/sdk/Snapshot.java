package com.microsandbox.sdk;

import com.microsandbox.sdk.exception.MicrosandboxException;
import com.microsandbox.sdk.ffi.NativeBridge;
import jnr.ffi.Pointer;

import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot management operations for the microsandbox SDK.
 *
 * <p>Provides access to snapshot artifacts and the snapshot index.</p>
 *
 * <p>JDK 8 compatible.</p>
 */
public final class Snapshot {

    private static final int DEFAULT_BUF_SIZE = 1024 * 1024;

    private Snapshot() {} // utility class

    // ── Options ───────────────────────────────────────────────────────────

    /**
     * Options for creating a snapshot.
     */
    public static class CreateOptions {
        private String name;
        private String fromSandbox;
        private String destDir;
        private java.util.Map<String, String> labels;
        private boolean force;
        private boolean recordIntegrity;
        private boolean resumable;

        public CreateOptions name(String v) { this.name = v; return this; }
        public CreateOptions fromSandbox(String v) { this.fromSandbox = v; return this; }
        public CreateOptions destDir(String v) { this.destDir = v; return this; }
        public CreateOptions labels(java.util.Map<String, String> v) { this.labels = v; return this; }
        public CreateOptions force(boolean v) { this.force = v; return this; }
        public CreateOptions recordIntegrity(boolean v) { this.recordIntegrity = v; return this; }
        public CreateOptions resumable(boolean v) { this.resumable = v; return this; }

        String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            if (name != null && !name.isEmpty()) {
                sb.append("\"name\":\"").append(name).append('"');
                first = false;
            }
            if (destDir != null && !destDir.isEmpty()) {
                if (!first) sb.append(',');
                sb.append("\"dest_dir\":\"").append(destDir).append('"');
                first = false;
            }
            if (labels != null && !labels.isEmpty()) {
                if (!first) sb.append(',');
                sb.append("\"labels\":{");
                boolean fe = true;
                for (java.util.Map.Entry<String, String> e : labels.entrySet()) {
                    if (!fe) sb.append(',');
                    sb.append('"').append(e.getKey()).append("\":\"")
                      .append(e.getValue()).append('"');
                    fe = false;
                }
                sb.append('}');
                first = false;
            }
            if (force) {
                if (!first) sb.append(',');
                sb.append("\"force\":true");
                first = false;
            }
            if (recordIntegrity) {
                if (!first) sb.append(',');
                sb.append("\"record_integrity\":true");
                first = false;
            }
            if (resumable) {
                if (!first) sb.append(',');
                sb.append("\"resumable\":true");
            }
            sb.append('}');
            return sb.toString();
        }
    }

    // ── Operations ────────────────────────────────────────────────────────

    /**
     * Creates a snapshot from a stopped sandbox.
     *
     * @param name   snapshot name
     * @param from   source sandbox name (must be stopped)
     * @param options optional creation options
     * @return raw JSON response with snapshot metadata
     */
    public static String create(String name, String from, CreateOptions options)
            throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        CreateOptions opts = options != null ? options : new CreateOptions();
        opts.name(name);
        opts.fromSandbox(from);
        String optsJson = opts.toJson();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_snapshot_create(
                    cancelId, from, optsJson, buf, DEFAULT_BUF_SIZE);
            return bridge.checkError(err, buf);
        });
    }

    /**
     * Opens a snapshot artifact by path or name.
     */
    public static String open(String pathOrName) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_snapshot_open(
                    cancelId, pathOrName, buf, DEFAULT_BUF_SIZE);
            return bridge.checkError(err, buf);
        });
    }

    /**
     * Verifies the content integrity of a snapshot.
     */
    public static String verify(String pathOrName) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_snapshot_verify(
                    cancelId, pathOrName, buf, DEFAULT_BUF_SIZE);
            return bridge.checkError(err, buf);
        });
    }

    /**
     * Looks up a snapshot by name or digest.
     */
    public static String get(String nameOrDigest) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_snapshot_get(
                    cancelId, nameOrDigest, buf, DEFAULT_BUF_SIZE);
            return bridge.checkError(err, buf);
        });
    }

    /**
     * Lists all snapshots in the index.
     */
    public static String list() throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_snapshot_list(
                    cancelId, buf, DEFAULT_BUF_SIZE);
            return bridge.checkError(err, buf);
        });
    }

    /**
     * Lists snapshot artifacts in a directory.
     */
    public static String listDir(String dir) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_snapshot_list_dir(
                    cancelId, dir, buf, DEFAULT_BUF_SIZE);
            return bridge.checkError(err, buf);
        });
    }

    /**
     * Removes a snapshot by path or name.
     */
    public static void remove(String pathOrName, boolean force) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_snapshot_remove(
                    cancelId, pathOrName, force ? 1 : 0, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Removes a snapshot with force=false.
     */
    public static void remove(String pathOrName) throws MicrosandboxException {
        remove(pathOrName, false);
    }

    /**
     * Reindexes the snapshot directory.
     *
     * @return the number of snapshots reindexed
     */
    public static long reindex(String dir) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_snapshot_reindex(
                    cancelId, dir, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return extractLong(json, "count");
        });
    }

    /**
     * Exports a snapshot to an archive.
     *
     * @param nameOrPath   source snapshot name or path
     * @param out          destination archive path
     * @param withParents  include parent snapshots
     * @param withImage    include image data
     * @param plainTar     use plain tar format
     */
    public static void save(String nameOrPath, String out,
                            boolean withParents, boolean withImage, boolean plainTar)
            throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        String optsJson = "{\"with_parents\":" + withParents
                + ",\"with_image\":" + withImage
                + ",\"plain_tar\":" + plainTar + "}";
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_snapshot_export(
                    cancelId, nameOrPath, out, optsJson, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Imports a snapshot from an archive.
     */
    public static String load(String archive, String dest) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_snapshot_import(
                    cancelId, archive, dest, buf, DEFAULT_BUF_SIZE);
            return bridge.checkError(err, buf);
        });
    }

    /**
     * Captures the snapshot of a stopped sandbox handle under a bare name.
     */
    public static String capture(String sandboxName, String snapshotName)
            throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_handle_snapshot(
                    cancelId, sandboxName, snapshotName, buf, DEFAULT_BUF_SIZE);
            return bridge.checkError(err, buf);
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────

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
}
