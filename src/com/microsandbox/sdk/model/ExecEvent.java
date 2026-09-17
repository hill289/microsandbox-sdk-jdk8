package com.microsandbox.sdk.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * An event from a streaming exec session.
 * <p>JDK 8 compatible.</p>
 */
public class ExecEvent {

    /**
     * Event kind, aligned with the Go SDK's ExecEventKind values.
     */
    public enum Kind {
        STARTED("started"),
        STDOUT("stdout"),
        STDERR("stderr"),
        EXITED("exited"),
        FAILED("failed"),
        STDIN_ERROR("stdin_error"),
        DONE("done");

        private final String wireValue;

        Kind(String wireValue) { this.wireValue = wireValue; }

        public String wireValue() { return wireValue; }

        public static Kind fromWireValue(String value) {
            if (value == null) return DONE;
            for (Kind k : values()) {
                if (k.wireValue.equals(value)) return k;
            }
            return DONE;
        }
    }

    private Kind kind;
    private long pid;
    private byte[] data;
    private int exitCode;
    private String failure;

    public ExecEvent() {}

    public ExecEvent(Kind kind, byte[] data) {
        this.kind = kind;
        this.data = data;
    }

    public Kind getKind() { return kind; }
    public long getPid() { return pid; }
    public byte[] getData() { return data; }
    public int getExitCode() { return exitCode; }
    public String getFailure() { return failure; }

    public void setKind(Kind kind) { this.kind = kind; }
    public void setPid(long pid) { this.pid = pid; }
    public void setData(byte[] data) { this.data = data; }
    public void setExitCode(int exitCode) { this.exitCode = exitCode; }
    public void setFailure(String failure) { this.failure = failure; }

    /**
     * Returns true if this is a terminal event (DONE or EXITED or FAILED).
     */
    public boolean isTerminal() {
        return kind == Kind.DONE || kind == Kind.EXITED || kind == Kind.FAILED;
    }

    /**
     * Parses an event from a JSON line returned by {@code msb_exec_recv}.
     * Expected formats:
     * <ul>
     *   <li>{@code {"event":"started","pid":N}}</li>
     *   <li>{@code {"event":"stdout","data_b64":"..."}}</li>
     *   <li>{@code {"event":"stderr","data_b64":"..."}}</li>
     *   <li>{@code {"event":"exited","exit_code":N}}</li>
     *   <li>{@code {"event":"failed","failure":"..."}}</li>
     *   <li>{@code {"event":"stdin_error","failure":"..."}}</li>
     *   <li>{@code {"event":"done"}}</li>
     * </ul>
     */
    public static ExecEvent fromJson(String json) {
        ExecEvent event = new ExecEvent();
        String eventKind = extractField(json, "event");
        if (eventKind == null) eventKind = "";

        switch (eventKind) {
            case "started":
                event.kind = Kind.STARTED;
                event.pid = extractLong(json, "pid");
                break;
            case "stdout":
                event.kind = Kind.STDOUT;
                event.data = decodeB64(extractField(json, "data"));
                break;
            case "stderr":
                event.kind = Kind.STDERR;
                event.data = decodeB64(extractField(json, "data"));
                break;
            case "exited":
                event.kind = Kind.EXITED;
                event.exitCode = extractInt(json, "code");
                break;
            case "failed":
                event.kind = Kind.FAILED;
                event.failure = extractField(json, "failure");
                if (event.failure == null) event.failure = extractField(json, "message");
                break;
            case "stdin_error":
                event.kind = Kind.STDIN_ERROR;
                event.failure = extractField(json, "failure");
                if (event.failure == null) event.failure = extractField(json, "message");
                break;
            case "done":
            default:
                event.kind = Kind.DONE;
                break;
        }
        return event;
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

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
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        if (end == start) return -1;
        try { return Integer.parseInt(json.substring(start, end)); }
        catch (NumberFormatException e) { return -1; }
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
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        if (end == start) return 0;
        try { return Long.parseLong(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0; }
    }

    private static byte[] decodeB64(String b64) {
        if (b64 == null || b64.isEmpty()) return new byte[0];
        try { return Base64.getDecoder().decode(b64); }
        catch (Exception e) { return new byte[0]; }
    }
}
