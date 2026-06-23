package io.subbu.ai.firedrill.models;

/**
 * Employee status enumeration
 */
public enum EmployeeStatus {
    ACTIVE("Active", "Currently employed"),
    ON_LEAVE("On Leave", "On approved leave"),
    SUSPENDED("Suspended", "Temporarily suspended"),
    TERMINATED("Terminated", "Employment terminated");

    private final String displayName;
    private final String description;

    EmployeeStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }
}
