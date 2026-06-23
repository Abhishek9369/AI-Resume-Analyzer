package io.subbu.ai.firedrill.models;

/**
 * Types of jobs that can be queued for processing
 */
public enum JobType {
    /**
     * Resume parsing, AI analysis, and embedding generation
     */
    RESUME_PROCESSING,
    
    /**
     * Batch embedding generation for multiple candidates
     */
    BATCH_EMBEDDING,
    
    /**
     * Data migration or bulk update jobs
     */
    DATA_MIGRATION
}
