package com.microsandbox.sdk.model;

/**
 * Sandbox status enum.
 * <p>JDK 8 compatible.</p>
 *
 * <p>Aligned with the Go SDK's SandboxStatus values.</p>
 */
public enum SandboxStatus {

    UNKNOWN("unknown"),
    CREATED("created"),
    STARTING("starting"),
    RUNNING("running"),
    DRAINING("draining"),
    PAUSED("paused"),
    STOPPED("stopped"),
    CRASHED("crashed");

    private final String wireValue;

    SandboxStatus(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static SandboxStatus fromWireValue(String wireValue) {
        if (wireValue == null) return UNKNOWN;
        for (SandboxStatus s : values()) {
            if (s.wireValue.equals(wireValue)) return s;
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return wireValue;
    }
}
