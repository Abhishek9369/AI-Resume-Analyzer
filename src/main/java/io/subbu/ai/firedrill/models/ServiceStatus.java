package io.subbu.ai.firedrill.models;

/**
 * Service health status enumeration
 */
public enum ServiceStatus {
    UP("Up", "Service is operational"),
    DOWN("Down", "Service is not available"),
    DEGRADED("Degraded", "Service is partially operational"),
    UNKNOWN("Unknown", "Service status unknown");

    private final String displayName;
    private final String description;

    ServiceStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHealthy() {
        return this == UP;
    }
}
