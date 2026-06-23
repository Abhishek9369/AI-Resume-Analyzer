package io.subbu.ai.firedrill.models;

/**
 * Input model for updating candidate match status.
 * Corresponds to the UpdateCandidateMatchInput GraphQL input type.
 */
public record UpdateCandidateMatchInput(
        Boolean isSelected,
        Boolean isShortlisted,
        String recruiterNotes) {
}
