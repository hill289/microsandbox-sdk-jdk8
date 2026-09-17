package com.microsandbox.sdk.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A single log entry from a sandbox log stream.
 * <p>JDK 8 compatible.</p>
 */
public class LogEntry {

    private String source;
    private Long sessionId;
    private Long timestampMs;
    private byte[] data;
    private String cursor;

    public LogEntry() {}

    // ── Getters ─────────────────────────────────────────────────────────────

    /** Returns the source (e.g. "stdout", "stderr", "output", "system"). */
    public String getSource() { return source; }

    /** Returns the session ID, or null if not set. */
    public Long getSessionId() { return sessionId; }

    /** Returns the timestamp in milliseconds since epoch, or null. */
    public Long getTimestampMs() { return timestampMs; }

    /** Returns the raw log payload bytes. */
    public byte[] getData() { return data; }

    /** Returns the opaque resume cursor, or null. */
    public String getCursor() { return cursor; }

    // ── Setters ─────────────────────────────────────────────────────────────

    public void setSource(String source) { this.source = source; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public void setTimestampMs(Long timestampMs) { this.timestampMs = timestampMs; }
    public void setData(byte[] data) { this.data = data; }
    public void setCursor(String cursor) { this.cursor = cursor; }

    // ── Convenience ─────────────────────────────────────────────────────────

    /** Returns the log payload as a UTF-8 string. */
    public String text() {
        return data != null ? new String(data, StandardCharsets.UTF_8) : "";
    }

    @Override
    public String toString() {
        return "[" + (source != null ? source : "?") + "] "
                + text();
    }

    /**
     * Legacy constructor for backward compatibility.
     *
     * @deprecated Use the default constructor with setters, or fromExtendedJson.
     */
    @Deprecated
    public LogEntry(String timestamp, String stream, String message) {
        this.source = stream;
        this.data = message != null ? message.getBytes(StandardCharsets.UTF_8) : new byte[0];
    }

    /**
     * Creates a LogEntry from the extended JSON format used by the FFI barrier:
     * {@code {"source":"stdout","data_b64":"...","timestamp_ms":123,"cursor":"...","session_id":1}}.
     */
    public static LogEntry fromExtendedJson(String json) {
        LogEntry entry = new LogEntry();
        entry.source = extractField(json, "source");
        if (entry.source == null) {
            entry.source = extractField(json, "stream"); // fall back to legacy
        }
        entry.timestampMs = extractNullableLong(json, "timestamp_ms");
        entry.cursor = extractField(json, "cursor");
        entry.sessionId = extractNullableLong(json, "session_id");
        String dataB64 = extractField(json, "data_b64");
        if (dataB64 != null && !dataB64.isEmpty()) {
            try {
                entry.data = Base64.getDecoder().decode(dataB64);
            } catch (Exception e) {
                entry.data = new byte[0];
            }
        } else {
            String message = extractField(json, "message");
            if (message != null) {
                entry.data = message.getBytes(StandardCharsets.UTF_8);
            } else {
                entry.data = new byte[0];
            }
        }
        return entry;
    }

    /**
     * Creates a LogEntry from the legacy JSON format.
     * @deprecated Use {@link #fromExtendedJson(String)} instead.
     */
    @Deprecated
    public static LogEntry fromJson(String json) {
        return fromExtendedJson(json);
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
