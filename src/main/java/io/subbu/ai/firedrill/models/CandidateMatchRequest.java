package io.subbu.ai.firedrill.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model object for Candidate Matching request to LLM.
 * Contains candidate and job requirement data for AI-based matching.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateMatchRequest {

    /**
     * Candidate's experience summary
     */
    private String experienceSummary;

    /**
     * Candidate's skills
     */
    private String skills;

    /**
     * Candidate's domain knowledge
     */
    private String domainKnowledge;

    /**
     * Candidate's academic background
     */
    private String academicBackground;

    /**
     * Candidate's years of experience
     */
    private Integer yearsOfExperience;

    /**
     * Job title
     */
    private String jobTitle;

    /**
     * Job description
     */
    private String jobDescription;

    /**
     * Required skills for the job
     */
    private String requiredSkills;

    /**
     * Required education
     */
    private String requiredEducation;

    /**
     * Domain requirements
     */
    private String domainRequirements;

    /**
     * Minimum experience years required
     */
    private Integer minExperienceYears;

    /**
     * Maximum experience years required
     */
    private Integer maxExperienceYears;

    /**
     * Additional context from external profile enrichment (GitHub, LinkedIn, etc.).
     * Included when available to improve match accuracy.
     */
    private String enrichedProfileContext;
}
