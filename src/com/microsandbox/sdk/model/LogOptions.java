package com.microsandbox.sdk.model;

/**
 * Options for querying persisted sandbox logs.
 * <p>JDK 8 compatible.</p>
 */
public class LogOptions {

    private long tail;
    private String since;
    private String until;
    private String[] sources;

    public LogOptions() {}

    public long getTail() { return tail; }
    public String getSince() { return since; }
    public String getUntil() { return until; }
    public String[] getSources() { return sources; }

    public void setTail(long tail) { this.tail = tail; }
    public void setSince(String since) { this.since = since; }
    public void setUntil(String until) { this.until = until; }
    public void setSources(String[] sources) { this.sources = sources; }

    /**
     * Serializes to the JSON expected by the FFI.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;

        if (tail > 0) {
            sb.append("\"tail\":").append(tail);
            first = false;
        }
        if (since != null && !since.isEmpty()) {
            if (!first) sb.append(',');
            sb.append("\"since_ms\":").append(since);
            first = false;
        }
        if (until != null && !until.isEmpty()) {
            if (!first) sb.append(',');
            sb.append("\"until_ms\":").append(until);
            first = false;
        }
        if (sources != null && sources.length > 0) {
            if (!first) sb.append(',');
            sb.append("\"sources\":[");
            for (int i = 0; i < sources.length; i++) {
                if (i > 0) sb.append(',');
                sb.append('"').append(sources[i]).append('"');
            }
            sb.append(']');
        }
        sb.append('}');
        return sb.toString();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final LogOptions opts = new LogOptions();

        public Builder tail(long tail) { opts.tail = tail; return this; }
        public Builder since(String since) { opts.since = since; return this; }
        public Builder until(String until) { opts.until = until; return this; }
        public Builder sources(String[] sources) { opts.sources = sources; return this; }

        public LogOptions build() { return opts; }
    }
}
