package io.subbu.ai.firedrill.controllers;

import io.subbu.ai.firedrill.entities.JobQueue;
import io.subbu.ai.firedrill.models.JobStatus;
import io.subbu.ai.firedrill.models.JobType;
import io.subbu.ai.firedrill.services.JobQueueService;
import io.subbu.ai.firedrill.services.JobSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for job queue monitoring and management.
 * Provides endpoints for queue statistics, job details, and manual operations.
 * Only active when scheduler is enabled.
 */
@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class JobQueueController {

    private final JobQueueService jobQueueService;
    private final JobSchedulerService jobSchedulerService;

    /**
     * Get queue health summary.
     * 
     * @return Queue health metrics
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getQueueHealth() {
        log.debug("API: Get queue health");
        
        Map<String, Object> health = jobSchedulerService.getQueueHealth();
        
        log.debug("Queue health retrieved: {}", health);
        return ResponseEntity.ok(health);
    }

    /**
     * Get comprehensive queue statistics.
     * 
     * @return Queue statistics including counts, averages, and throughput
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getQueueStatistics() {
        log.debug("API: Get queue statistics");
        
        Map<String, Object> stats = new HashMap<>();
        
        // Job counts by status
        stats.put("pendingCount", jobQueueService.getJobCount(JobStatus.PENDING));
        stats.put("processingCount", jobQueueService.getJobCount(JobStatus.PROCESSING));
        stats.put("completedCount", jobQueueService.getJobCount(JobStatus.COMPLETED));
        stats.put("failedCount", jobQueueService.getJobCount(JobStatus.FAILED));
        stats.put("cancelledCount", jobQueueService.getJobCount(JobStatus.CANCELLED));
        
        // Queue depth
        stats.put("queueDepth", jobQueueService.getQueueDepth(JobType.RESUME_PROCESSING));
        
        // Performance metrics
        stats.put("averageProcessingDuration", jobQueueService.getAverageProcessingDuration(JobType.RESUME_PROCESSING));
        
        // Active workers
        stats.put("activeWorkers", jobSchedulerService.getActiveJobCount());
        
        // Job statistics by type
        Map<JobStatus, Long> jobStats = jobQueueService.getJobStats(JobType.RESUME_PROCESSING);
        stats.put("jobStatsByStatus", jobStats);
        
        log.info("Queue statistics retrieved: pendingCount={}, processingCount={}, completedCount={}, failedCount={}",
                 stats.get("pendingCount"), stats.get("processingCount"), 
                 stats.get("completedCount"), stats.get("failedCount"));
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Get job by ID.
     * 
     * @param jobId Job UUID
     * @return Job details
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<JobQueue> getJob(@PathVariable UUID jobId) {
        log.debug("API: Get job: jobId={}", jobId);
        
        return jobQueueService.getJob(jobId)
                .map(job -> {
                    log.debug("Job found: jobId={}, status={}", jobId, job.getStatus());
                    return ResponseEntity.ok(job);
                })
                .orElseGet(() -> {
                    log.warn("Job not found: jobId={}", jobId);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Get jobs by correlation ID.
     * 
     * @param correlationId Correlation ID
     * @return List of jobs with the correlation ID
     */
    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<List<JobQueue>> getJobsByCorrelationId(@PathVariable String correlationId) {
        log.debug("API: Get jobs by correlation ID: {}", correlationId);
        
        List<JobQueue> jobs = jobQueueService.getJobsByCorrelationId(correlationId);
        
        log.debug("Found {} jobs for correlationId={}", jobs.size(), correlationId);
        return ResponseEntity.ok(jobs);
    }

    /**
     * Get jobs by status with pagination.
     * 
     * @param status Job status
     * @param page Page number (default 0)
     * @param size Page size (default 20)
     * @return Page of jobs
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<JobQueue>> getJobsByStatus(
            @PathVariable JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("API: Get jobs by status: status={}, page={}, size={}", status, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<JobQueue> jobs = jobQueueService.getJobsByStatus(status, pageable);
        
        log.debug("Retrieved {} jobs for status={}, totalPages={}", 
                 jobs.getNumberOfElements(), status, jobs.getTotalPages());
        
        return ResponseEntity.ok(jobs);
    }

    /**
     * Cancel a pending or processing job.
     * 
     * @param jobId Job UUID
     * @return Success response
     */
    @PostMapping("/{jobId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelJob(@PathVariable UUID jobId) {
        log.info("API: Cancel job: jobId={}", jobId);
        
        boolean cancelled = jobQueueService.cancelJob(jobId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("jobId", jobId);
        response.put("cancelled", cancelled);
        
        if (cancelled) {
            log.info("Job cancelled successfully: jobId={}", jobId);
            response.put("message", "Job cancelled successfully");
            return ResponseEntity.ok(response);
        } else {
            log.warn("Failed to cancel job: jobId={}", jobId);
            response.put("message", "Job cannot be cancelled (not found or already completed/failed)");
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Manually trigger stale job detection and reset.
     * 
     * @return Number of jobs reset
     */
    @PostMapping("/stale/reset")
    public ResponseEntity<Map<String, Object>> resetStaleJobs() {
        log.info("API: Manual stale job reset triggered");
        
        int resetCount = jobQueueService.resetStaleJobs();
        
        Map<String, Object> response = new HashMap<>();
        response.put("resetCount", resetCount);
        response.put("message", resetCount > 0 
                ? String.format("Reset %d stale jobs", resetCount)
                : "No stale jobs found");
        
        log.info("Stale job reset complete: resetCount={}", resetCount);
        return ResponseEntity.ok(response);
    }

    /**
     * Get recent job metrics for dashboard.
     * 
     * @param hours Number of hours to look back (default 24)
     * @return Job metrics
     */
    @GetMapping("/metrics/recent")
    public ResponseEntity<Map<String, Object>> getRecentMetrics(
            @RequestParam(defaultValue = "24") int hours) {
        
        log.debug("API: Get recent metrics: hours={}", hours);
        
        java.time.LocalDateTime startTime = java.time.LocalDateTime.now().minusHours(hours);
        java.time.LocalDateTime endTime = java.time.LocalDateTime.now();
        
        long totalJobs = jobQueueService.countJobsInTimeRange(startTime, endTime);
        long completedJobs = jobQueueService.countCompletedJobsInTimeRange(startTime, endTime);
        long failedJobs = jobQueueService.countFailedJobsInTimeRange(startTime, endTime);
        
        double successRate = totalJobs > 0 ? (completedJobs * 100.0 / totalJobs) : 0.0;
        double failureRate = totalJobs > 0 ? (failedJobs * 100.0 / totalJobs) : 0.0;
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("timeRange", Map.of("startTime", startTime, "endTime", endTime, "hours", hours));
        metrics.put("totalJobs", totalJobs);
        metrics.put("completedJobs", completedJobs);
        metrics.put("failedJobs", failedJobs);
        metrics.put("successRate", String.format("%.2f%%", successRate));
        metrics.put("failureRate", String.format("%.2f%%", failureRate));
        metrics.put("averageProcessingDuration", jobQueueService.getAverageProcessingDuration(JobType.RESUME_PROCESSING));
        
        log.debug("Recent metrics: totalJobs={}, completedJobs={}, failedJobs={}, successRate={:.2f}%",
                 totalJobs, completedJobs, failedJobs, successRate);
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * Cleanup old completed jobs.
     * 
     * @param daysToKeep Number of days to keep (default 30)
     * @return Number of jobs deleted
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupOldJobs(
            @RequestParam(defaultValue = "30") int daysToKeep) {
        
        log.info("API: Manual cleanup triggered: daysToKeep={}", daysToKeep);
        
        int deletedCount = jobQueueService.cleanupOldJobs(daysToKeep);
        
        Map<String, Object> response = new HashMap<>();
        response.put("deletedCount", deletedCount);
        response.put("daysToKeep", daysToKeep);
        response.put("message", String.format("Deleted %d old completed jobs", deletedCount));
        
        log.info("Cleanup complete: deletedCount={}", deletedCount);
        return ResponseEntity.ok(response);
    }

    /**
     * Get queue depth for monitoring.
     * 
     * @return Queue depth by job type
     */
    @GetMapping("/queue/depth")
    public ResponseEntity<Map<String, Object>> getQueueDepth() {
        log.debug("API: Get queue depth");
        
        long resumeProcessingDepth = jobQueueService.getQueueDepth(JobType.RESUME_PROCESSING);
        
        Map<String, Object> depth = new HashMap<>();
        depth.put("RESUME_PROCESSING", resumeProcessingDepth);
        depth.put("total", resumeProcessingDepth);
        
        log.debug("Queue depth: {}", depth);
        return ResponseEntity.ok(depth);
    }
}
