package com.microsandbox.sdk.model;

import java.util.Map;

/**
 * Metadata for a volume.
 * <p>JDK 8 compatible.</p>
 */
public class VolumeInfo {

    private String name;
    private String path;
    private String kind;
    private boolean isDefault;
    private Long quotaMiB;
    private long usedBytes;
    private Long capacityBytes;
    private String diskFormat;
    private String diskFstype;
    private Map<String, String> labels;
    private Long createdAtUnix;

    public VolumeInfo() {}

    public String getName() { return name; }
    public String getPath() { return path; }
    public String getKind() { return kind; }
    public boolean isDefault() { return isDefault; }
    public Long getQuotaMiB() { return quotaMiB; }
    public long getUsedBytes() { return usedBytes; }
    public Long getCapacityBytes() { return capacityBytes; }
    public String getDiskFormat() { return diskFormat; }
    public String getDiskFstype() { return diskFstype; }
    public Map<String, String> getLabels() { return labels; }
    public Long getCreatedAtUnix() { return createdAtUnix; }

    public void setName(String v) { this.name = v; }
    public void setPath(String v) { this.path = v; }
    public void setKind(String v) { this.kind = v; }
    public void setDefault(boolean v) { this.isDefault = v; }
    public void setQuotaMiB(Long v) { this.quotaMiB = v; }
    public void setUsedBytes(long v) { this.usedBytes = v; }
    public void setCapacityBytes(Long v) { this.capacityBytes = v; }
    public void setDiskFormat(String v) { this.diskFormat = v; }
    public void setDiskFstype(String v) { this.diskFstype = v; }
    public void setLabels(Map<String, String> v) { this.labels = v; }
    public void setCreatedAtUnix(Long v) { this.createdAtUnix = v; }

    /**
     * Parses VolumeInfo from JSON.
     */
    public static VolumeInfo fromJson(String json) {
        VolumeInfo info = new VolumeInfo();
        info.name = extractField(json, "name");
        info.path = extractField(json, "path");
        info.kind = extractField(json, "kind");
        info.isDefault = json.contains("\"is_default\":true");
        info.quotaMiB = extractNullableLong(json, "quota_mib");
        info.usedBytes = extractLong(json, "used_bytes");
        info.capacityBytes = extractNullableLong(json, "capacity_bytes");
        info.diskFormat = extractField(json, "disk_format");
        info.diskFstype = extractField(json, "disk_fstype");
        info.createdAtUnix = extractNullableLong(json, "created_at_unix");
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
