package com.microsandbox.sdk.model;

/**
 * Options for a live log stream.
 * <p>JDK 8 compatible.</p>
 */
public class LogStreamOptions {

    private String[] sources;
    private String since;
    private String fromCursor;
    private String until;
    private boolean follow;

    public LogStreamOptions() {}

    public String[] getSources() { return sources; }
    public String getSince() { return since; }
    public String getFromCursor() { return fromCursor; }
    public String getUntil() { return until; }
    public boolean isFollow() { return follow; }

    public void setSources(String[] sources) { this.sources = sources; }
    public void setSince(String since) { this.since = since; }
    public void setFromCursor(String fromCursor) { this.fromCursor = fromCursor; }
    public void setUntil(String until) { this.until = until; }
    public void setFollow(boolean follow) { this.follow = follow; }

    /**
     * Serializes to the JSON expected by the FFI.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;

        if (sources != null && sources.length > 0) {
            sb.append("\"sources\":[");
            for (int i = 0; i < sources.length; i++) {
                if (i > 0) sb.append(',');
                sb.append('"').append(sources[i]).append('"');
            }
            sb.append(']');
            first = false;
        }
        if (since != null && !since.isEmpty()) {
            if (!first) sb.append(',');
            sb.append("\"since_ms\":").append(since);
            first = false;
        }
        if (fromCursor != null && !fromCursor.isEmpty()) {
            if (!first) sb.append(',');
            sb.append("\"from_cursor\":\"").append(fromCursor).append('"');
            first = false;
        }
        if (until != null && !until.isEmpty()) {
            if (!first) sb.append(',');
            sb.append("\"until_ms\":").append(until);
            first = false;
        }
        if (follow) {
            if (!first) sb.append(',');
            sb.append("\"follow\":true");
        }
        sb.append('}');
        return sb.toString();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final LogStreamOptions opts = new LogStreamOptions();

        public Builder sources(String[] sources) { opts.sources = sources; return this; }
        public Builder since(String since) { opts.since = since; return this; }
        public Builder fromCursor(String cursor) { opts.fromCursor = cursor; return this; }
        public Builder until(String until) { opts.until = until; return this; }
        public Builder follow(boolean follow) { opts.follow = follow; return this; }

        public LogStreamOptions build() { return opts; }
    }
}
