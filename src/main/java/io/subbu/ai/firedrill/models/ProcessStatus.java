package io.subbu.ai.firedrill.models;

/**
 * Enumeration representing the various stages of resume processing.
 * Each status indicates a milestone in the asynchronous processing pipeline.
 */
public enum ProcessStatus {
    /**
     * Initial state when the upload is received and processing begins
     */
    INITIATED,
    
    /**
     * Embeddings have been successfully generated from the resume text
     */
    EMBED_GENERATED,
    
    /**
     * Vector database has been updated with the new embeddings
     */
    VECTOR_DB_UPDATED,
    
    /**
     * Resume has been analyzed and summary has been generated
     */
    RESUME_ANALYZED,
    
    /**
     * All processing steps completed successfully
     */
    COMPLETED,
    
    /**
     * Error occurred during processing
     */
    FAILED
}
