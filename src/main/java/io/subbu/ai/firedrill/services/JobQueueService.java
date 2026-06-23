package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.JobQueue;
import io.subbu.ai.firedrill.models.JobPriority;
import io.subbu.ai.firedrill.models.JobStatus;
import io.subbu.ai.firedrill.models.JobType;
import io.subbu.ai.firedrill.repositories.JobQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing job queue operations.
 * Handles job creation, claiming, status updates, retries, and monitoring.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobQueueService {

    private final JobQueueRepository jobQueueRepository;

    @Value("${app.scheduler.stale-job-threshold-minutes:15}")
    private int staleJobThresholdMinutes;

    @Value("${app.scheduler.worker-id:default-worker}")
    private String workerId;

    /**
     * Create a new job and add it to the queue.
     * 
     * @param jobType Type of job
     * @param fileData File data to process
     * @param metadata Additional metadata as JSON
     * @param priority Job priority
     * @param correlationId Correlation ID for grouping related jobs
     * @return Created JobQueue entity
     */
    @Transactional
    public JobQueue createJob(JobType jobType, byte[] fileData, Map<String, Object> metadata, 
                              JobPriority priority, String correlationId) {
        log.info("Creating new job: type={}, priority={}, correlationId={}, dataSize={} bytes", 
                 jobType, priority, correlationId, fileData != null ? fileData.length : 0);
        
        JobQueue job = JobQueue.builder()
                .jobType(jobType)
                .status(JobStatus.PENDING)
                .priority(priority.getValue())
                .fileData(fileData)
                .metadata(metadata)
                .correlationId(correlationId)
                .retryCount(0)
                .maxRetries(3)
                .build();
        
        JobQueue savedJob = jobQueueRepository.save(job);
        log.info("Job created successfully: jobId={}, type={}, status={}", 
                 savedJob.getId(), savedJob.getJobType(), savedJob.getStatus());
        
        return savedJob;
    }

    /**
     * Create a job with default priority (NORMAL).
     */
    @Transactional
    public JobQueue createJob(JobType jobType, byte[] fileData, Map<String, Object> metadata, 
                              String correlationId) {
        return createJob(jobType, fileData, metadata, JobPriority.NORMAL, correlationId);
    }

    /**
     * Create a job with scheduled execution time.
     * 
     * @param jobType Type of job
     * @param fileData File data to process
     * @param metadata Additional metadata
     * @param priority Job priority
     * @param correlationId Correlation ID
     * @param scheduledAt Time when job should be picked up
     * @return Created JobQueue entity
     */
    @Transactional
    public JobQueue createScheduledJob(JobType jobType, byte[] fileData, Map<String, Object> metadata, 
                                       JobPriority priority, String correlationId, LocalDateTime scheduledAt) {
        log.info("Creating scheduled job: type={}, priority={}, scheduledAt={}, correlationId={}", 
                 jobType, priority, scheduledAt, correlationId);
        
        JobQueue job = JobQueue.builder()
                .jobType(jobType)
                .status(JobStatus.PENDING)
                .priority(priority.getValue())
                .fileData(fileData)
                .metadata(metadata)
                .correlationId(correlationId)
                .scheduledFor(scheduledAt)
                .retryCount(0)
                .maxRetries(3)
                .build();
        
        JobQueue savedJob = jobQueueRepository.save(job);
        log.info("Scheduled job created: jobId={}, scheduledAt={}", savedJob.getId(), scheduledAt);
        
        return savedJob;
    }

    /**
     * Claim a batch of pending jobs for processing.
     * Uses pessimistic locking to ensure atomic job claiming across multiple workers.
     * 
     * @param jobType Type of jobs to claim
     * @param batchSize Number of jobs to claim
     * @return List of claimed jobs
     */
    @Transactional
    public List<JobQueue> claimJobs(JobType jobType, int batchSize) {
        log.debug("Attempting to claim jobs: type={}, batchSize={}, worker={}", 
                  jobType, batchSize, workerId);
        
        Pageable pageable = PageRequest.of(0, batchSize);
        List<JobQueue> jobs = jobQueueRepository.findAndLockPendingJobs(jobType, pageable);
        
        if (jobs.isEmpty()) {
            log.trace("No pending jobs found for type={}", jobType);
            return jobs;
        }
        
        log.info("Found {} pending jobs to process for type={}", jobs.size(), jobType);
        
        // Mark jobs as PROCESSING
        for (JobQueue job : jobs) {
            job.markAsProcessing(workerId);
            log.debug("Claimed job: jobId={}, priority={}, createdAt={}", 
                     job.getId(), job.getPriority(), job.getCreatedAt());
        }
        
        List<JobQueue> claimedJobs = jobQueueRepository.saveAll(jobs);
        log.info("Successfully claimed {} jobs for processing by worker={}", 
                 claimedJobs.size(), workerId);
        
        return claimedJobs;
    }

    /**
     * Update heartbeat for a job to indicate it's still being processed.
     * 
     * @param jobId The job ID
     */
    @Transactional
    public void updateHeartbeat(UUID jobId) {
        log.trace("Updating heartbeat for jobId={}", jobId);
        
        Optional<JobQueue> jobOpt = jobQueueRepository.findById(jobId);
        if (jobOpt.isPresent()) {
            JobQueue job = jobOpt.get();
            job.updateHeartbeat();
            jobQueueRepository.save(job);
            log.trace("Heartbeat updated for jobId={}, lastHeartbeat={}", 
                     jobId, job.getHeartbeatAt());
        } else {
            log.warn("Cannot update heartbeat - job not found: jobId={}", jobId);
        }
    }

    /**
     * Mark a job as completed successfully.
     * 
     * @param jobId The job ID
     * @param result Result data to save
     */
    @Transactional
    public void markJobCompleted(UUID jobId, Map<String, Object> result) {
        log.info("Marking job as completed: jobId={}", jobId);
        
        Optional<JobQueue> jobOpt = jobQueueRepository.findById(jobId);
        if (jobOpt.isPresent()) {
            JobQueue job = jobOpt.get();
            job.markAsCompleted();
            jobQueueRepository.save(job);
            
            long processingDuration = job.getStartedAt() != null && job.getCompletedAt() != null
                    ? java.time.Duration.between(job.getStartedAt(), job.getCompletedAt()).getSeconds()
                    : 0;
            
            log.info("Job completed successfully: jobId={}, duration={}s, retries={}, result={}", 
                     jobId, processingDuration, job.getRetryCount(), result);
        } else {
            log.error("Cannot mark as completed - job not found: jobId={}", jobId);
        }
    }

    /**
     * Mark a job as failed.
     * 
     * @param jobId The job ID
     * @param errorMessage Error message describing the failure
     * @param shouldRetry Whether the job should be retried
     */
    @Transactional
    public void markJobFailed(UUID jobId, String errorMessage, boolean shouldRetry) {
        log.warn("Marking job as failed: jobId={}, shouldRetry={}, error={}", 
                 jobId, shouldRetry, errorMessage);
        
        Optional<JobQueue> jobOpt = jobQueueRepository.findById(jobId);
        if (jobOpt.isPresent()) {
            JobQueue job = jobOpt.get();
            
            if (shouldRetry && job.canRetry()) {
                job.resetForRetry();
                job.setErrorMessage(errorMessage);
                log.info("Job will be retried: jobId={}, retryCount={}/{}", 
                        jobId, job.getRetryCount(), job.getMaxRetries());
            } else {
                job.markAsFailed(new RuntimeException(errorMessage));
                log.error("Job permanently failed: jobId={}, retryCount={}, error={}", 
                         jobId, job.getRetryCount(), errorMessage);
            }
            
            jobQueueRepository.save(job);
        } else {
            log.error("Cannot mark as failed - job not found: jobId={}", jobId);
        }
    }

    /**
     * Cancel a pending or processing job.
     * 
     * @param jobId The job ID
     * @return true if cancelled, false if job cannot be cancelled
     */
    @Transactional
    public boolean cancelJob(UUID jobId) {
        log.info("Attempting to cancel job: jobId={}", jobId);
        
        Optional<JobQueue> jobOpt = jobQueueRepository.findByIdWithLock(jobId);
        if (jobOpt.isEmpty()) {
            log.warn("Cannot cancel - job not found: jobId={}", jobId);
            return false;
        }
        
        JobQueue job = jobOpt.get();
        
        // Only cancel if job is PENDING or PROCESSING
        if (job.getStatus() != JobStatus.PENDING && job.getStatus() != JobStatus.PROCESSING) {
            log.warn("Cannot cancel job in status: jobId={}, status={}", jobId, job.getStatus());
            return false;
        }
        
        job.setStatus(JobStatus.CANCELLED);
        job.setErrorMessage("Cancelled by user");
        job.setCompletedAt(LocalDateTime.now());
        jobQueueRepository.save(job);
        
        log.info("Job cancelled successfully: jobId={}", jobId);
        return true;
    }

    /**
     * Find and reset stale jobs back to PENDING status.
     * A job is stale if it's in PROCESSING status but hasn't received a heartbeat update.
     * 
     * @return Number of jobs reset
     */
    @Transactional
    public int resetStaleJobs() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(staleJobThresholdMinutes);
        log.info("Checking for stale jobs: threshold={}, staleThresholdMinutes={}", 
                 threshold, staleJobThresholdMinutes);
        
        List<JobQueue> staleJobs = jobQueueRepository.findStaleJobs(threshold);
        
        if (staleJobs.isEmpty()) {
            log.debug("No stale jobs found");
            return 0;
        }
        
        log.warn("Found {} stale jobs to reset", staleJobs.size());
        
        int resetCount = 0;
        int dlqCount = 0;
        
        for (JobQueue job : staleJobs) {
            if (job.canRetry()) {
                job.resetForRetry();
                job.setErrorMessage("Stale job detected - no heartbeat since " + job.getHeartbeatAt());
                resetCount++;
                log.warn("Resetting stale job for retry: jobId={}, retryCount={}/{}, lastHeartbeat={}", 
                        job.getId(), job.getRetryCount(), job.getMaxRetries(), job.getHeartbeatAt());
            } else {
                job.markAsFailed(new RuntimeException("Max retries exceeded after becoming stale. Last heartbeat: " + 
                                 job.getHeartbeatAt()));
                dlqCount++;
                log.error("Moving stale job to DLQ (max retries exceeded): jobId={}, retryCount={}", 
                         job.getId(), job.getRetryCount());
            }
        }
        
        jobQueueRepository.saveAll(staleJobs);
        
        log.info("Stale job reset complete: reset={}, movedToDLQ={}, total={}", 
                 resetCount, dlqCount, staleJobs.size());
        
        return resetCount + dlqCount;
    }

    /**
     * Get job by ID.
     * 
     * @param jobId The job ID
     * @return Optional containing the job if found
     */
    @Transactional(readOnly = true)
    public Optional<JobQueue> getJob(UUID jobId) {
        log.debug("Retrieving job: jobId={}", jobId);
        return jobQueueRepository.findById(jobId);
    }

    /**
     * Get all jobs with a specific correlation ID.
     * 
     * @param correlationId The correlation ID
     * @return List of jobs with the correlation ID
     */
    @Transactional(readOnly = true)
    public List<JobQueue> getJobsByCorrelationId(String correlationId) {
        log.debug("Retrieving jobs by correlationId={}", correlationId);
        List<JobQueue> jobs = jobQueueRepository.findByCorrelationId(correlationId);
        log.debug("Found {} jobs for correlationId={}", jobs.size(), correlationId);
        return jobs;
    }

    /**
     * Get jobs by status with pagination.
     * 
     * @param status The job status
     * @param pageable Pagination information
     * @return Page of jobs
     */
    @Transactional(readOnly = true)
    public Page<JobQueue> getJobsByStatus(JobStatus status, Pageable pageable) {
        log.debug("Retrieving jobs by status={}, page={}, size={}", 
                 status, pageable.getPageNumber(), pageable.getPageSize());
        return jobQueueRepository.findByStatus(status, pageable);
    }

    /**
     * Get queue depth (number of pending jobs) for a job type.
     * 
     * @param jobType The job type
     * @return Number of pending jobs
     */
    @Transactional(readOnly = true)
    public long getQueueDepth(JobType jobType) {
        long depth = jobQueueRepository.getQueueDepth(jobType);
        log.debug("Queue depth for type={}: {}", jobType, depth);
        return depth;
    }

    /**
     * Get count of jobs by status.
     * 
     * @param status The job status
     * @return Count of jobs
     */
    @Transactional(readOnly = true)
    public long getJobCount(JobStatus status) {
        long count = jobQueueRepository.countByStatus(status);
        log.debug("Job count for status={}: {}", status, count);
        return count;
    }

    /**
     * Get average processing duration for a job type.
     * 
     * @param jobType The job type
     * @return Average duration in seconds
     */
    @Transactional(readOnly = true)
    public double getAverageProcessingDuration(JobType jobType) {
        Double avg = jobQueueRepository.getAverageProcessingDuration(jobType.name());
        log.debug("Average processing duration for type={}: {}s", jobType, avg);
        return avg != null ? avg : 0.0;
    }

    /**
     * Get job statistics grouped by status.
     * 
     * @param jobType The job type
     * @return Map of status to count
     */
    @Transactional(readOnly = true)
    public Map<JobStatus, Long> getJobStats(JobType jobType) {
        log.debug("Retrieving job statistics for type={}", jobType);
        List<Object[]> results = jobQueueRepository.getJobStatsByType(jobType);
        
        Map<JobStatus, Long> stats = new java.util.HashMap<>();
        for (Object[] result : results) {
            JobStatus status = (JobStatus) result[0];
            Long count = (Long) result[1];
            stats.put(status, count);
        }
        
        log.debug("Job stats for type={}: {}", jobType, stats);
        return stats;
    }

    /**
     * Delete completed jobs older than specified days.
     * 
     * @param daysToKeep Number of days to keep completed jobs
     * @return Number of jobs deleted
     */
    @Transactional
    public int cleanupOldJobs(int daysToKeep) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(daysToKeep);
        log.info("Cleaning up completed jobs older than {} days (before={})", daysToKeep, threshold);
        
        int deletedCount = jobQueueRepository.deleteCompletedJobsBefore(threshold);
        log.info("Cleanup complete: deleted {} completed jobs", deletedCount);
        
        return deletedCount;
    }

    /**
     * Get jobs eligible for retry.
     * 
     * @param jobType The job type
     * @param limit Maximum number of jobs to retrieve
     * @return List of jobs that can be retried
     */
    @Transactional(readOnly = true)
    public List<JobQueue> getJobsForRetry(JobType jobType, int limit) {
        log.debug("Finding jobs for retry: type={}, limit={}", jobType, limit);
        Pageable pageable = PageRequest.of(0, limit);
        List<JobQueue> jobs = jobQueueRepository.findJobsForRetry(jobType, pageable);
        log.debug("Found {} jobs eligible for retry", jobs.size());
        return jobs;
    }

    /**
     * Count total jobs in a time range.
     * 
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return Total count of jobs
     */
    @Transactional(readOnly = true)
    public long countJobsInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Counting jobs in time range: start={}, end={}", startTime, endTime);
        return jobQueueRepository.countJobsInTimeRange(startTime, endTime);
    }

    /**
     * Count completed jobs in a time range.
     * 
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return Count of completed jobs
     */
    @Transactional(readOnly = true)
    public long countCompletedJobsInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Counting completed jobs in time range: start={}, end={}", startTime, endTime);
        return jobQueueRepository.countCompletedJobsInTimeRange(startTime, endTime);
    }

    /**
     * Count failed jobs in a time range.
     * 
     * @param startTime Start of time range
     * @param endTime End of time range
     * @return Count of failed jobs
     */
    @Transactional(readOnly = true)
    public long countFailedJobsInTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        log.debug("Counting failed jobs in time range: start={}, end={}", startTime, endTime);
        return jobQueueRepository.countFailedJobsInTimeRange(startTime, endTime);
    }
}
