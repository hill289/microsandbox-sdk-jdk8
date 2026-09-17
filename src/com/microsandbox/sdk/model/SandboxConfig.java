package com.microsandbox.sdk.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for creating a new sandbox.
 * <p>JDK 8 compatible.</p>
 *
 * <p>Aligned with the Go SDK's SandboxConfig fields.</p>
 */
public class SandboxConfig {

    private String image;
    private int memory;        // MiB
    private int cpus;
    private boolean replace;
    private boolean detached;
    private NetworkConfig network;
    private Map<String, MountConfig> mounts;
    private Map<Integer, Integer> ports;
    private Map<String, String> env;
    private String timeout;
    private Long maxMemory;       // MiB
    private Integer maxCpus;
    private String workdir;
    private String shell;
    private String[] entrypoint;
    private String[] cmd;
    private String pullPolicy;
    private String securityProfile;
    private boolean ephemeral;
    private String user;
    private String hostname;
    private Map<String, String> labels;
    private Map<Integer, Integer> portsUdp;

    public SandboxConfig() {}

    // ── Getters ─────────────────────────────────────────────────────────────

    public String getImage() { return image; }
    public int getMemory() { return memory; }
    public int getCpus() { return cpus; }
    public boolean isReplace() { return replace; }
    public boolean isDetached() { return detached; }
    public NetworkConfig getNetwork() { return network; }
    public Map<String, MountConfig> getMounts() { return mounts; }
    public Map<Integer, Integer> getPorts() { return ports; }
    public Map<String, String> getEnv() { return env; }
    public String getTimeout() { return timeout; }
    public Long getMaxMemory() { return maxMemory; }
    public Integer getMaxCpus() { return maxCpus; }
    public String getWorkdir() { return workdir; }
    public String getShell() { return shell; }
    public String[] getEntrypoint() { return entrypoint; }
    public String[] getCmd() { return cmd; }
    public String getPullPolicy() { return pullPolicy; }
    public String getSecurityProfile() { return securityProfile; }
    public boolean isEphemeral() { return ephemeral; }
    public String getUser() { return user; }
    public String getHostname() { return hostname; }
    public Map<String, String> getLabels() { return labels; }
    public Map<Integer, Integer> getPortsUdp() { return portsUdp; }

    // ── Setters ─────────────────────────────────────────────────────────────

    public void setImage(String image) { this.image = image; }
    public void setMemory(int memory) { this.memory = memory; }
    public void setCpus(int cpus) { this.cpus = cpus; }
    public void setReplace(boolean replace) { this.replace = replace; }
    public void setDetached(boolean detached) { this.detached = detached; }
    public void setNetwork(NetworkConfig network) { this.network = network; }
    public void setMounts(Map<String, MountConfig> mounts) { this.mounts = mounts; }
    public void setPorts(Map<Integer, Integer> ports) { this.ports = ports; }
    public void setEnv(Map<String, String> env) { this.env = env; }
    public void setTimeout(String timeout) { this.timeout = timeout; }
    public void setMaxMemory(Long maxMemory) { this.maxMemory = maxMemory; }
    public void setMaxCpus(Integer maxCpus) { this.maxCpus = maxCpus; }
    public void setWorkdir(String workdir) { this.workdir = workdir; }
    public void setShell(String shell) { this.shell = shell; }
    public void setEntrypoint(String[] entrypoint) { this.entrypoint = entrypoint; }
    public void setCmd(String[] cmd) { this.cmd = cmd; }
    public void setPullPolicy(String pullPolicy) { this.pullPolicy = pullPolicy; }
    public void setSecurityProfile(String securityProfile) { this.securityProfile = securityProfile; }
    public void setEphemeral(boolean ephemeral) { this.ephemeral = ephemeral; }
    public void setUser(String user) { this.user = user; }
    public void setHostname(String hostname) { this.hostname = hostname; }
    public void setLabels(Map<String, String> labels) { this.labels = labels; }
    public void setPortsUdp(Map<Integer, Integer> portsUdp) { this.portsUdp = portsUdp; }

