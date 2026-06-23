package io.subbu.ai.firedrill.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model object for Resume Analysis request to LLM.
 * Contains the resume content and metadata for AI processing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumeAnalysisRequest {

    /**
     * Full text content of the resume
     */
    private String resumeContent;

    /**
     * Original filename
     */
    private String filename;

    /**
     * Additional context or instructions for the LLM
     */
    private String additionalContext;
}
