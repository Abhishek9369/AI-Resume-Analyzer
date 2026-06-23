package io.subbu.ai.firedrill.repositories;

import io.subbu.ai.firedrill.entities.JobQueue;
import io.subbu.ai.firedrill.models.JobStatus;
import io.subbu.ai.firedrill.models.JobType;
import jakarta.persistence.LockModeType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for JobQueue entity with custom queries for job management.
 * Provides atomic job claiming using pessimistic locking and various query methods.
 */
@Repository
public interface JobQueueRepository extends JpaRepository<JobQueue, UUID> {

    /**
     * Find and lock the next available pending job with PESSIMISTIC_WRITE lock.
     * This ensures atomic job claiming across multiple scheduler instances.
     * 
     * @param jobType The type of job to find
     * @param limit Maximum number of jobs to retrieve
     * @return List of locked pending jobs ordered by priority (desc) and created date (asc)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM JobQueue j WHERE j.status = 'PENDING' AND j.jobType = :jobType " +
           "AND (j.scheduledFor IS NULL OR j.scheduledFor <= CURRENT_TIMESTAMP) " +
           "ORDER BY j.priority DESC, j.createdAt ASC")
    List<JobQueue> findAndLockPendingJobs(@Param("jobType") JobType jobType, Pageable pageable);

    /**
     * Find stale jobs that are stuck in PROCESSING status.
     * A job is considered stale if its heartbeat hasn't been updated within the staleThreshold.
     * 
     * @param staleThreshold DateTime threshold for considering a job stale
     * @return List of stale jobs
     */
    @Query("SELECT j FROM JobQueue j WHERE j.status = 'PROCESSING' " +
           "AND (j.heartbeatAt IS NULL OR j.heartbeatAt < :staleThreshold)")
    List<JobQueue> findStaleJobs(@Param("staleThreshold") LocalDateTime staleThreshold);

    /**
     * Find all jobs by correlation ID.
     * Useful for tracking related jobs that were submitted together.
     * 
     * @param correlationId The correlation ID to search for
     * @return List of jobs with matching correlation ID
     */
    @Query("SELECT j FROM JobQueue j WHERE j.correlationId = :correlationId " +
           "ORDER BY j.createdAt ASC")
    List<JobQueue> findByCorrelationId(@Param("correlationId") String correlationId);

    /**
     * Find jobs by status.
     * 
     * @param status The job status to search for
     * @param pageable Pagination information
     * @return Page of jobs with matching status
     */
    Page<JobQueue> findByStatus(JobStatus status, Pageable pageable);

    /**
     * Find jobs by status and type.
     * 
     * @param status The job status
     * @param jobType The job type
     * @param pageable Pagination information
     * @return Page of jobs matching status and type
     */
    Page<JobQueue> findByStatusAndJobType(JobStatus status, JobType jobType, Pageable pageable);

    /**
     * Find jobs created by a specific worker.
     * 
     * @param assignedTo Worker ID
     * @param pageable Pagination information
     * @return Page of jobs processed by the worker
     */
    Page<JobQueue> findByAssignedTo(String assignedTo, Pageable pageable);

    /**
     * Count jobs by status.
     * 
     * @param status The job status
     * @return Count of jobs with the given status
     */
    long countByStatus(JobStatus status);

    /**
     * Count jobs by status and job type.
     * 
     * @param status The job status
     * @param jobType The job type
     * @return Count of jobs matching both criteria
     */
    long countByStatusAndJobType(JobStatus status, JobType jobType);

    /**
     * Count jobs created within a time range.
     * 
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return Count of jobs created in the time range
     */
    @Query("SELECT COUNT(j) FROM JobQueue j WHERE j.createdAt BETWEEN :startTime AND :endTime")
    long countJobsInTimeRange(@Param("startTime") LocalDateTime startTime, 
                               @Param("endTime") LocalDateTime endTime);

