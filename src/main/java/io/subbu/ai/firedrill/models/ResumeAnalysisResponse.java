package io.subbu.ai.firedrill.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model object for Resume Analysis response from LLM.
 * Contains extracted candidate information and AI-generated summaries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysisResponse {

    /**
     * Extracted candidate name
     */
    private String name;

    /**
     * Extracted email address
     */
    private String email;

    /**
     * Extracted mobile number
     */
    private String mobile;

    /**
     * AI-generated experience summary
     */
    private String experienceSummary;

    /**
     * Identified skills (comma-separated)
     */
    @JsonDeserialize(using = ArrayOrStringDeserializer.class)
    private String skills;

    /**
     * Domain knowledge summary
     */
    @JsonDeserialize(using = ArrayOrStringDeserializer.class)
    private String domainKnowledge;

    /**
     * Academic background summary
     */
    @JsonDeserialize(using = ArrayOrStringDeserializer.class)
    private String academicBackground;

    /**
     * Calculated years of experience
     */
    private Integer yearsOfExperience;

    /**
     * Confidence score of the analysis (0-1)
     */
    private Double confidenceScore;
}
