package io.subbu.ai.firedrill.models;

/**
 * Employment type enumeration
 */
public enum EmploymentType {
    FULL_TIME("Full Time", "Regular full-time employee"),
    PART_TIME("Part Time", "Part-time employee"),
    CONTRACT("Contract", "Contract worker"),
    INTERN("Intern", "Internship position");

    private final String displayName;
    private final String description;

    EmploymentType(String displayName, String description) {
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
