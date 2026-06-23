package io.subbu.ai.firedrill.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model object for Candidate Matching response from LLM.
 * Contains AI-generated matching scores and explanations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateMatchResponse {

    /**
     * Overall match score (0-100)
     */
    private Double matchScore;

    /**
     * Skills matching score (0-100)
     */
    private Double skillsScore;

    /**
     * Experience matching score (0-100)
     */
    private Double experienceScore;

    /**
     * Education matching score (0-100)
     */
    private Double educationScore;

    /**
     * Domain knowledge matching score (0-100)
     */
    private Double domainScore;

    /**
     * Detailed explanation of the match
     */
    private String explanation;

    /**
     * Key strengths of the candidate for this role
     */
    private String strengths;

    /**
     * Potential gaps or weaknesses
     */
    private String gaps;

    /**
     * Recommendation (e.g., "Strong Match", "Good Match", "Partial Match", "No Match")
     */
    private String recommendation;
}
