package com.microsandbox.sdk.model;

import java.util.List;

/**
 * Network configuration for a sandbox.
 * <p>JDK 8 compatible.</p>
 */
public class NetworkConfig {

    private String preset;
    private List<String> denyDomains;
    private List<String> denyDomainSuffixes;
    private DNSConfig dns;
    private Boolean tlsIntercept;

    public NetworkConfig() {}

    // ── Getters ─────────────────────────────────────────────────────────────
    public String getPreset() { return preset; }
    public List<String> getDenyDomains() { return denyDomains; }
    public List<String> getDenyDomainSuffixes() { return denyDomainSuffixes; }
    public DNSConfig getDns() { return dns; }
    public Boolean getTlsIntercept() { return tlsIntercept; }

    // ── Setters ─────────────────────────────────────────────────────────────
    public void setPreset(String preset) { this.preset = preset; }
    public void setDenyDomains(List<String> denyDomains) { this.denyDomains = denyDomains; }
    public void setDenyDomainSuffixes(List<String> denyDomainSuffixes) { this.denyDomainSuffixes = denyDomainSuffixes; }
    public void setDns(DNSConfig dns) { this.dns = dns; }
    public void setTlsIntercept(Boolean tlsIntercept) { this.tlsIntercept = tlsIntercept; }

    // ── JSON ────────────────────────────────────────────────────────────────

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        if (preset != null) {
            sb.append("\"preset\":\"").append(preset).append('"');
            first = false;
        }
        if (denyDomains != null && !denyDomains.isEmpty()) {
            if (!first) sb.append(',');
            sb.append("\"deny_domains\":[");
            for (int i = 0; i < denyDomains.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append('"').append(denyDomains.get(i)).append('"');
            }
            sb.append(']');
            first = false;
        }
        if (denyDomainSuffixes != null && !denyDomainSuffixes.isEmpty()) {
            if (!first) sb.append(',');
            sb.append("\"deny_domain_suffixes\":[");
            for (int i = 0; i < denyDomainSuffixes.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append('"').append(denyDomainSuffixes.get(i)).append('"');
            }
            sb.append(']');
            first = false;
        }
        if (dns != null) {
            if (!first) sb.append(',');
            sb.append("\"dns\":").append(dns.toJson());
            first = false;
        }
        if (tlsIntercept != null) {
            if (!first) sb.append(',');
            sb.append("\"tls_intercept\":").append(tlsIntercept);
        }
        sb.append('}');
        return sb.toString();
    }

    // ── Static factory presets ──────────────────────────────────────────────

    public static NetworkConfig none() {
        NetworkConfig c = new NetworkConfig();
        c.preset = "none";
        return c;
    }

    public static NetworkConfig internet() {
        NetworkConfig c = new NetworkConfig();
        c.preset = "internet";
        return c;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final NetworkConfig config = new NetworkConfig();
        public Builder preset(String preset) { config.preset = preset; return this; }
        public Builder denyDomains(List<String> domains) { config.denyDomains = domains; return this; }
        public Builder denyDomainSuffixes(List<String> suffixes) { config.denyDomainSuffixes = suffixes; return this; }
        public Builder dns(DNSConfig dns) { config.dns = dns; return this; }
        public Builder tlsIntercept(Boolean intercept) { config.tlsIntercept = intercept; return this; }
        public NetworkConfig build() { return config; }
    }

    /**
     * DNS configuration.
     */
    public static class DNSConfig {
        private List<String> nameservers;

        public DNSConfig() {}
        public List<String> getNameservers() { return nameservers; }
        public void setNameservers(List<String> nameservers) { this.nameservers = nameservers; }

        public String toJson() {
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            if (nameservers != null && !nameservers.isEmpty()) {
                sb.append("\"nameservers\":[");
                for (int i = 0; i < nameservers.size(); i++) {
                    if (i > 0) sb.append(',');
                    sb.append('"').append(nameservers.get(i)).append('"');
                }
                sb.append(']');
            }
            sb.append('}');
            return sb.toString();
        }
    }
}