    /**
     * Count jobs completed within a time range.
     * 
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return Count of completed jobs in the time range
     */
    @Query("SELECT COUNT(j) FROM JobQueue j WHERE j.status = 'COMPLETED' " +
           "AND j.completedAt BETWEEN :startTime AND :endTime")
    long countCompletedJobsInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * Count jobs failed within a time range.
     * 
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return Count of failed jobs in the time range
     */
    @Query("SELECT COUNT(j) FROM JobQueue j WHERE j.status = 'FAILED' " +
           "AND j.updatedAt BETWEEN :startTime AND :endTime")
    long countFailedJobsInTimeRange(@Param("startTime") LocalDateTime startTime, 
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * Calculate average processing duration for completed jobs.
     * 
     * @param jobType The job type to calculate average for
     * @return Average processing duration in seconds, or 0 if no completed jobs
     */
    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (completed_at - started_at))), 0) " +
           "FROM job_queue WHERE status = 'COMPLETED' AND job_type = :jobType " +
           "AND started_at IS NOT NULL AND completed_at IS NOT NULL", 
           nativeQuery = true)
    Double getAverageProcessingDuration(@Param("jobType") String jobType);

    /**
     * Get queue depth (count of pending jobs) by job type.
     * 
     * @param jobType The job type
     * @return Count of pending jobs for the type
     */
    @Query("SELECT COUNT(j) FROM JobQueue j WHERE j.status = 'PENDING' AND j.jobType = :jobType")
    long getQueueDepth(@Param("jobType") JobType jobType);

    /**
     * Find jobs that can be retried (FAILED status with retryCount < maxRetries).
     * 
     * @param jobType The job type
     * @return List of jobs eligible for retry
     */
    @Query("SELECT j FROM JobQueue j WHERE j.status = 'FAILED' AND j.jobType = :jobType " +
           "AND j.retryCount < j.maxRetries ORDER BY j.updatedAt ASC")
    List<JobQueue> findJobsForRetry(@Param("jobType") JobType jobType, Pageable pageable);

    /**
     * Delete completed jobs older than specified date.
     * Used for cleanup/archival of old job records.
     * 
     * @param beforeDate Delete jobs completed before this date
     * @return Number of jobs deleted
     */
    @Modifying
    @Query("DELETE FROM JobQueue j WHERE j.status = 'COMPLETED' AND j.completedAt < :beforeDate")
    int deleteCompletedJobsBefore(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * Find jobs by metadata JSON field using PostgreSQL JSONB operators.
     * Example: find jobs with specific userId in metadata.
     * 
     * @param jsonPath JSON path expression (e.g., '$.userId')
     * @param value Value to search for
     * @return List of jobs matching the metadata criteria
     */
    @Query(value = "SELECT * FROM job_queue WHERE metadata->:jsonPath = to_jsonb(:value::text)", 
           nativeQuery = true)
    List<JobQueue> findByMetadataField(@Param("jsonPath") String jsonPath, 
                                        @Param("value") String value);

    /**
     * Get job statistics for monitoring dashboard.
     * Returns counts grouped by status.
     * 
     * @param jobType The job type to get stats for
     * @return Map-like result with status and count
     */
    @Query("SELECT j.status as status, COUNT(j) as count FROM JobQueue j " +
           "WHERE j.jobType = :jobType GROUP BY j.status")
    List<Object[]> getJobStatsByType(@Param("jobType") JobType jobType);

    /**
     * Reset stale jobs back to PENDING status for retry.
     * Updates jobs that are stuck in PROCESSING status.
     * 
     * @param staleThreshold DateTime threshold for considering a job stale
     * @return Number of jobs reset
     */
    @Modifying
    @Query("UPDATE JobQueue j SET j.status = 'PENDING', j.assignedTo = NULL, " +
           "j.startedAt = NULL, j.heartbeatAt = NULL, j.retryCount = j.retryCount + 1 " +
           "WHERE j.status = 'PROCESSING' AND (j.heartbeatAt IS NULL OR j.heartbeatAt < :staleThreshold) " +
           "AND j.retryCount < j.maxRetries")
    int resetStaleJobs(@Param("staleThreshold") LocalDateTime staleThreshold);

    /**
     * Move jobs to dead letter queue that have exceeded max retries.
     * 
     * @param staleThreshold DateTime threshold
     * @return Number of jobs moved to DLQ
     */
    @Modifying
    @Query("UPDATE JobQueue j SET j.status = 'FAILED', j.errorMessage = " +
           "CONCAT('Max retries exceeded. Last heartbeat: ', COALESCE(CAST(j.heartbeatAt as string), 'never')) " +
           "WHERE j.status = 'PROCESSING' AND (j.heartbeatAt IS NULL OR j.heartbeatAt < :staleThreshold) " +
           "AND j.retryCount >= j.maxRetries")
    int moveToDeadLetterQueue(@Param("staleThreshold") LocalDateTime staleThreshold);

    /**
     * Find a job by its ID with pessimistic lock.
     * Useful for updating job status atomically.
     * 
     * @param id Job ID
     * @return Optional containing the locked job if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT j FROM JobQueue j WHERE j.id = :id")
    Optional<JobQueue> findByIdWithLock(@Param("id") UUID id);
}