    // ── JSON serialization ──────────────────────────────────────────────────

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        first = appendJsonField(sb, first, "image", image);
        if (memory > 0) { if (!first) sb.append(','); sb.append("\"memory\":").append(memory); first = false; }
        if (cpus > 0) { if (!first) sb.append(','); sb.append("\"cpus\":").append(cpus); first = false; }
        if (replace) { if (!first) sb.append(','); sb.append("\"replace\":true"); first = false; }
        if (detached) { if (!first) sb.append(','); sb.append("\"detached\":true"); first = false; }
        if (ephemeral) { if (!first) sb.append(','); sb.append("\"ephemeral\":true"); first = false; }
        if (network != null) { if (!first) sb.append(','); sb.append("\"network\":").append(network.toJson()); first = false; }
        if (mounts != null && !mounts.isEmpty()) { if (!first) sb.append(','); sb.append("\"mounts\":").append(mountsToJson()); first = false; }
        if (ports != null && !ports.isEmpty()) {
            if (!first) sb.append(',');
            sb.append("\"ports\":{");
            boolean fe = true;
            for (Map.Entry<Integer, Integer> e : ports.entrySet()) {
                if (!fe) sb.append(',');
                sb.append('"').append(e.getKey()).append("\":").append(e.getValue());
                fe = false;
            }
            sb.append('}');
            first = false;
        }
        if (portsUdp != null && !portsUdp.isEmpty()) {
            if (!first) sb.append(',');
            sb.append("\"ports_udp\":{");
            boolean fe = true;
            for (Map.Entry<Integer, Integer> e : portsUdp.entrySet()) {
                if (!fe) sb.append(',');
                sb.append('"').append(e.getKey()).append("\":").append(e.getValue());
                fe = false;
            }
            sb.append('}');
            first = false;
        }
        if (env != null && !env.isEmpty()) {
            if (!first) sb.append(',');
            sb.append("\"env\":{");
            boolean fe = true;
            for (Map.Entry<String, String> e : env.entrySet()) {
                if (!fe) sb.append(',');
                sb.append('"').append(escapeJson(e.getKey())).append("\":\"")
                  .append(escapeJson(e.getValue())).append('"');
                fe = false;
            }
            sb.append('}');
            first = false;
        }
        if (labels != null && !labels.isEmpty()) {
            if (!first) sb.append(',');
            sb.append("\"labels\":{");
            boolean fe = true;
            for (Map.Entry<String, String> e : labels.entrySet()) {
                if (!fe) sb.append(',');
                sb.append('"').append(escapeJson(e.getKey())).append("\":\"")
                  .append(escapeJson(e.getValue())).append('"');
                fe = false;
            }
            sb.append('}');
            first = false;
        }
        if (timeout != null) { if (!first) sb.append(','); sb.append("\"timeout\":\"").append(escapeJson(timeout)).append('"'); first = false; }
        if (maxMemory != null) { if (!first) sb.append(','); sb.append("\"max_memory_mib\":").append(maxMemory); first = false; }
        if (maxCpus != null) { if (!first) sb.append(','); sb.append("\"max_cpus\":").append(maxCpus); first = false; }
        if (workdir != null) { if (!first) sb.append(','); sb.append("\"workdir\":\"").append(escapeJson(workdir)).append('"'); first = false; }
        if (shell != null) { if (!first) sb.append(','); sb.append("\"shell\":\"").append(escapeJson(shell)).append('"'); first = false; }
        if (entrypoint != null && entrypoint.length > 0) {
            if (!first) sb.append(',');
            sb.append("\"entrypoint\":[");
            for (int i = 0; i < entrypoint.length; i++) {
                if (i > 0) sb.append(',');
                sb.append('"').append(escapeJson(entrypoint[i])).append('"');
            }
            sb.append(']');
            first = false;
        }
        if (cmd != null && cmd.length > 0) {
            if (!first) sb.append(',');
            sb.append("\"cmd\":[");
            for (int i = 0; i < cmd.length; i++) {
                if (i > 0) sb.append(',');
                sb.append('"').append(escapeJson(cmd[i])).append('"');
            }
            sb.append(']');
            first = false;
        }
        if (pullPolicy != null) { if (!first) sb.append(','); sb.append("\"pull_policy\":\"").append(escapeJson(pullPolicy)).append('"'); first = false; }
        if (securityProfile != null) { if (!first) sb.append(','); sb.append("\"security_profile\":\"").append(escapeJson(securityProfile)).append('"'); first = false; }
        if (user != null) { if (!first) sb.append(','); sb.append("\"user\":\"").append(escapeJson(user)).append('"'); first = false; }
        if (hostname != null) { if (!first) sb.append(','); sb.append("\"hostname\":\"").append(escapeJson(hostname)).append('"'); }
        sb.append('}');
        return sb.toString();
    }

    private boolean appendJsonField(StringBuilder sb, boolean first, String key, String value) {
        if (value != null && !value.isEmpty()) {
            if (!first) sb.append(',');
            sb.append('"').append(key).append("\":\"").append(escapeJson(value)).append('"');
            return false;
        }
        return first;
    }

    private String mountsToJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, MountConfig> e : mounts.entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(escapeJson(e.getKey())).append("\":").append(e.getValue().toJson());
            first = false;
        }
        sb.append('}');
        return sb.toString();
    }

    static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    // ── Builder ─────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final SandboxConfig config = new SandboxConfig();

        public Builder image(String image) { config.image = image; return this; }
        public Builder memory(int mb) { config.memory = mb; return this; }
        public Builder cpus(int cpus) { config.cpus = cpus; return this; }
        public Builder replace(boolean replace) { config.replace = replace; return this; }
        public Builder detached(boolean detached) { config.detached = detached; return this; }
        public Builder ephemeral(boolean ephemeral) { config.ephemeral = ephemeral; return this; }
        public Builder network(NetworkConfig network) { config.network = network; return this; }
        public Builder mounts(Map<String, MountConfig> mounts) { config.mounts = mounts; return this; }
        public Builder ports(Map<Integer, Integer> ports) { config.ports = ports; return this; }
        public Builder env(String key, String value) {
            if (config.env == null) config.env = new HashMap<String, String>();
            config.env.put(key, value);
            return this;
        }
        public Builder env(Map<String, String> env) { config.env = env; return this; }
        public Builder timeout(String timeout) { config.timeout = timeout; return this; }
        public Builder maxMemory(Long mb) { config.maxMemory = mb; return this; }
        public Builder maxCpus(Integer cpus) { config.maxCpus = cpus; return this; }
        public Builder workdir(String workdir) { config.workdir = workdir; return this; }
        public Builder shell(String shell) { config.shell = shell; return this; }
        public Builder entrypoint(String[] entrypoint) { config.entrypoint = entrypoint; return this; }
        public Builder cmd(String[] cmd) { config.cmd = cmd; return this; }
        public Builder pullPolicy(String policy) { config.pullPolicy = policy; return this; }
        public Builder securityProfile(String profile) { config.securityProfile = profile; return this; }
        public Builder user(String user) { config.user = user; return this; }
        public Builder hostname(String hostname) { config.hostname = hostname; return this; }
        public Builder labels(Map<String, String> labels) { config.labels = labels; return this; }
        public Builder portsUdp(Map<Integer, Integer> portsUdp) { config.portsUdp = portsUdp; return this; }

        public SandboxConfig build() { return config; }
    }
}
