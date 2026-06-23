# Scheduler Implementation Complete

## Overview
Successfully implemented a scheduler-based job queue system for resume processing with feature flag control and comprehensive logging/audit capabilities.

## What Was Implemented

### 1. Database Layer
- **Migration Script**: `V2__add_job_queue.sql`
  - `job_queue` table: Main queue with JSONB metadata, priority, retry logic
  - `job_dead_letter_queue` table: Failed jobs that exceeded max retries
  - `job_metrics` table: Aggregated metrics for monitoring
  - Indexes for performance optimization
  - Triggers for auto-timestamping
  - Updated `process_tracker` table with `job_id` and `correlation_id` columns

### 2. Model Enums
- **JobType.java**: Job types (RESUME_PROCESSING, BATCH_EMBEDDING, DATA_MIGRATION)
- **JobStatus.java**: Job lifecycle states (PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED)
- **JobPriority.java**: Priority levels (LOW=0, NORMAL=1, HIGH=2, URGENT=3)

### 3. Entities
- **JobQueue.java**: JPA entity with:
  - Optimistic locking (@Version)
  - Lifecycle callbacks (@PrePersist, @PreUpdate)
  - Helper methods: markAsProcessing(), markAsCompleted(), markAsFailed(), resetForRetry()
  - Comprehensive logging at all lifecycle points (debug, trace, info, warn, error levels)
  - Heartbeat tracking for stale job detection
  
- **ProcessTracker.java**: Updated with:
  - `jobId` field linking to job_queue
  - `correlationId` field for grouping related jobs

### 4. Repository Layer
- **JobQueueRepository.java**: Custom queries including:
  - Pessimistic locking for atomic job claiming
  - Stale job detection
  - Metrics aggregation
  - Correlation ID grouping
  - JSONB metadata querying

### 5. Service Layer
- **JobQueueService.java**: Business logic for:
  - Job creation (immediate and scheduled)
  - Job claiming with pessimistic locking
  - Heartbeat updates
  - Status management (completed, failed, cancelled)
  - Stale job detection and reset
  - Metrics calculation
  - Old job cleanup

- **JobSchedulerService.java**: Scheduled tasks:
  - Main job processor (@Scheduled every 5 seconds by default)
  - Stale job checker (every minute)
  - Old job cleanup (daily at 2 AM)
  - Queue metrics logging (every 5 minutes)
  - Only active when `app.scheduler.enabled=true`

- **ResumeJobProcessor.java**: Job processing logic:
  - Wraps existing resume processing workflow
  - Sends heartbeats during long operations
  - Intelligent error handling (retryable vs non-retryable)
  - Updates both job_queue and process_tracker
  - Detailed logging at each step

- **FileUploadService.java**: Refactored with feature flag:
  - Checks `app.scheduler.enabled` flag
  - If true: Creates jobs in queue
  - If false: Uses legacy async processing
  - Sets correlation IDs for batch tracking

### 6. Configuration
- **SchedulerConfig.java**: Spring configuration:
  - Configures ExecutorService for job processing
  - Enables @Scheduled annotations
  - Only active when scheduler is enabled
  - Logs configuration on startup

- **application.yml**: New scheduler section:
  ```yaml
  scheduler:
    enabled: false  # Feature flag
    poll-interval-ms: 5000
    batch-size: 5
    thread-pool-size: 5
    worker-id: worker-1
    stale-job-threshold-minutes: 15
    stale-check-interval-ms: 60000
    cleanup-cron: 0 0 2 * * ?
    cleanup-retention-days: 30
    metrics-log-interval-ms: 300000
  ```

### 7. REST API
- **JobQueueController.java**: Monitoring endpoints:
  - `GET /api/jobs/health` - Queue health summary
  - `GET /api/jobs/stats` - Comprehensive statistics
  - `GET /api/jobs/{jobId}` - Get specific job
  - `GET /api/jobs/correlation/{correlationId}` - Get related jobs
  - `GET /api/jobs/status/{status}` - Get jobs by status (paginated)
  - `POST /api/jobs/{jobId}/cancel` - Cancel a job
  - `POST /api/jobs/stale/reset` - Manually reset stale jobs
  - `GET /api/jobs/metrics/recent` - Recent job metrics
  - `POST /api/jobs/cleanup` - Manual cleanup
  - `GET /api/jobs/queue/depth` - Current queue depth

