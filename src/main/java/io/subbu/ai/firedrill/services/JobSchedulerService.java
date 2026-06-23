package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.JobQueue;
import io.subbu.ai.firedrill.models.JobType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework. boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduler service that periodically picks up jobs from the queue and processes them.
 * Enabled only when app.scheduler.enabled=true in configuration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class JobSchedulerService {

    private final JobQueueService jobQueueService;
    private final ResumeJobProcessor resumeJobProcessor;
    private final ExecutorService jobExecutorService;

    private final AtomicInteger activeJobCount = new AtomicInteger(0);
    
    /**
     * Main scheduler that processes resume jobs.
     * Runs at fixed intervals configured in application.yml.
     * Uses fixedDelayString to ensure jobs are not picked up while previous batch is still processing.
     */
    @Scheduled(fixedDelayString = "${app.scheduler.poll-interval-ms:5000}", 
               initialDelayString = "${app.scheduler.initial-delay-ms:10000}")
    public void processResumeJobs() {
        try {
            int batchSize = getBatchSize();
            int currentActive = activeJobCount.get();
            
            log.debug("Resume job scheduler tick: activeJobs={}, batchSize={}", currentActive, batchSize);
            
            // Don't claim more jobs if we're already at or near capacity
            if (currentActive >= batchSize) {
                log.debug("Skipping job claim - already at capacity: active={}, max={}", 
                         currentActive, batchSize);
                return;
            }
            
            // Claim jobs
            int availableSlots = batchSize - currentActive;
            List<JobQueue> jobs = jobQueueService.claimJobs(JobType.RESUME_PROCESSING, availableSlots);
            
            if (jobs.isEmpty()) {
                log.trace("No pending resume jobs to process");
                return;
            }
            
            log.info("Claimed {} resume jobs for processing", jobs.size());
            
            // Process each job asynchronously
            for (JobQueue job : jobs) {
                processJobAsync(job);
            }
            
        } catch (Exception e) {
            log.error("Error in resume job scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduler to detect and reset stale jobs.
     * Runs less frequently than the main job processor.
     */
    @Scheduled(fixedDelayString = "${app.scheduler.stale-check-interval-ms:60000}", 
               initialDelayString = "${app.scheduler.stale-check-initial-delay-ms:30000}")
    public void checkForStaleJobs() {
        try {
            log.debug("Running stale job detection");
            
            int resetCount = jobQueueService.resetStaleJobs();
            
            if (resetCount > 0) {
                log.warn("Stale job check completed: reset {} stale jobs", resetCount);
            } else {
                log.debug("Stale job check completed: no stale jobs found");
            }
            
        } catch (Exception e) {
            log.error("Error checking for stale jobs: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduler to cleanup old completed jobs.
     * Runs daily to maintain database size.
     */
    @Scheduled(cron = "${app.scheduler.cleanup-cron:0 0 2 * * ?}") // Default: 2 AM daily
    public void cleanupOldJobs() {
        try {
            int daysToKeep = 30; // TODO: Make configurable
            log.info("Running cleanup of old completed jobs (keeping last {} days)", daysToKeep);
            
            int deletedCount = jobQueueService.cleanupOldJobs(daysToKeep);
            
            if (deletedCount > 0) {
                log.info("Cleanup completed: deleted {} old completed jobs", deletedCount);
            } else {
                log.debug("Cleanup completed: no old jobs to delete");
            }
            
        } catch (Exception e) {
            log.error("Error during job cleanup: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduler to log queue metrics.
     * Provides visibility into queue health and performance.
     */
    @Scheduled(fixedDelayString = "${app.scheduler.metrics-log-interval-ms:300000}") // Default: 5 minutes
    public void logQueueMetrics() {
        try {
            long pendingCount = jobQueueService.getQueueDepth(JobType.RESUME_PROCESSING);
            long processingCount = jobQueueService.getJobCount(io.subbu.ai.firedrill.models.JobStatus.PROCESSING);
            long completedCount = jobQueueService.getJobCount(io.subbu.ai.firedrill.models.JobStatus.COMPLETED);
            long failedCount = jobQueueService.getJobCount(io.subbu.ai.firedrill.models.JobStatus.FAILED);
            double avgDuration = jobQueueService.getAverageProcessingDuration(JobType.RESUME_PROCESSING);
            int currentActive = activeJobCount.get();
            
            log.info("Queue Metrics - Pending: {}, Processing: {}, Active Tasks: {}, Completed: {}, Failed: {}, Avg Duration: {:.2f}s",
                     pendingCount, processingCount, currentActive, completedCount, failedCount, avgDuration);
            
        } catch (Exception e) {
            log.error("Error logging queue metrics: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a job asynchronously using the executor service.
     * 
     * @param job The job to process
     */
    private void processJobAsync(JobQueue job) {
        activeJobCount.incrementAndGet();
        
        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting async job processing: jobId={}, type={}, priority={}", 
                        job.getId(), job.getJobType(), job.getPriority());
                
                resumeJobProcessor.processJob(job);
                
                log.info("Async job processing completed: jobId={}", job.getId());
                
            } catch (Exception e) {
                log.error("Error processing job asynchronously: jobId={}, error={}", 
                         job.getId(), e.getMessage(), e);
            } finally {
                activeJobCount.decrementAndGet();
            }
        }, jobExecutorService);
    }

    /**
     * Get the configured batch size for job claiming.
     * Falls back to a reasonable default if not configured.
     * 
     * @return Batch size
     */
    private int getBatchSize() {
        // This could be injected via @Value, but for now use a reasonable default
        // TODO: Make this configurable via application.yml
        return 5;
    }

    /**
     * Get current active job count.
     * Useful for health checks and monitoring.
     * 
     * @return Number of jobs currently being processed
     */
    public int getActiveJobCount() {
        return activeJobCount.get();
    }

    /**
     * Get queue health summary.
     * 
     * @return Map containing queue health metrics
     */
    public java.util.Map<String, Object> getQueueHealth() {
        return java.util.Map.of(
            "activeJobs", activeJobCount.get(),
            "pendingJobs", jobQueueService.getQueueDepth(JobType.RESUME_PROCESSING),
            "processingJobs", jobQueueService.getJobCount(io.subbu.ai.firedrill.models.JobStatus.PROCESSING),
            "averageProcessingTime", jobQueueService.getAverageProcessingDuration(JobType.RESUME_PROCESSING)
        );
    }
}
