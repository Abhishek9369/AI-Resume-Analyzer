package io.subbu.ai.firedrill.models;

/**
 * Priority levels for job processing
 * Higher priority jobs are processed first
 */
public enum JobPriority {
    /**
     * Low priority - processed when capacity available
     */
    LOW(0),
    
    /**
     * Normal priority - standard processing
     */
    NORMAL(1),
    
    /**
     * High priority - processed before normal jobs
     */
    HIGH(2),
    
    /**
     * Urgent priority - processed immediately
     */
    URGENT(3);

    private final int value;

    JobPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
    
    /**
     * Get priority from integer value
     */
    public static JobPriority fromValue(int value) {
        for (JobPriority priority : values()) {
            if (priority.value == value) {
                return priority;
            }
        }
        return NORMAL; // Default
    }
}
