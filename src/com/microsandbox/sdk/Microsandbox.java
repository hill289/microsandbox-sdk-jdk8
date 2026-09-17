package com.microsandbox.sdk;

import com.microsandbox.sdk.exception.MicrosandboxException;
import com.microsandbox.sdk.ffi.NativeBridge;
import com.microsandbox.sdk.model.*;
import jnr.ffi.Pointer;

import java.util.ArrayList;
import java.util.List;

/**
 * Main entry point for the Microsandbox Java SDK.
 *
 * <p>JDK 8 compatible.</p>
 */
public final class Microsandbox {

    private static final int DEFAULT_BUF_SIZE = 1024 * 1024;

    private Microsandbox() {} // utility class

    // ── Setup ───────────────────────────────────────────────────────────────

    public static void ensureInstalled() throws MicrosandboxException {
        Setup.ensureInstalled();
    }

    public static boolean isInstalled() {
        return Setup.isInstalled();
    }

    // ── Sandbox CRUD ────────────────────────────────────────────────────────

    public static Sandbox createSandbox(String name, SandboxCreateOptions... options)
            throws MicrosandboxException {
        SandboxConfig config = SandboxConfig.builder().build();
        if (options != null) {
            for (SandboxCreateOptions opt : options) {
                config = opt.applyTo(config);
            }
        }
        return createSandbox(name, config);
    }

    public static Sandbox createSandbox(String name, SandboxConfig config)
            throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        String configJson = config.toJson();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);

            String err = bridge.nativeLib().msb_sandbox_create(
                    cancelId, name, configJson, 0, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            Pointer handle = extractPointer(json, "handle");     // ← Pointer
            return new Sandbox(name, handle);
        });
    }

    public static Sandbox connectOrCreateSandbox(String name, SandboxCreateOptions... options)
            throws MicrosandboxException {
        SandboxConfig config = SandboxConfig.builder().build();
        if (options != null) {
            for (SandboxCreateOptions opt : options) {
                config = opt.applyTo(config);
            }
        }

        NativeBridge bridge = NativeBridge.getInstance();
        String configJson = config.toJson();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_create(
                    cancelId, name, configJson, 1, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            Pointer handle = extractPointer(json, "handle");     // ← Pointer
            return new Sandbox(name, handle);
        });
    }

    /**
     * Returns a lightweight handle for a sandbox by name without connecting to it.
     *
     * @return a SandboxHandle with metadata and lifecycle operations
     */
    public static SandboxHandle getSandbox(String name) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_lookup(
                    cancelId, name, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return SandboxHandle.fromJson(json);
        });
    }

    /**
     * Boots a stopped sandbox by name and returns a live Sandbox.
     */
    public static Sandbox startSandbox(String name) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_start(
                    cancelId, name, 0, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            Pointer handle = extractPointer(json, "handle");
            return new Sandbox(name, handle);
        });
    }

    /**
     * Boots a stopped sandbox in detached mode. The VM keeps running after
     * the returned handle is released.
     */
    public static Sandbox startSandboxDetached(String name) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_start(
                    cancelId, name, 1, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            Pointer handle = extractPointer(json, "handle");
            return new Sandbox(name, handle);
        });
    }

    /**
     * Lists all sandboxes with their metadata.
     *
     * @return list of SandboxHandle objects
     */
    public static List<SandboxHandle> listSandboxes() throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_list(
                    cancelId, "{}", buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return SandboxHandle.parseList(json);
        });
    }

    public static void removeSandbox(String name) throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();

        bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_sandbox_remove(
                    cancelId, name, buf, DEFAULT_BUF_SIZE);
            bridge.checkError(err, buf);
            return null;
        });
    }

    // ── Version / Info ──────────────────────────────────────────────────────

    public static String version() throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        Pointer buf = bridge.allocateBuffer(4096);
        String err = bridge.nativeLib().msb_version(buf, 4096);
        String json = bridge.checkError(err, buf);
        return extractField(json, "version");
    }

    /**
     * Returns the backend selected and cached by the native SDK.
     *
     * @return a map-like JSON string containing kind, api_url, source, profile
     */
    public static String defaultBackendInfo() throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();
        Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
        String err = bridge.nativeLib().msb_default_backend_info(buf, DEFAULT_BUF_SIZE);
        return bridge.checkError(err, buf);
    }

    /**
     * Returns the current SDK version.
     */
    public static String sdkVersion() {
        return "0.6.18"; // Matches the Go SDK's sdkVersion
    }


    // ── Metrics ─────────────────────────────────────────────────────────────


    public static java.util.Map<String, Metrics> allMetrics() throws MicrosandboxException {
        NativeBridge bridge = NativeBridge.getInstance();

        return bridge.withCancel(cancelId -> {
            Pointer buf = bridge.allocateBuffer(DEFAULT_BUF_SIZE);
            String err = bridge.nativeLib().msb_all_sandbox_metrics(
                    cancelId, buf, DEFAULT_BUF_SIZE);
            String json = bridge.checkError(err, buf);
            return parseMetricsMap(json);
        });
    }

    // ── Internal JSON parsing ───────────────────────────────────────────────

    /**
     * 从 JSON 中提取一个数字字段（保留供 exit_code 等场景使用）。
     */
    static long extractLong(String json, String key) {
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

    /**
     * 从 JSON 字段中提取指针。支持十进制或 0x 十六进制字符串。
     * 例如 {"handle":"140234567890"} 或 {"handle":"0x7f8a..."}。
     */
    static Pointer extractPointer(String json, String key) {
        return NativeBridge.extractPointer(json, key);
    }

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

    private static List<SandboxInfo> parseSandboxList(String json) {
        List<SandboxInfo> result = new ArrayList<SandboxInfo>();
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
                    result.add(SandboxInfo.fromJson(obj));
                    objStart = -1;
                }
            }
        }
        return result;
    }

    private static java.util.Map<String, Metrics> parseMetricsMap(String json) {
        java.util.Map<String, Metrics> result = new java.util.LinkedHashMap<String, Metrics>();
        int idx = json.indexOf("\"sandboxes\"");
        if (idx < 0) return result;
        int objStart = json.indexOf('{', idx + 12);
        if (objStart < 0) return result;

        int depth = 0;
        String currentKey = null;
        int valueStart = -1;

        for (int i = objStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (depth == 0 && c == '"') {
                int qEnd = json.indexOf('"', i + 1);
                if (qEnd > 0) {
                    currentKey = json.substring(i + 1, qEnd);
                    i = qEnd;
                }
            } else if (c == '{') {
                if (depth == 0) valueStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && currentKey != null && valueStart >= 0) {
                    String obj = json.substring(valueStart, i + 1);
                    result.put(currentKey, Metrics.fromJson(obj));
                    currentKey = null;
                    valueStart = -1;
                }
            }
        }
        return result;
    }
}