package com.microsandbox.sdk.model;

import java.util.Map;

/**
 * Options for command execution inside a sandbox.
 *
 * <p>Aligned with the Go SDK's ExecOption set.</p>
 *
 * <p>JDK 8 compatible.</p>
 */
public class ExecOptions {

    private final String[] args;
    private final String cwd;
    private final long timeoutSecs;
    private final boolean tty;
    private final String user;
    private final Map<String, String> env;
    private final boolean stdinPipe;

    private ExecOptions(Builder b) {
        this.args = b.args;
        this.cwd = b.cwd;
        this.timeoutSecs = b.timeoutSecs;
        this.tty = b.tty;
        this.user = b.user;
        this.env = b.env;
        this.stdinPipe = b.stdinPipe;
    }

    public String[] getArgs() { return args; }
    public String getCwd() { return cwd; }
    public long getTimeoutSecs() { return timeoutSecs; }
    public boolean isTty() { return tty; }
    public String getUser() { return user; }
    public Map<String, String> getEnv() { return env; }
    public boolean isStdinPipe() { return stdinPipe; }

    /** Serializes to the JSON expected by the FFI. */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        if (args != null && args.length > 0) {
            sb.append("\"args\":[");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(',');
                sb.append('"').append(escapeJson(args[i])).append('"');
            }
            sb.append(']');
            first = false;
        }
        if (cwd != null && !cwd.isEmpty()) {
            if (!first) sb.append(',');
            sb.append("\"cwd\":\"").append(escapeJson(cwd)).append('"');
            first = false;
        }
        if (timeoutSecs > 0) {
            if (!first) sb.append(',');
            sb.append("\"timeout_secs\":").append(timeoutSecs);
            first = false;
        }
        if (tty) {
            if (!first) sb.append(',');
            sb.append("\"tty\":true");
            first = false;
        }
        if (user != null && !user.isEmpty()) {
            if (!first) sb.append(',');
            sb.append("\"user\":\"").append(escapeJson(user)).append('"');
            first = false;
        }
        if (env != null && !env.isEmpty()) {
            if (!first) sb.append(',');
            sb.append("\"env\":{");
            boolean firstEnv = true;
            for (Map.Entry<String, String> e : env.entrySet()) {
                if (!firstEnv) sb.append(',');
                sb.append('"').append(escapeJson(e.getKey())).append("\":\"")
                  .append(escapeJson(e.getValue())).append('"');
                firstEnv = false;
            }
            sb.append('}');
            first = false;
        }
        if (stdinPipe) {
            if (!first) sb.append(',');
            sb.append("\"stdin_pipe\":true");
        }
        sb.append('}');
        return sb.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String[] args;
        private String cwd;
        private long timeoutSecs;
        private boolean tty;
        private String user;
        private Map<String, String> env;
        private boolean stdinPipe;

        public Builder args(String[] args) { this.args = args; return this; }
        public Builder cwd(String cwd) { this.cwd = cwd; return this; }
        public Builder timeoutSecs(long timeoutSecs) { this.timeoutSecs = timeoutSecs; return this; }
        public Builder tty(boolean tty) { this.tty = tty; return this; }
        public Builder user(String user) { this.user = user; return this; }
        public Builder env(Map<String, String> env) { this.env = env; return this; }
        public Builder stdinPipe(boolean stdinPipe) { this.stdinPipe = stdinPipe; return this; }

        public ExecOptions build() {
            return new ExecOptions(this);
        }
    }

    static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
