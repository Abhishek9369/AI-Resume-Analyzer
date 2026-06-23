package io.subbu.ai.firedrill.entities;

import io.subbu.ai.firedrill.models.ProcessStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking the progress of resume processing operations.
 * Provides real-time status updates for async batch processing.
 */
@Entity
@Table(name = "process_tracker")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessTracker {

    /**
     * Primary key - unique identifier for this processing job
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    /**
     * Current status of the processing job
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProcessStatus status;

    /**
     * Number of files to be processed in this batch
     */
    @Column(name = "total_files")
    private Integer totalFiles;

    /**
     * Number of files successfully processed
     */
    @Column(name = "processed_files")
    private Integer processedFiles;

    /**
     * Number of files that failed processing
     */
    @Column(name = "failed_files")
    private Integer failedFiles;

    /**
     * Detailed message about current processing state or errors
     */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /**
     * Name of the uploaded file (original or ZIP)
     */
    @Column(name = "uploaded_filename")
    private String uploadedFilename;

    /**
     * Link to job queue for scheduler-based processing
     */
    @Column(name = "job_id")
    private UUID jobId;

    /**
     * Correlation ID to group related jobs (batch uploads)
     */
    @Column(name = "correlation_id")
    private String correlationId;

    /**
     * Timestamp when the processing started
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp of the last status update
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Timestamp when processing completed (success or failure)
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Helper method to update status and message atomically
     * 
     * @param newStatus The new processing status
     * @param newMessage Descriptive message about the status
     */
    public void updateStatus(ProcessStatus newStatus, String newMessage) {
        this.status = newStatus;
        this.message = newMessage;
        if (newStatus == ProcessStatus.COMPLETED || newStatus == ProcessStatus.FAILED) {
            this.completedAt = LocalDateTime.now();
        }
    }

    /**
     * Increment the count of processed files
     */
    public void incrementProcessedFiles() {
        this.processedFiles = (this.processedFiles == null ? 0 : this.processedFiles) + 1;
    }

    /**
     * Increment the count of failed files
     */
    public void incrementFailedFiles() {
        this.failedFiles = (this.failedFiles == null ? 0 : this.failedFiles) + 1;
    }
}
