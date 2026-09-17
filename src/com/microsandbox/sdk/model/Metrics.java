package com.microsandbox.sdk.model;

/**
 * Sandbox metrics snapshot.
 * <p>JDK 8 compatible.</p>
 */
public class Metrics {

    private double cpuPercent;
    private long vcpuTimeNs;
    private long memoryBytes;
    private Long memoryAvailableBytes;
    private Long memoryHostResidentBytes;
    private long memoryLimitBytes;
    private long diskReadBytes;
    private long diskWriteBytes;
    private long netRxBytes;
    private long netTxBytes;
    private Long upperUsedBytes;
    private Long upperFreeBytes;
    private Long upperHostAllocatedBytes;
    private long uptime;

    public Metrics() {}

    // ── Getters ─────────────────────────────────────────────────────────────

    public double getCpuPercent() { return cpuPercent; }
    public long getVcpuTimeNs() { return vcpuTimeNs; }
    public long getMemoryBytes() { return memoryBytes; }
    public Long getMemoryAvailableBytes() { return memoryAvailableBytes; }
    public Long getMemoryHostResidentBytes() { return memoryHostResidentBytes; }
    public long getMemoryLimitBytes() { return memoryLimitBytes; }
    public long getDiskReadBytes() { return diskReadBytes; }
    public long getDiskWriteBytes() { return diskWriteBytes; }
    public long getNetRxBytes() { return netRxBytes; }
    public long getNetTxBytes() { return netTxBytes; }
    public Long getUpperUsedBytes() { return upperUsedBytes; }
    public Long getUpperFreeBytes() { return upperFreeBytes; }
    public Long getUpperHostAllocatedBytes() { return upperHostAllocatedBytes; }
    public long getUptime() { return uptime; }

    public double getMemoryMiB() { return (double) memoryBytes / (1024 * 1024); }

    // ── Setters ─────────────────────────────────────────────────────────────

    public void setCpuPercent(double v) { this.cpuPercent = v; }
    public void setVcpuTimeNs(long v) { this.vcpuTimeNs = v; }
    public void setMemoryBytes(long v) { this.memoryBytes = v; }
    public void setMemoryAvailableBytes(Long v) { this.memoryAvailableBytes = v; }
    public void setMemoryHostResidentBytes(Long v) { this.memoryHostResidentBytes = v; }
    public void setMemoryLimitBytes(long v) { this.memoryLimitBytes = v; }
    public void setDiskReadBytes(long v) { this.diskReadBytes = v; }
    public void setDiskWriteBytes(long v) { this.diskWriteBytes = v; }
    public void setNetRxBytes(long v) { this.netRxBytes = v; }
    public void setNetTxBytes(long v) { this.netTxBytes = v; }
    public void setUpperUsedBytes(Long v) { this.upperUsedBytes = v; }
    public void setUpperFreeBytes(Long v) { this.upperFreeBytes = v; }
    public void setUpperHostAllocatedBytes(Long v) { this.upperHostAllocatedBytes = v; }
    public void setUptime(long v) { this.uptime = v; }

    @Override
    public String toString() {
        return String.format("Metrics{cpu=%.1f%%, mem=%.1f MiB}", cpuPercent, getMemoryMiB());
    }

    /**
     * Creates Metrics from JSON: parses all known metric fields.
     */
    public static Metrics fromJson(String json) {
        Metrics m = new Metrics();
        m.cpuPercent = extractDouble(json, "cpu_percent");
        m.vcpuTimeNs = extractLong(json, "vcpu_time_ns");
        m.memoryBytes = extractLong(json, "memory_bytes");
        m.memoryAvailableBytes = extractNullableLong(json, "memory_available_bytes");
        m.memoryHostResidentBytes = extractNullableLong(json, "memory_host_resident_bytes");
        m.memoryLimitBytes = extractLong(json, "memory_limit_bytes");
        m.diskReadBytes = extractLong(json, "disk_read_bytes");
        m.diskWriteBytes = extractLong(json, "disk_write_bytes");
        m.netRxBytes = extractLong(json, "net_rx_bytes");
        m.netTxBytes = extractLong(json, "net_tx_bytes");
        m.upperUsedBytes = extractNullableLong(json, "upper_used_bytes");
        m.upperFreeBytes = extractNullableLong(json, "upper_free_bytes");
        m.upperHostAllocatedBytes = extractNullableLong(json, "upper_host_allocated_bytes");
        m.uptime = extractLong(json, "uptime");
        return m;
    }

    // ── JSON helpers ──────────────────────────────────────────────────────

    private static double extractDouble(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return 0;
        int colon = json.indexOf(':', idx + key.length() + 2);
        if (colon < 0) return 0;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '.' || json.charAt(end) == '-' || json.charAt(end) == 'e' || json.charAt(end) == 'E' || json.charAt(end) == '+')) {
            end++;
        }
        if (end == start) return 0;
        try {
            return Double.parseDouble(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static long extractLong(String json, String key) {
        Long val = extractNullableLong(json, key);
        return val != null ? val : 0;
    }

    private static Long extractNullableLong(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + key.length() + 2);
        if (colon < 0) return null;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return null;
        if (json.startsWith("null", start)) return null;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        if (end == start) return null;
        try {
            return Long.valueOf(json.substring(start, end));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
