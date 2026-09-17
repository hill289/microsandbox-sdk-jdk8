package com.microsandbox.sdk;

import com.microsandbox.sdk.exception.MicrosandboxException;
import com.microsandbox.sdk.ffi.NativeBridge;
import com.microsandbox.sdk.model.VolumeInfo;
import jnr.ffi.Pointer;

import java.util.ArrayList;
import java.util.List;

/**
 * Volume management operations for the microsandbox SDK.
 *
 * <p>JDK 8 compatible.</p>
 */
public final class Volume {

    private static final int DEFAULT_BUF_SIZE = 1024 * 1024;

    private Volume() {} // utility class

    // ── Create options ────────────────────────────────────────────────────

    /**
     * Options for creating a volume.
     */
    public static class CreateOptions {
        private Long quotaMiB;
        private String kind;
        private Long sizeMiB;
        private java.util.Map<String, String> labels;

        public CreateOptions quotaMiB(long b) { this.quotaMiB = b; return this; }
        public CreateOptions kind(String k) { this.kind = k; return this; }
        public CreateOptions sizeMiB(long s) { this.sizeMiB = s; return this; }
        public CreateOptions labels(java.util.Map<String, String> l) { this.labels = l; return this; }

        String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            if (quotaMiB != null && quotaMiB > 0) {
                sb.append("\"quota_mib\":").append(quotaMiB);
                first = false;
            }
            if (kind != null && !kind.isEmpty()) {
                if (!first) sb.append(',');
                sb.append("\"kind\":\"").append(kind).append('"');
                first = false;
            }
            if (sizeMiB != null && sizeMiB > 0) {
                if (!first) sb.append(',');
                sb.append("\"size_mib\":").append(sizeMiB);
                first = false;
            }
            if (labels != null && !labels.isEmpty()) {
                if (!first) sb.append(',');
                sb.append("\"labels\":{");
                boolean fe = true;
                for (java.util.Map.Entry<String, String> e : labels.entrySet()) {
                    if (!fe) sb.append(',');
                    sb.append('"').append(e.getKey().replace("\\", "\\\\")
                            .replace("\"", "\\\"")).append("\":\"")
                      .append(e.getValue().replace("\\", "\\\\")
                            .replace("\"", "\\\"")).append('"');
                    fe = false;
                }
                sb.append('}');
            }
            sb.append('}');
            return sb.toString();
        }
    }

    // ── Operations ────────────────────────────────────────────────────────

    /**
     * Creates a named volume and returns its metadata.
     */
    public static VolumeInfo create(String name, CreateOptions options)
            throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        String optsJson = options != null ? options.toJson() : "{}";
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_volume_create(
                    cancelId, name, optsJson, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return VolumeInfo.fromJson(json);
        });
    }

    /**
     * Creates a named volume with default options.
     */
    public static VolumeInfo create(String name) throws MicrosandboxException {
        return create(name, null);
    }

    /**
     * Lists metadata for every named volume on the host.
     */
    public static List<VolumeInfo> list() throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_volume_list(
                    cancelId, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return parseVolumeList(json);
        });
    }

    /**
     * Removes a volume by name.
     */
    public static void remove(String name) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_volume_remove(
                    cancelId, name, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    /**
     * Looks up a volume by name and returns its metadata.
     */
    public static VolumeInfo get(String name) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_volume_get(
                    cancelId, name, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return VolumeInfo.fromJson(json);
        });
    }

    /**
     * Returns the cloud account's always-present default volume.
     */
    public static VolumeInfo getDefault() throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_volume_get_default(
                    cancelId, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return VolumeInfo.fromJson(json);
        });
    }

    /**
     * Performs a filesystem operation directly on a volume (for cloud volumes
     * that do not expose a host path).
     *
     * @param target  the volume target (name or "cloud-id:...")
     * @param op      the operation ("read", "write", "mkdir", "remove", "exists")
     * @param argsJson JSON arguments for the operation
     * @return the operation result JSON
     */
    public static String fsOp(String target, String op, String argsJson)
            throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_volume_fs_op(
                    cancelId, target, op,
                    argsJson != null ? argsJson : "{}",
                    buf, DEFAULT_BUF_SIZE);
            return bridge.checkError(err, buf);
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static List<VolumeInfo> parseVolumeList(String json) {
        List<VolumeInfo> result = new ArrayList<VolumeInfo>();
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
                    result.add(VolumeInfo.fromJson(obj));
                    objStart = -1;
                }
            }
        }
        return result;
    }
}
