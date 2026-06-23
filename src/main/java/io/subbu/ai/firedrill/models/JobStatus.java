package io.subbu.ai.firedrill.models;

/**
 * Status of a job in the queue
 */
public enum JobStatus {
    /**
     * Job is waiting to be picked up by scheduler
     */
    PENDING,
    
    /**
     * Job is currently being processed
     */
    PROCESSING,
    
    /**
     * Job completed successfully
     */
    COMPLETED,
    
    /**
     * Job failed after all retries
     */
    FAILED,
    
    /**
     * Job was manually cancelled
     */
    CANCELLED
}
