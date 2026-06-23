package io.subbu.ai.firedrill.models;

/**
 * Feedback type enumeration
 */
public enum FeedbackType {
    SHORTLIST("Shortlist", "Candidate shortlisted for next round"),
    REJECT("Reject", "Candidate rejected"),
    INTERVIEW("Interview", "Feedback from interview"),
    OFFER("Offer", "Offer extended to candidate"),
    GENERAL("General", "General feedback or comments"),
    TECHNICAL("Technical", "Technical skills assessment"),
    CULTURAL_FIT("Cultural Fit", "Cultural fit assessment");

    private final String displayName;
    private final String description;

    FeedbackType(String displayName, String description) {
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
