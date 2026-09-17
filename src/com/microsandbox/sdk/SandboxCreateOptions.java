package com.microsandbox.sdk;

import com.microsandbox.sdk.model.MountConfig;
import com.microsandbox.sdk.model.NetworkConfig;
import com.microsandbox.sdk.model.SandboxConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Options for creating a sandbox. Builder pattern for fluent API.
 *
 * <p>Usage:
 * <pre>{@code
 * Sandbox sb = Microsandbox.createSandbox("my-sandbox",
 *     SandboxCreateOptions.builder()
 *         .image("alpine:3.19")
 *         .memory(512)
 *         .cpus(1)
 *         .replace(true)
 *         .env("MY_VAR", "value")
 *         .build());
 * }</pre>
 *
 * <p>JDK 8 compatible.</p>
 */
public class SandboxCreateOptions {

    private String image;
    private int memory;
    private int cpus;
    private boolean replace;
    private boolean detached;
    private NetworkConfig network;
    private Map<String, MountConfig> mounts;
    private Map<Integer, Integer> ports;
    private Map<String, String> env;
    private Long maxMemory;
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

    SandboxCreateOptions() {}

    /**
     * Applies these options to a base config, returning the merged config.
     */
    public SandboxConfig applyTo(SandboxConfig base) {
        SandboxConfig.Builder b = SandboxConfig.builder();
        if (base != null) {
            if (base.getImage() != null) b.image(base.getImage());
            if (base.getMemory() > 0) b.memory(base.getMemory());
            if (base.getCpus() > 0) b.cpus(base.getCpus());
            if (base.isReplace()) b.replace(true);
            if (base.isDetached()) b.detached(true);
            if (base.getNetwork() != null) b.network(base.getNetwork());
            if (base.getMounts() != null) b.mounts(base.getMounts());
            if (base.getPorts() != null) b.ports(base.getPorts());
            if (base.getEnv() != null) b.env(base.getEnv());
            if (base.getMaxMemory() != null) b.maxMemory(base.getMaxMemory());
            if (base.getMaxCpus() != null) b.maxCpus(base.getMaxCpus());
            if (base.getWorkdir() != null) b.workdir(base.getWorkdir());
            if (base.getShell() != null) b.shell(base.getShell());
            if (base.getEntrypoint() != null) b.entrypoint(base.getEntrypoint());
            if (base.getCmd() != null) b.cmd(base.getCmd());
            if (base.getPullPolicy() != null) b.pullPolicy(base.getPullPolicy());
            if (base.isEphemeral()) b.ephemeral(true);
            if (base.getUser() != null) b.user(base.getUser());
            if (base.getHostname() != null) b.hostname(base.getHostname());
        }
        if (image != null) b.image(image);
        if (memory > 0) b.memory(memory);
        if (cpus > 0) b.cpus(cpus);
        if (replace) b.replace(true);
        if (detached) b.detached(true);
        if (network != null) b.network(network);
        if (mounts != null) b.mounts(mounts);
        if (ports != null) b.ports(ports);
        if (env != null) b.env(env);
        if (maxMemory != null) b.maxMemory(maxMemory);
        if (maxCpus != null) b.maxCpus(maxCpus);
        if (workdir != null) b.workdir(workdir);
        if (shell != null) b.shell(shell);
        if (entrypoint != null) b.entrypoint(entrypoint);
        if (cmd != null) b.cmd(cmd);
        if (pullPolicy != null) b.pullPolicy(pullPolicy);
        if (securityProfile != null) b.securityProfile(securityProfile);
        if (ephemeral) b.ephemeral(true);
        if (user != null) b.user(user);
        if (hostname != null) b.hostname(hostname);
        return b.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final SandboxCreateOptions opts = new SandboxCreateOptions();

        /** Sets the OCI image. */
        public Builder image(String image) { opts.image = image; return this; }

        /** Sets the memory limit in MiB. */
        public Builder memory(int mb) { opts.memory = mb; return this; }

        /** Sets the number of CPUs. */
        public Builder cpus(int cpus) { opts.cpus = cpus; return this; }

        /** Sets the maximum memory that can be hot-plugged in MiB. */
        public Builder maxMemory(long mb) { opts.maxMemory = mb; return this; }

        /** Sets the maximum number of CPUs that can be hot-plugged. */
        public Builder maxCpus(int cpus) { opts.maxCpus = cpus; return this; }

        /** If true, replaces any existing sandbox with the same name. */
        public Builder replace(boolean replace) { opts.replace = replace; return this; }

        /** If true, creates a detached sandbox that outlives this process. */
        public Builder detached(boolean detached) { opts.detached = detached; return this; }

        /** If true, creates an ephemeral sandbox. */
        public Builder ephemeral(boolean ephemeral) { opts.ephemeral = ephemeral; return this; }

        /** Sets network policy. */
        public Builder network(NetworkConfig network) { opts.network = network; return this; }

        /** Sets mount points. */
        public Builder mounts(Map<String, MountConfig> mounts) { opts.mounts = mounts; return this; }

        /** Adds a mount point. */
        public Builder mount(String guestPath, MountConfig mount) {
            if (opts.mounts == null) opts.mounts = new HashMap<String, MountConfig>();
            opts.mounts.put(guestPath, mount);
            return this;
        }

        /** Sets port mappings (host -> guest). */
        public Builder ports(Map<Integer, Integer> ports) { opts.ports = ports; return this; }

        /** Adds a port mapping. */
        public Builder port(int hostPort, int guestPort) {
            if (opts.ports == null) opts.ports = new HashMap<Integer, Integer>();
            opts.ports.put(hostPort, guestPort);
            return this;
        }

        /** Adds an environment variable. */
        public Builder env(String key, String value) {
            if (opts.env == null) opts.env = new HashMap<String, String>();
            opts.env.put(key, value);
            return this;
        }

        /** Sets all environment variables. */
        public Builder env(Map<String, String> env) { opts.env = env; return this; }

        /** Sets the initial working directory. */
        public Builder workdir(String workdir) { opts.workdir = workdir; return this; }

        /** Sets the default shell for exec operations. */
        public Builder shell(String shell) { opts.shell = shell; return this; }

        /** Sets the OCI entrypoint. */
        public Builder entrypoint(String[] entrypoint) { opts.entrypoint = entrypoint; return this; }

        /** Sets the OCI CMD. */
        public Builder cmd(String[] cmd) { opts.cmd = cmd; return this; }

        /** Sets the image pull policy ("always", "if-missing", "never"). */
        public Builder pullPolicy(String policy) { opts.pullPolicy = policy; return this; }

        /** Sets the security profile. */
        public Builder securityProfile(String profile) { opts.securityProfile = profile; return this; }

        /** Sets the guest user for exec operations. */
        public Builder user(String user) { opts.user = user; return this; }

        /** Sets the sandbox hostname. */
        public Builder hostname(String hostname) { opts.hostname = hostname; return this; }

        public SandboxCreateOptions build() { return opts; }
    }
}
