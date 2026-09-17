package com.microsandbox.sdk.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Mount configuration for a sandbox.
 * <p>JDK 8 compatible.</p>
 */
public class MountConfig {

    public enum Type { NAMED, BIND, TMPFS, DISK }

    private Type type;
    private String source;
    private Map<String, String> options;

    public MountConfig() {}

    // ── Static factory methods ──────────────────────────────────────────────

    public static MountConfig named(String volumeName) {
        MountConfig m = new MountConfig();
        m.type = Type.NAMED;
        m.source = volumeName;
        return m;
    }

    public static MountConfig named(String volumeName, Map<String, String> opts) {
        MountConfig m = new MountConfig();
        m.type = Type.NAMED;
        m.source = volumeName;
        m.options = opts;
        return m;
    }

    public static MountConfig bind(String hostPath) {
        MountConfig m = new MountConfig();
        m.type = Type.BIND;
        m.source = hostPath;
        return m;
    }

    public static MountConfig tmpfs(long sizeBytes) {
        MountConfig m = new MountConfig();
        m.type = Type.TMPFS;
        m.options = new HashMap<String, String>();
        m.options.put("size", String.valueOf(sizeBytes));
        return m;
    }

    public static MountConfig disk(String imagePath) {
        MountConfig m = new MountConfig();
        m.type = Type.DISK;
        m.source = imagePath;
        return m;
    }

    // ── Getters ─────────────────────────────────────────────────────────────
    public Type getType() { return type; }
    public String getSource() { return source; }
    public Map<String, String> getOptions() { return options; }

    // ── JSON ────────────────────────────────────────────────────────────────

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"type\":\"").append(type != null ? type.name().toLowerCase() : "named").append('"');
        if (source != null) {
            sb.append(",\"source\":\"").append(source).append('"');
        }
        if (options != null && !options.isEmpty()) {
            sb.append(",\"options\":{");
            boolean first = true;
            for (Map.Entry<String, String> e : options.entrySet()) {
                if (!first) sb.append(',');
                sb.append('"').append(e.getKey()).append("\":\"").append(e.getValue()).append('"');
                first = false;
            }
            sb.append('}');
        }
        sb.append('}');
        return sb.toString();
    }
}
