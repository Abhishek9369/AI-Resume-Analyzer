package io.subbu.ai.firedrill.entities;

import io.subbu.ai.firedrill.models.JobStatus;
import io.subbu.ai.firedrill.models.JobType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a job in the processing queue.
 * Jobs are picked up by schedulers and processed asynchronously.
 * 
 * Lifecycle: PENDING → PROCESSING → COMPLETED/FAILED
 * Failed jobs can be retried up to max_retries times.
 */
@Entity
@Table(name = "job_queue", indexes = {
    @Index(name = "idx_job_queue_status_priority", columnList = "status, priority DESC, created_at ASC"),
    @Index(name = "idx_job_queue_scheduled_for", columnList = "scheduled_for"),
    @Index(name = "idx_job_queue_assigned_to", columnList = "assigned_to"),
    @Index(name = "idx_job_queue_correlation_id", columnList = "correlation_id"),
    @Index(name = "idx_job_queue_created_at", columnList = "created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class JobQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private JobType jobType;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JobStatus status;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Lob
    @Column(name = "file_data", columnDefinition = "BYTEA")
    private byte[] fileData;

    @Column(name = "filename", length = 500)
    private String filename;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "retry_count")
    private Integer retryCount;

    @Column(name = "max_retries")
    private Integer maxRetries;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "scheduled_for")
    private LocalDateTime scheduledFor;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "heartbeat_at")
    private LocalDateTime heartbeatAt;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (scheduledFor == null) {
            scheduledFor = LocalDateTime.now();
        }
        if (status == null) {
            status = JobStatus.PENDING;
        }
        if (priority == null) {
            priority = 0;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (maxRetries == null) {
            maxRetries = 3;
        }
        log.debug("Created job {}: type={}, priority={}, filename={}", 
            id, jobType, priority, filename);
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        log.trace("Updated job {}: status={}, retry={}/{}", 
            id, status, retryCount, maxRetries);
    }

    /**
     * Check if job can be retried
     */
    public boolean canRetry() {
        boolean can = retryCount < maxRetries;
        log.debug("Job {} canRetry: {} (retry {}/{})", id, can, retryCount, maxRetries);
        return can;
    }

    /**
     * Increment retry count
     */
    public void incrementRetryCount() {
        this.retryCount++;
        log.info("Job {} retry count incremented to {}/{}", id, retryCount, maxRetries);
    }

    /**
     * Mark job as processing
     */
    public void markAsProcessing(String workerId) {
        this.status = JobStatus.PROCESSING;
        this.assignedTo = workerId;
        this.startedAt = LocalDateTime.now();
        this.heartbeatAt = LocalDateTime.now();
        log.info("Job {} marked as PROCESSING, assigned to worker: {}", id, workerId);
    }

    /**
     * Mark job as completed
     */
    public void markAsCompleted() {
        this.status = JobStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        long durationSeconds = startedAt != null 
                ? java.time.Duration.between(startedAt, completedAt).getSeconds()
                : 0;
        log.info("Job {} COMPLETED successfully in {} seconds ({})", id, durationSeconds, filename);
    }

    /**
     * Mark job as failed
     */
    public void markAsFailed(Exception error) {
        this.status = JobStatus.FAILED;
        this.errorMessage = error.getMessage();
        this.errorStackTrace = getStackTraceAsString(error);
        this.completedAt = LocalDateTime.now();
        log.error("Job {} FAILED: {} (retry {}/{})", id, error.getMessage(), retryCount, maxRetries);
    }

    /**
     * Reset for retry
     */
    public void resetForRetry() {
        this.status = JobStatus.PENDING;
        this.assignedTo = null;
        this.startedAt = null;
        this.heartbeatAt = null;
        this.scheduledFor = LocalDateTime.now().plusMinutes(5); // Delay retry by 5 minutes
        incrementRetryCount();
        log.warn("Job {} reset for retry (attempt {}/{}), scheduled for {}", 
            id, retryCount, maxRetries, scheduledFor);
    }

    /**
     * Update heartbeat to indicate job is still processing
     */
    public void updateHeartbeat() {
        this.heartbeatAt = LocalDateTime.now();
        log.trace("Job {} heartbeat updated", id);
    }

    /**
     * Check if job is stale (no heartbeat for 10 minutes while processing)
     */
    public boolean isStale() {
        if (status != JobStatus.PROCESSING || heartbeatAt == null) {
            return false;
        }
        boolean stale = heartbeatAt.isBefore(LocalDateTime.now().minusMinutes(10));
        if (stale) {
            log.warn("Job {} is STALE (last heartbeat: {}, worker: {})", 
                id, heartbeatAt, assignedTo);
        }
        return stale;
    }

    /**
     * Get processing duration in seconds
     */
    public long getProcessingDurationSeconds() {
        if (startedAt == null) return 0;
        LocalDateTime endTime = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, endTime).getSeconds();
    }

    /**
     * Get queue wait time in seconds
     */
    public long getQueueWaitTimeSeconds() {
        if (startedAt == null) return 0;
        return java.time.Duration.between(createdAt, startedAt).getSeconds();
    }

    /**
     * Convert exception to stack trace string
     */
    private String getStackTraceAsString(Exception error) {
        if (error == null) return null;
        
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            error.printStackTrace(pw);
            return sw.toString();
        } catch (Exception e) {
            return error.toString();
        }
    }

    /**
     * Get metadata value as string
     */
    public String getMetadataValue(String key) {
        if (metadata == null) return null;
        Object value = metadata.get(key);
        return value != null ? value.toString() : null;
    }
}
