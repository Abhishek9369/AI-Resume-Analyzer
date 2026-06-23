package io.subbu.ai.firedrill.models;

/**
 * Entity type for feedback
 */
public enum EntityType {
    CANDIDATE("Candidate", "Feedback on a candidate"),
    JOB_REQUIREMENT("Job Requirement", "Feedback on a job requirement");

    private final String displayName;
    private final String description;

    EntityType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
