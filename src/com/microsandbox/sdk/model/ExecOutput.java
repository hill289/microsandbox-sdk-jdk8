package com.microsandbox.sdk.model;

import java.util.Arrays;

/**
 * The collected result of a command execution inside a sandbox.
 *
 * <p>A non-zero {@link #getExitCode() exit code} is NOT treated as an exception;
 * callers inspect {@link #isSuccess()} or the exit code explicitly, matching
 * how {@code os/exec.Cmd.Output} works.</p>
 *
 * <p>JDK 8 compatible.</p>
 */
public class ExecOutput {

    private final byte[] stdout;
    private final byte[] stderr;
    private final int exitCode;

    private ExecOutput(byte[] stdout, byte[] stderr, int exitCode) {
        this.stdout = stdout != null ? stdout : new byte[0];
        this.stderr = stderr != null ? stderr : new byte[0];
        this.exitCode = exitCode;
    }

    /** Returns captured standard output as a string (UTF-8). */
    public String getStdout() {
        return new String(stdout, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Returns captured standard output as raw bytes. */
    public byte[] getStdoutBytes() {
        return Arrays.copyOf(stdout, stdout.length);
    }

    /** Returns captured standard error as a string (UTF-8). */
    public String getStderr() {
        return new String(stderr, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Returns captured standard error as raw bytes. */
    public byte[] getStderrBytes() {
        return Arrays.copyOf(stderr, stderr.length);
    }

    /** Returns the process exit code, or -1 if not reported. */
    public int getExitCode() {
        return exitCode;
    }

    /** Returns true if the command exited with code 0. */
    public boolean isSuccess() {
        return exitCode == 0;
    }

    @Override
    public String toString() {
        return "ExecOutput{exitCode=" + exitCode
                + ", stdout_len=" + stdout.length
                + ", stderr_len=" + stderr.length + "}";
    }

    // ── Builder ─────────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private byte[] stdout;
        private byte[] stderr;
        private int exitCode = -1;

        public Builder stdout(byte[] stdout) {
            this.stdout = stdout;
            return this;
        }

        public Builder stdout(String stdout) {
            this.stdout = stdout != null
                    ? stdout.getBytes(java.nio.charset.StandardCharsets.UTF_8)
                    : new byte[0];
            return this;
        }

        public Builder stderr(byte[] stderr) {
            this.stderr = stderr;
            return this;
        }

        public Builder stderr(String stderr) {
            this.stderr = stderr != null
                    ? stderr.getBytes(java.nio.charset.StandardCharsets.UTF_8)
                    : new byte[0];
            return this;
        }

        public Builder exitCode(int exitCode) {
            this.exitCode = exitCode;
            return this;
        }

        public ExecOutput build() {
            return new ExecOutput(stdout, stderr, exitCode);
        }
    }

    // ── Factory from JSON ───────────────────────────────────────────────────

    /**
     * Creates an ExecOutput from the JSON returned by {@code msb_exec_collect}
     * or similar: {@code {"stdout_b64":"…","stderr_b64":"…","exit_code":N}}.
     */
    public static ExecOutput fromJson(String json) {
        byte[] stdoutBytes = extractOutputBytes(json, "stdout_b64", "stdout");
        byte[] stderrBytes = extractOutputBytes(json, "stderr_b64", "stderr");
        int exitCode = extractIntField(json, "exit_code");

        return new ExecOutput(stdoutBytes, stderrBytes, exitCode);
    }

    /**
     * Reads captured output, preferring the base64 field when present and
     * falling back to the plain-text field (the native SDK emits
     * {@code "stdout"}/{@code "stderr"} as UTF-8 strings).
     */
    private static byte[] extractOutputBytes(String json, String b64Key, String plainKey) {
        String b64 = extractField(json, b64Key);
        if (b64 != null && !b64.isEmpty()) {
            try {
                return java.util.Base64.getDecoder().decode(b64);
            } catch (Exception e) {
                // Fall through to the plain-text field.
            }
        }
        String plain = extractField(json, plainKey);
        if (plain != null) {
            return plain.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        return new byte[0];
    }

    // ── Minimal JSON parsing helpers ────────────────────────────────────────

    private static String extractField(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int qStart = json.indexOf('"', colon + 1);
        if (qStart < 0) return null;
        StringBuilder sb = new StringBuilder();
        int i = qStart + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char n = json.charAt(i + 1);
                switch (n) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    default: sb.append(n); break;
                }
                i += 2;
                continue;
            }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private static int extractIntField(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return -1;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return -1;
        int start = colon + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;
        if (start >= json.length()) return -1;
        // Handle null
        if (json.startsWith("null", start)) return -1;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        if (end == start) return -1;
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static byte[] base64Decode(String b64) {
        try {
            return java.util.Base64.getDecoder().decode(b64);
        } catch (Exception e) {
            return new byte[0];
        }
    }
}
