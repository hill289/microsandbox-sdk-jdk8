package com.microsandbox.sdk.model;

import java.util.Map;

/**
 * Metadata for a cached OCI image.
 * <p>JDK 8 compatible.</p>
 */
public class ImageInfo {

    private String reference;
    private String manifestDigest;
    private String architecture;
    private String os;
    private long layerCount;
    private Long sizeBytes;
    private Long createdAtUnix;
    private Long lastUsedAtUnix;

    public ImageInfo() {}

    public String getReference() { return reference; }
    public String getManifestDigest() { return manifestDigest; }
    public String getArchitecture() { return architecture; }
    public String getOs() { return os; }
    public long getLayerCount() { return layerCount; }
    public Long getSizeBytes() { return sizeBytes; }
    public Long getCreatedAtUnix() { return createdAtUnix; }
    public Long getLastUsedAtUnix() { return lastUsedAtUnix; }

    public void setReference(String r) { this.reference = r; }
    public void setManifestDigest(String d) { this.manifestDigest = d; }
    public void setArchitecture(String a) { this.architecture = a; }
    public void setOs(String o) { this.os = o; }
    public void setLayerCount(long c) { this.layerCount = c; }
    public void setSizeBytes(Long s) { this.sizeBytes = s; }
    public void setCreatedAtUnix(Long t) { this.createdAtUnix = t; }
    public void setLastUsedAtUnix(Long t) { this.lastUsedAtUnix = t; }

    /**
     * Parses ImageInfo from JSON.
     */
    public static ImageInfo fromJson(String json) {
        ImageInfo info = new ImageInfo();
        info.reference = extractField(json, "reference");
        info.manifestDigest = extractField(json, "manifest_digest");
        info.architecture = extractField(json, "architecture");
        info.os = extractField(json, "os");
        info.layerCount = extractLong(json, "layer_count");
        info.sizeBytes = extractNullableLong(json, "size_bytes");
        info.createdAtUnix = extractNullableLong(json, "created_at_unix");
        info.lastUsedAtUnix = extractNullableLong(json, "last_used_at_unix");
        return info;
    }

    // ── JSON helpers ──────────────────────────────────────────────────────

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

    private static Long extractNullableLong(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return null;
        if (json.startsWith("null", start)) return null;
        int end = start;
        while (end < json.length()
                && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        if (end == start) return null;
        try { return Long.valueOf(json.substring(start, end)); }
        catch (NumberFormatException e) { return null; }
    }
}