## Feature Flag Control

The entire scheduler system is controlled by a single feature flag: **`app.scheduler.enabled`**

### To Enable Scheduler Mode:
```yaml
# application.yml
app:
  scheduler:
    enabled: true
```
Or set environment variable:
```bash
SCHEDULER_ENABLED=true
```

### To Use Legacy Async Mode:
```yaml
# application.yml
app:
  scheduler:
    enabled: false  # Default
```

### What Happens When Enabled:
1. `@EnableScheduling` activates
2. `JobSchedulerService` starts polling for jobs
3. `FileUploadService` creates jobs in queue instead of calling async methods
4. `JobQueueController` REST endpoints become available
5. `SchedulerConfig` creates thread pool for processing

### What Happens When Disabled:
1. Scheduler components are not loaded (@ConditionalOnProperty)
2. `FileUploadService` uses existing async processing
3. No job queue overhead
4. System works exactly as before

## Logging & Audit Trail

### Logging Levels
- **TRACE**: Heartbeat updates, individual job status checks
- **DEBUG**: Job claiming, queue depth checks, state transitions
- **INFO**: Job lifecycle events (created, started, completed), metrics
- **WARN**: Retries, stale jobs, cancellations
- **ERROR**: Permanent failures, exceptions

### Key Metrics Logged
1. **Job creation**: jobId, type, priority, correlation ID, file size
2. **Job claiming**: Number of jobs claimed, worker ID
3. **Job processing**: Processing duration, retry count
4. **Job completion**: Success/failure, error messages, final status
5. **Queue metrics**: Pending, processing, completed, failed counts
6. **Performance**: Average processing duration, throughput

### Database Audit
- All job state changes stored in `job_queue` table
- Failed jobs moved to `job_dead_letter_queue` with full context
- Aggregated metrics in `job_metrics` table
- Timestamps: created_at, started_at, completed_at
- Retry tracking: retry_count, max_retries
- Error details: error_message column

## How to Test

### 1. Enable Scheduler
```yaml
# application.yml
app:
  scheduler:
    enabled: true
```

### 2. Start Application
```bash
mvn spring-boot:run
```

Look for startup logs:
```
=============================================================
  JOB SCHEDULER CONFIGURATION
=============================================================
  Scheduler Enabled: true
  Thread Pool Size: 5
  Worker ID: worker-1
=============================================================
```

### 3. Upload a Resume
Use existing upload UI or API:
```bash
curl -X POST http://localhost:8080/api/upload \
  -F "file=@resume.pdf"
```

### 4. Monitor Queue
```bash
# Check queue health
curl http://localhost:8080/api/jobs/health

# Check statistics
curl http://localhost:8080/api/jobs/stats

# Check recent metrics
curl http://localhost:8080/api/jobs/metrics/recent?hours=1
```

### 5. Check Logs
```
2025-01-15 10:00:00 - Creating new job: type=RESUME_PROCESSING, priority=NORMAL
2025-01-15 10:00:00 - Job created successfully: jobId=..., type=RESUME_PROCESSING, status=PENDING
2025-01-15 10:00:05 - Claimed 1 jobs for processing
2025-01-15 10:00:05 - Starting resume job processing: jobId=..., priority=NORMAL
2025-01-15 10:00:10 - Text extraction complete: jobId=..., contentLength=5432 chars
2025-01-15 10:00:25 - AI analysis complete: jobId=..., candidateName=John Doe
2025-01-15 10:00:30 - Resume job processing completed successfully: jobId=..., duration=25s
```

## Performance Tuning

### Adjust Poll Interval
Faster polling = lower latency, higher CPU usage:
```yaml
scheduler:
  poll-interval-ms: 2000  # Check every 2 seconds
```

### Adjust Batch Size
Larger batches = higher throughput, more memory:
```yaml
scheduler:
  batch-size: 10  # Process 10 jobs per poll
```

### Adjust Thread Pool
More threads = higher concurrency, more resources:
```yaml
scheduler:
  thread-pool-size: 10  # 10 concurrent jobs
```

### Adjust Stale Job Threshold
Shorter threshold = faster recovery, more false positives:
```yaml
scheduler:
  stale-job-threshold-minutes: 10  # Jobs stale after 10 min
```

## Migration Path

### Phase 1: Testing (Current)
- Deploy with `scheduler.enabled=false` (default)
- No impact on production
- Run in staging with `scheduler.enabled=true`
- Monitor metrics and logs

### Phase 2: Canary Release
- Enable scheduler for small percentage of traffic
- Use load balancer or feature flag service
- Compare performance with async mode
- Monitor error rates and processing times

### Phase 3: Full Rollout
- Enable scheduler for all traffic: `scheduler.enabled=true`
- Monitor queue depth and processing times
- Keep async code as fallback (don't delete yet)

### Phase 4: Cleanup
- After successful rollout (e.g., 30 days)
- Remove async processing code from ResumeProcessingService
- Remove feature flag checks from FileUploadService
- Scheduler becomes the only processing mode

## Files Created/Modified

### Created Files (13):
1. `src/main/resources/db/migration/V2__add_job_queue.sql`
2. `src/main/java/io/subbu/ai/firedrill/models/JobType.java`
3. `src/main/java/io/subbu/ai/firedrill/models/JobStatus.java`
4. `src/main/java/io/subbu/ai/firedrill/models/JobPriority.java`
5. `src/main/java/io/subbu/ai/firedrill/entities/JobQueue.java`
6. `src/main/java/io/subbu/ai/firedrill/repositories/JobQueueRepository.java`
7. `src/main/java/io/subbu/ai/firedrill/services/JobQueueService.java`
8. `src/main/java/io/subbu/ai/firedrill/services/JobSchedulerService.java`
9. `src/main/java/io/subbu/ai/firedrill/services/ResumeJobProcessor.java`
10. `src/main/java/io/subbu/ai/firedrill/config/SchedulerConfig.java`
11. `src/main/java/io/subbu/ai/firedrill/controllers/JobQueueController.java`
12. `docs/SCHEDULER-IMPLEMENTATION-SUMMARY.md` (this file)

### Modified Files (2):
1. `src/main/java/io/subbu/ai/firedrill/entities/ProcessTracker.java`
2. `src/main/java/io/subbu/ai/firedrill/services/FileUploadService.java`
3. `src/main/resources/application.yml`

## Next Steps

1. **Run Database Migration**: Flyway will auto-run V2__add_job_queue.sql on next startup
2. **Test in Development**: Start app with scheduler enabled, upload test resumes
3. **Review Logs**: Check that all logging levels are appropriate
4. **Monitor Metrics**: Use /api/jobs/stats endpoint to verify queue health
5. **Load Test**: Upload batch of resumes, verify processing throughput
6. **Tune Configuration**: Adjust poll interval, batch size, thread pool as needed
7. **Deploy to Staging**: Test with production-like data volume
8. **Gradual Rollout**: Use feature flag for controlled production deployment

## Support

All components include comprehensive logging. If issues arise:
1. Check application logs for ERROR and WARN messages
2. Query `job_queue` table for stuck jobs
3. Use `/api/jobs/stats` for queue health
4. Check `/api/jobs/status/FAILED` for failed jobs
5. Review `job_dead_letter_queue` for permanently failed jobs

## Success Criteria

The implementation will be considered successful when:
- ✅ Database migration runs without errors
- ✅ Jobs are created when files are uploaded (scheduler.enabled=true)
- ✅ Scheduler polls and processes jobs
- ✅ All logging statements appear in logs
- ✅ Jobs complete successfully with status=COMPLETED
- ✅ Failed jobs retry appropriately
- ✅ Stale jobs are detected and reset
- ✅ Metrics endpoints return valid data
- ✅ Processing time is comparable to async mode
- ✅ No jobs are lost or stuck indefinitely
