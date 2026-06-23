# Scheduler-Based Resume Processing: Implementation Guide

## Overview

This document provides a comprehensive, step-by-step guide to implementing a scheduler-based resume processing system. It includes complete code examples, database schemas, configuration details, and migration strategies.

**Target Audience:** Development team implementing the scheduler-based approach  
**Prerequisites:** Understanding of Spring Boot, PostgreSQL, and async processing concepts  
**Estimated Implementation Time:** 1-2 weeks

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Database Design](#database-design)
3. [Backend Implementation](#backend-implementation)
4. [Configuration](#configuration)
5. [Frontend Changes](#frontend-changes)
6. [Testing Strategy](#testing-strategy)
7. [Deployment Guide](#deployment-guide)
8. [Migration Path](#migration-path)
9. [Monitoring & Operations](#monitoring--operations)

---

## Architecture Overview

### High-Level Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                         FRONTEND (React)                        │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐ │
│  │ File Upload  │  │ Status Poll  │  │  Tracker Dashboard   │ │
│  │  Component   │  │   (2s int)   │  │                      │ │
│  └──────────────┘  └──────────────┘  └──────────────────────┘ │
└────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTPS
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                       REST API LAYER                            │
│  ┌──────────────────┐  ┌──────────────────┐                   │
│  │ Upload Endpoint  │  │ Status Endpoint  │                   │
│  │ POST /upload     │  │ GET /tracker/:id │                   │
│  └──────────────────┘  └──────────────────┘                   │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                      SERVICE LAYER                              │
│  ┌─────────────────┐  ┌──────────────────┐                    │
│  │ FileUpload      │  │  JobQueue        │                    │
│  │ Service         │  │  Service         │                    │
│  └─────────────────┘  └──────────────────┘                    │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                      SCHEDULER LAYER                            │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  JobSchedulerService (@Scheduled every 10s)              │ │
│  │                                                           │ │
│  │  1. Fetch pending jobs (batch of 10)                     │ │
│  │  2. Claim jobs (assign to worker)                        │ │
│  │  3. Process in parallel (ExecutorService)                │ │
│  │  4. Update status                                         │ │
│  └──────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                     PROCESSING LAYER                            │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │  ResumeJobProcessor                                       │ │
│  │                                                           │ │
│  │  Job → Parse File → AI Analysis → Save Candidate →      │ │
│  │        Generate Embeddings → Update Status               │ │
│  └──────────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                      DATA LAYER                                 │
│  ┌────────────┐  ┌──────────────┐  ┌─────────────────────┐   │
│  │ job_queue  │  │process_tracker│  │   candidates        │   │
│  │  (Queue)   │  │ (User Status) │  │  (Results)          │   │
│  └────────────┘  └──────────────┘  └─────────────────────┘   │
│                                                                 │
│                      PostgreSQL Database                        │
└────────────────────────────────────────────────────────────────┘
```

### Key Components

| Component | Purpose | Technologies |
|-----------|---------|--------------|
| **Job Queue** | Persistent queue for pending work | PostgreSQL table |
| **Scheduler** | Periodic job pickup and dispatch | Spring @Scheduled |
| **Executor Service** | Parallel job processing | Java ExecutorService |
| **Job Processor** | Resume processing logic | Spring Service |
| **Process Tracker** | User-facing status updates | JPA Entity |

### Processing Flow

```
1. User uploads file(s)
        ↓
2. FileUploadService validates and creates Job records (PENDING status)
        ↓
3. JobSchedulerService runs every 10s
        ↓
4. Scheduler queries for PENDING jobs (batch of 10)
        ↓
5. Jobs are claimed and marked PROCESSING
        ↓
6. Jobs processed in parallel using ExecutorService
        ↓
7. For each job:
   - Parse file → AI analysis → Save candidate → Generate embeddings
        ↓
8. Job marked COMPLETED or FAILED
        ↓
9. ProcessTracker updated for user visibility
        ↓
10. Frontend polls ProcessTracker for updates
```

---

## Database Design

### Schema Changes

#### 1. Create Job Queue Table

```sql
-- Drop existing table if migrating
DROP TABLE IF EXISTS job_queue CASCADE;

-- Main job queue table
CREATE TABLE job_queue (
    -- Primary key
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Job identification
    job_type VARCHAR(50) NOT NULL,
    correlation_id VARCHAR(255),  -- For grouping related jobs
    
    -- Status tracking
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority INT NOT NULL DEFAULT 0,
    
    -- Job data
    file_data BYTEA,
    filename VARCHAR(500),
    metadata JSONB,
    
    -- Retry logic
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    error_message TEXT,
    error_stack_trace TEXT,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scheduled_for TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Worker assignment
    assigned_to VARCHAR(100),
    heartbeat_at TIMESTAMP,
    
    -- Optimistic locking
    version BIGINT DEFAULT 0,
    
    -- Constraints
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_job_type CHECK (job_type IN ('RESUME_PROCESSING', 'BATCH_EMBEDDING', 'DATA_MIGRATION'))
);

-- Indexes for performance
CREATE INDEX idx_job_queue_status_priority ON job_queue(status, priority DESC, created_at ASC)
    WHERE status = 'PENDING';

CREATE INDEX idx_job_queue_scheduled_for ON job_queue(scheduled_for)
    WHERE status = 'PENDING';

CREATE INDEX idx_job_queue_assigned_to ON job_queue(assigned_to)
    WHERE status = 'PROCESSING';

CREATE INDEX idx_job_queue_correlation_id ON job_queue(correlation_id)
    WHERE correlation_id IS NOT NULL;

CREATE INDEX idx_job_queue_created_at ON job_queue(created_at DESC);

-- Trigger to update updated_at
CREATE OR REPLACE FUNCTION update_job_queue_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_job_queue_updated_at
    BEFORE UPDATE ON job_queue
    FOR EACH ROW
    EXECUTE FUNCTION update_job_queue_timestamp();

-- Add comments
COMMENT ON TABLE job_queue IS 'Queue for asynchronous job processing';
COMMENT ON COLUMN job_queue.priority IS 'Higher number = higher priority (0=normal, 1=high, 2=urgent)';
COMMENT ON COLUMN job_queue.metadata IS 'JSON data specific to job type';
COMMENT ON COLUMN job_queue.correlation_id IS 'Groups related jobs (e.g., all files in one upload)';
COMMENT ON COLUMN job_queue.heartbeat_at IS 'Last heartbeat from worker processing this job';
```

#### 2. Modify Process Tracker Table

```sql
-- Add job_id reference to existing process_tracker
ALTER TABLE process_tracker 
    ADD COLUMN job_id UUID,
    ADD COLUMN correlation_id VARCHAR(255),
    ADD CONSTRAINT fk_process_tracker_job 
        FOREIGN KEY (job_id) REFERENCES job_queue(id) ON DELETE SET NULL;

CREATE INDEX idx_process_tracker_job_id ON process_tracker(job_id);
CREATE INDEX idx_process_tracker_correlation_id ON process_tracker(correlation_id);

COMMENT ON COLUMN process_tracker.job_id IS 'Links to job_queue for internal tracking';
COMMENT ON COLUMN process_tracker.correlation_id IS 'Groups multiple trackers (batch uploads)';
```

#### 3. Job History Archive Table (Optional)

```sql
-- Archive completed jobs for analytics
CREATE TABLE job_queue_history (
    LIKE job_queue INCLUDING ALL
);

CREATE INDEX idx_job_history_completed_at ON job_queue_history(completed_at DESC);
CREATE INDEX idx_job_history_status ON job_queue_history(status);

-- Partition by month for better performance
CREATE TABLE job_queue_history_2026_02 PARTITION OF job_queue_history
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
```

#### 4. Dead Letter Queue Table

```sql
-- For jobs that failed all retries
CREATE TABLE job_dead_letter_queue (
    id UUID PRIMARY KEY,
    original_job_id UUID NOT NULL,
    job_type VARCHAR(50) NOT NULL,
    failed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failure_reason TEXT,
    job_data JSONB,
    retry_attempts INT,
    notify_sent BOOLEAN DEFAULT FALSE,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(100),
    resolution_notes TEXT
);

CREATE INDEX idx_dlq_failed_at ON job_dead_letter_queue(failed_at DESC);
CREATE INDEX idx_dlq_resolved ON job_dead_letter_queue(resolved, failed_at DESC);
```

### Database Migration Script

```sql
-- Complete migration script
-- File: src/main/resources/db/migration/V2__add_job_queue.sql

BEGIN;

-- 1. Create job queue table
CREATE TABLE job_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_type VARCHAR(50) NOT NULL,
    correlation_id VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    priority INT NOT NULL DEFAULT 0,
    file_data BYTEA,
    filename VARCHAR(500),
    metadata JSONB,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    error_message TEXT,
    error_stack_trace TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    scheduled_for TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_to VARCHAR(100),
    heartbeat_at TIMESTAMP,
    version BIGINT DEFAULT 0,
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_job_type CHECK (job_type IN ('RESUME_PROCESSING', 'BATCH_EMBEDDING', 'DATA_MIGRATION'))
);

CREATE INDEX idx_job_queue_status_priority ON job_queue(status, priority DESC, created_at ASC)
    WHERE status = 'PENDING';
CREATE INDEX idx_job_queue_scheduled_for ON job_queue(scheduled_for)
    WHERE status = 'PENDING';
CREATE INDEX idx_job_queue_assigned_to ON job_queue(assigned_to)
    WHERE status = 'PROCESSING';
CREATE INDEX idx_job_queue_correlation_id ON job_queue(correlation_id)
    WHERE correlation_id IS NOT NULL;
CREATE INDEX idx_job_queue_created_at ON job_queue(created_at DESC);

-- 2. Update trigger
CREATE OR REPLACE FUNCTION update_job_queue_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_job_queue_updated_at
    BEFORE UPDATE ON job_queue
    FOR EACH ROW
    EXECUTE FUNCTION update_job_queue_timestamp();

-- 3. Modify process_tracker
ALTER TABLE process_tracker 
    ADD COLUMN job_id UUID,
    ADD COLUMN correlation_id VARCHAR(255);

ALTER TABLE process_tracker
    ADD CONSTRAINT fk_process_tracker_job 
        FOREIGN KEY (job_id) REFERENCES job_queue(id) ON DELETE SET NULL;

CREATE INDEX idx_process_tracker_job_id ON process_tracker(job_id);
CREATE INDEX idx_process_tracker_correlation_id ON process_tracker(correlation_id);

-- 4. Dead letter queue
CREATE TABLE job_dead_letter_queue (
    id UUID PRIMARY KEY,
    original_job_id UUID NOT NULL,
    job_type VARCHAR(50) NOT NULL,
    failed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failure_reason TEXT,
    job_data JSONB,
    retry_attempts INT,
    notify_sent BOOLEAN DEFAULT FALSE,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(100),
    resolution_notes TEXT
);

CREATE INDEX idx_dlq_failed_at ON job_dead_letter_queue(failed_at DESC);
CREATE INDEX idx_dlq_resolved ON job_dead_letter_queue(resolved, failed_at DESC);

COMMIT;
```

---

## Backend Implementation

### Step 1: Entity Classes

#### JobQueue Entity

**File:** `src/main/java/io/subbu/ai/firedrill/entities/JobQueue.java`

```java
package io.subbu.ai.firedrill.entities;

import io.subbu.ai.firedrill.models.JobStatus;
import io.subbu.ai.firedrill.models.JobType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing a job in the processing queue.
 * Jobs are picked up by schedulers and processed asynchronously.
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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if job can be retried
     */
    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    /**
     * Increment retry count
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * Mark job as processing
     */
    public void markAsProcessing(String workerId) {
        this.status = JobStatus.PROCESSING;
        this.assignedTo = workerId;
        this.startedAt = LocalDateTime.now();
        this.heartbeatAt = LocalDateTime.now();
    }

    /**
     * Mark job as completed
     */
    public void markAsCompleted() {
        this.status = JobStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Mark job as failed
     */
    public void markAsFailed(Exception error) {
        this.status = JobStatus.FAILED;
        this.errorMessage = error.getMessage();
        this.errorStackTrace = getStackTraceAsString(error);
        this.completedAt = LocalDateTime.now();
    }

    /**
     * Reset for retry
     */
    public void resetForRetry() {
        this.status = JobStatus.PENDING;
        this.assignedTo = null;
        this.startedAt = null;
        this.heartbeatAt = null;
        this.scheduledFor = LocalDateTime.now().plusMinutes(5); // Delay retry
        incrementRetryCount();
    }

    /**
     * Update heartbeat
     */
    public void updateHeartbeat() {
        this.heartbeatAt = LocalDateTime.now();
    }

    /**
     * Check if job is stale (no heartbeat for 10 minutes)
     */
    public boolean isStale() {
        if (status != JobStatus.PROCESSING || heartbeatAt == null) {
            return false;
        }
        return heartbeatAt.isBefore(LocalDateTime.now().minusMinutes(10));
    }

    private String getStackTraceAsString(Exception error) {
        if (error == null) return null;
        
        StringBuilder sb = new StringBuilder();
        sb.append(error.toString()).append("\n");
        for (StackTraceElement element : error.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        
        Throwable cause = error.getCause();
        if (cause != null) {
            sb.append("Caused by: ").append(cause.toString()).append("\n");
            for (StackTraceElement element : cause.getStackTrace()) {
                sb.append("\tat ").append(element.toString()).append("\n");
            }
        }
        
        return sb.toString();
    }
}
```

#### Model Enums

**File:** `src/main/java/io/subbu/ai/firedrill/models/JobType.java`

```java
package io.subbu.ai.firedrill.models;

/**
 * Types of jobs that can be queued
 */
public enum JobType {
    RESUME_PROCESSING,
    BATCH_EMBEDDING,
    DATA_MIGRATION,
    CLEANUP
}
```

**File:** `src/main/java/io/subbu/ai/firedrill/models/JobStatus.java`

```java
package io.subbu.ai.firedrill.models;

/**
 * Status of a job in the queue
 */
public enum JobStatus {
    PENDING,      // Waiting to be picked up
    PROCESSING,   // Currently being processed
    COMPLETED,    // Successfully completed
    FAILED,       // Failed after all retries
    CANCELLED     // Manually cancelled
}
```

**File:** `src/main/java/io/subbu/ai/firedrill/models/JobPriority.java`

```java
package io.subbu.ai.firedrill.models;

/**
 * Priority levels for jobs
 */
public enum JobPriority {
    LOW(0),
    NORMAL(1),
    HIGH(2),
    URGENT(3);

    private final int value;

    JobPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
```

### Step 2: Repository Layer

#### JobQueueRepository

**File:** `src/main/java/io/subbu/ai/firedrill/repos/JobQueueRepository.java`

```java
package io.subbu.ai.firedrill.repos;

import io.subbu.ai.firedrill.entities.JobQueue;
import io.subbu.ai.firedrill.models.JobStatus;
import io.subbu.ai.firedrill.models.JobType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for job queue operations
 */
@Repository
public interface JobQueueRepository extends JpaRepository<JobQueue, UUID> {

    /**
     * Find pending jobs ordered by priority and creation time
     * Uses pessimistic write lock to prevent concurrent pickup
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT j FROM JobQueue j
        WHERE j.status = 'PENDING'
        AND j.scheduledFor <= :now
        ORDER BY j.priority DESC, j.createdAt ASC
        LIMIT :limit
        """)
    List<JobQueue> findAndLockPendingJobs(
        @Param("now") LocalDateTime now,
        @Param("limit") int limit
    );

    /**
     * Find jobs by correlation ID
     */
    List<JobQueue> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);

    /**
     * Find jobs by status
     */
    List<JobQueue> findByStatusOrderByCreatedAtDesc(JobStatus status);

    /**
     * Find jobs by status and type
     */
    List<JobQueue> findByStatusAndJobTypeOrderByCreatedAtDesc(JobStatus status, JobType jobType);

    /**
     * Count jobs by status
     */
    long countByStatus(JobStatus status);

    /**
     * Find stale jobs (processing but no heartbeat for 10+ minutes)
     */
    @Query("""
        SELECT j FROM JobQueue j
        WHERE j.status = 'PROCESSING'
        AND j.heartbeatAt < :threshold
        """)
    List<JobQueue> findStaleJobs(@Param("threshold") LocalDateTime threshold);

    /**
     * Find jobs for cleanup (completed/failed older than retention period)
     */
    @Query("""
        SELECT j FROM JobQueue j
        WHERE j.status IN ('COMPLETED', 'FAILED')
        AND j.completedAt < :threshold
        """)
    List<JobQueue> findJobsForCleanup(@Param("threshold") LocalDateTime threshold);

    /**
     * Delete jobs by IDs (for archiving)
     */
    @Modifying
    @Query("DELETE FROM JobQueue j WHERE j.id IN :ids")
    void deleteByIdIn(@Param("ids") List<UUID> ids);

    /**
     * Find jobs assigned to a worker
     */
    List<JobQueue> findByAssignedToAndStatus(String workerId, JobStatus status);

    /**
     * Get job statistics
     */
    @Query("""
        SELECT j.status as status, COUNT(j) as count
        FROM JobQueue j
        GROUP BY j.status
        """)
    List<Object[]> getJobStatistics();

    /**
     * Get average processing time
     */
    @Query("""
        SELECT AVG(TIMESTAMPDIFF(SECOND, j.startedAt, j.completedAt))
        FROM JobQueue j
        WHERE j.status = 'COMPLETED'
        AND j.completedAt > :since
        """)
    Double getAverageProcessingTime(@Param("since") LocalDateTime since);
}
```

### Step 3: Service Layer

#### JobQueueService

**File:** `src/main/java/io/subbu/ai/firedrill/services/JobQueueService.java`

```java
package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.JobQueue;
import io.subbu.ai.firedrill.models.JobPriority;
import io.subbu.ai.firedrill.models.JobStatus;
import io.subbu.ai.firedrill.models.JobType;
import io.subbu.ai.firedrill.repos.JobQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing job queue operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobQueueService {

    private final JobQueueRepository jobQueueRepository;

    /**
     * Create a new job in the queue
     *
     * @param jobType Type of job
     * @param fileData Binary file data
     * @param filename Original filename
     * @param metadata Additional metadata
     * @param priority Job priority
     * @return Created job
     */
    @Transactional
    public JobQueue createJob(
        JobType jobType,
        byte[] fileData,
        String filename,
        Map<String, Object> metadata,
        JobPriority priority
    ) {
        JobQueue job = JobQueue.builder()
            .jobType(jobType)
            .fileData(fileData)
            .filename(filename)
            .metadata(metadata)
            .priority(priority.getValue())
            .status(JobStatus.PENDING)
            .scheduledFor(LocalDateTime.now())
            .build();

        job = jobQueueRepository.save(job);
        log.info("Created job {} of type {} with priority {}", 
            job.getId(), jobType, priority);
        
        return job;
    }

    /**
     * Create a job with correlation ID (for batch processing)
     */
    @Transactional
    public JobQueue createJobWithCorrelation(
        JobType jobType,
        byte[] fileData,
        String filename,
        Map<String, Object> metadata,
        JobPriority priority,
        String correlationId
    ) {
        JobQueue job = JobQueue.builder()
            .jobType(jobType)
            .fileData(fileData)
            .filename(filename)
            .metadata(metadata)
            .priority(priority.getValue())
            .correlationId(correlationId)
            .status(JobStatus.PENDING)
            .scheduledFor(LocalDateTime.now())
            .build();

        return jobQueueRepository.save(job);
    }

    /**
     * Fetch and lock pending jobs for processing
     *
     * @param batchSize Maximum number of jobs to fetch
     * @return List of locked jobs
     */
    @Transactional
    public List<JobQueue> fetchAndLockPendingJobs(int batchSize) {
        return jobQueueRepository.findAndLockPendingJobs(
            LocalDateTime.now(),
            batchSize
        );
    }

    /**
     * Mark job as processing
     */
    @Transactional
    public void markAsProcessing(JobQueue job, String workerId) {
        job.markAsProcessing(workerId);
        jobQueueRepository.save(job);
        log.debug("Job {} assigned to worker {}", job.getId(), workerId);
    }

    /**
     * Mark job as completed
     */
    @Transactional
    public void markAsCompleted(JobQueue job) {
        job.markAsCompleted();
        jobQueueRepository.save(job);
        log.info("Job {} completed successfully", job.getId());
    }

    /**
     * Handle job failure with retry logic
     */
    @Transactional
    public void handleFailure(JobQueue job, Exception error) {
        log.error("Job {} failed: {}", job.getId(), error.getMessage(), error);

        if (job.canRetry()) {
            job.resetForRetry();
            jobQueueRepository.save(job);
            log.info("Job {} will be retried (attempt {}/{})", 
                job.getId(), job.getRetryCount(), job.getMaxRetries());
        } else {
            job.markAsFailed(error);
            jobQueueRepository.save(job);
            log.error("Job {} failed permanently after {} retries", 
                job.getId(), job.getMaxRetries());
        }
    }

    /**
     * Update job heartbeat
     */
    @Transactional
    public void updateHeartbeat(UUID jobId) {
        jobQueueRepository.findById(jobId).ifPresent(job -> {
            job.updateHeartbeat();
            jobQueueRepository.save(job);
        });
    }

    /**
     * Get job by ID
     */
    public JobQueue getJob(UUID jobId) {
        return jobQueueRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
    }

    /**
     * Get jobs by correlation ID
     */
    public List<JobQueue> getJobsByCorrelationId(String correlationId) {
        return jobQueueRepository.findByCorrelationIdOrderByCreatedAtAsc(correlationId);
    }

    /**
     * Get queue statistics
     */
    public Map<String, Long> getQueueStatistics() {
        return Map.of(
            "pending", jobQueueRepository.countByStatus(JobStatus.PENDING),
            "processing", jobQueueRepository.countByStatus(JobStatus.PROCESSING),
            "completed", jobQueueRepository.countByStatus(JobStatus.COMPLETED),
            "failed", jobQueueRepository.countByStatus(JobStatus.FAILED)
        );
    }

    /**
     * Recover stale jobs
     */
    @Transactional
    public int recoverStaleJobs() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        List<JobQueue> staleJobs = jobQueueRepository.findStaleJobs(threshold);
        
        for (JobQueue job : staleJobs) {
            log.warn("Recovering stale job {} (worker: {})", 
                job.getId(), job.getAssignedTo());
            
            if (job.canRetry()) {
                job.resetForRetry();
            } else {
                job.markAsFailed(new Exception("Job timed out"));
            }
            jobQueueRepository.save(job);
        }
        
        return staleJobs.size();
    }
}
```

#### JobSchedulerService

**File:** `src/main/java/io/subbu/ai/firedrill/services/JobSchedulerService.java`

```java
package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.JobQueue;
import io.subbu.ai.firedrill.models.JobType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Scheduler service that periodically processes jobs from the queue
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobSchedulerService {

    private final JobQueueService jobQueueService;
    private final ResumeJobProcessor resumeJobProcessor;
    private final ExecutorService executorService;

    @Value("${app.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${app.scheduler.batch-size:10}")
    private int batchSize;

    @Value("${app.scheduler.worker-id:#{null}}")
    private String configuredWorkerId;

    private String workerId;

    /**
     * Main scheduler that picks up and processes jobs
     * Runs every 10 seconds (configurable)
     */
    @Scheduled(
        fixedDelayString = "${app.scheduler.interval:10000}",
        initialDelayString = "${app.scheduler.initial-delay:5000}"
    )
    public void processJobQueue() {
        if (!schedulerEnabled) {
            return;
        }

        try {
            // Fetch pending jobs
            List<JobQueue> pendingJobs = jobQueueService.fetchAndLockPendingJobs(batchSize);

            if (pendingJobs.isEmpty()) {
                log.trace("No pending jobs found");
                return;
            }

            log.info("Processing {} jobs", pendingJobs.size());

            // Process jobs in parallel
            List<CompletableFuture<Void>> futures = pendingJobs.stream()
                .map(job -> CompletableFuture.runAsync(
                    () -> processJob(job),
                    executorService
                ))
                .toList();

            // Wait for all jobs in this batch to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            log.info("Batch processing completed");

        } catch (Exception e) {
            log.error("Error in job scheduler", e);
        }
    }

    /**
     * Recover stale jobs (no heartbeat for 10+ minutes)
     * Runs every 5 minutes
     */
    @Scheduled(fixedDelayString = "${app.scheduler.stale-job-check-interval:300000}")
    public void recoverStaleJobs() {
        if (!schedulerEnabled) {
            return;
        }

        try {
            int recovered = jobQueueService.recoverStaleJobs();
            if (recovered > 0) {
                log.warn("Recovered {} stale jobs", recovered);
            }
        } catch (Exception e) {
            log.error("Error recovering stale jobs", e);
        }
    }

    /**
     * Process a single job
     */
    private void processJob(JobQueue job) {
        String worker = getWorkerId();

        try {
            // Mark as processing
            jobQueueService.markAsProcessing(job, worker);

            // Route to appropriate processor
            switch (job.getJobType()) {
                case RESUME_PROCESSING:
                    resumeJobProcessor.process(job);
                    break;
                case BATCH_EMBEDDING:
                    // Add other processors as needed
                    throw new UnsupportedOperationException("Batch embedding not yet implemented");
                default:
                    throw new IllegalArgumentException("Unknown job type: " + job.getJobType());
            }

            // Mark as completed
            jobQueueService.markAsCompleted(job);

        } catch (Exception e) {
            log.error("Job {} failed: {}", job.getId(), e.getMessage(), e);
            jobQueueService.handleFailure(job, e);
        }
    }

    /**
     * Get worker ID (hostname or configured ID)
     */
    private String getWorkerId() {
        if (workerId == null) {
            if (configuredWorkerId != null && !configuredWorkerId.isBlank()) {
                workerId = configuredWorkerId;
            } else {
                try {
                    workerId = InetAddress.getLocalHost().getHostName();
                } catch (UnknownHostException e) {
                    workerId = "unknown-worker";
                }
            }
        }
        return workerId;
    }
}
```

#### ResumeJobProcessor

**File:** `src/main/java/io/subbu/ai/firedrill/services/ResumeJobProcessor.java`

```java
package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.Candidate;
import io.subbu.ai.firedrill.entities.JobQueue;
import io.subbu.ai.firedrill.entities.ProcessTracker;
import io.subbu.ai.firedrill.models.ProcessStatus;
import io.subbu.ai.firedrill.models.ResumeAnalysisRequest;
import io.subbu.ai.firedrill.models.ResumeAnalysisResponse;
import io.subbu.ai.firedrill.repos.CandidateRepository;
import io.subbu.ai.firedrill.repos.ProcessTrackerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Processor for resume processing jobs
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ResumeJobProcessor {

    private final FileParserService fileParserService;
    private final AIService aiService;
    private final EmbeddingService embeddingService;
    private final CandidateRepository candidateRepository;
    private final ProcessTrackerRepository trackerRepository;
    private final JobQueueService jobQueueService;

    /**
     * Process a resume job
     *
     * @param job The job to process
     * @throws Exception if processing fails
     */
    public void process(JobQueue job) throws Exception {
        log.info("Processing resume job: {} ({})", job.getId(), job.getFilename());

        // Extract job data
        byte[] fileData = job.getFileData();
        String filename = job.getFilename();

        // Get associated tracker if exists
        ProcessTracker tracker = null;
        if (job.getMetadata() != null && job.getMetadata().containsKey("trackerId")) {
            String trackerIdStr = (String) job.getMetadata().get("trackerId");
            UUID trackerId = UUID.fromString(trackerIdStr);
            tracker = trackerRepository.findById(trackerId).orElse(null);
        }

        try {
            // Step 1: Parse resume
            log.debug("Step 1/4: Parsing resume file");
            String resumeContent = fileParserService.extractText(fileData, filename);
            log.debug("Extracted {} characters from {}", resumeContent.length(), filename);
            updateTracker(tracker, ProcessStatus.INITIATED, "Resume parsed");

            // Update heartbeat
            jobQueueService.updateHeartbeat(job.getId());

            // Step 2: AI Analysis
            log.debug("Step 2/4: Analyzing resume with AI");
            ResumeAnalysisRequest request = ResumeAnalysisRequest.builder()
                .resumeContent(resumeContent)
                .filename(filename)
                .build();

            ResumeAnalysisResponse analysis = aiService.analyzeResume(request);
            log.debug("Resume analyzed for: {}", analysis.getName());
            updateTracker(tracker, ProcessStatus.RESUME_ANALYZED, "Resume analyzed");

            // Update heartbeat
            jobQueueService.updateHeartbeat(job.getId());

            // Step 3: Store candidate
            log.debug("Step 3/4: Storing candidate data");
            Candidate candidate = Candidate.builder()
                .name(analysis.getName())
                .email(analysis.getEmail())
                .mobile(analysis.getMobile())
                .resumeFilename(filename)
                .resumeContent(resumeContent)
                .resumeFile(fileData)
                .experienceSummary(analysis.getExperienceSummary())
                .skills(analysis.getSkills())
                .domainKnowledge(analysis.getDomainKnowledge())
                .academicBackground(analysis.getAcademicBackground())
                .yearsOfExperience(analysis.getYearsOfExperience())
                .build();

            candidate = candidateRepository.save(candidate);
            log.info("Candidate saved: {} (ID: {})", candidate.getName(), candidate.getId());

            // Update heartbeat
            jobQueueService.updateHeartbeat(job.getId());

            // Step 4: Generate embeddings
            log.debug("Step 4/4: Generating embeddings");
            embeddingService.generateAndStoreEmbeddings(candidate, resumeContent);
            updateTracker(tracker, ProcessStatus.EMBED_GENERATED, "Embeddings generated");

            // Final update
            updateTracker(tracker, ProcessStatus.COMPLETED, "Processing completed successfully");

            log.info("Resume processing completed for: {} (Job: {})", 
                candidate.getName(), job.getId());

        } catch (Exception e) {
            log.error("Error processing resume job {}: {}", job.getId(), e.getMessage(), e);
            updateTracker(tracker, ProcessStatus.FAILED, "Processing failed: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Update process tracker if it exists
     */
    private void updateTracker(ProcessTracker tracker, ProcessStatus status, String message) {
        if (tracker != null) {
            tracker.updateStatus(status, message);
            if (status == ProcessStatus.COMPLETED || status == ProcessStatus.FAILED) {
                tracker.incrementProcessedFiles();
            }
            trackerRepository.save(tracker);
            log.debug("Updated tracker {} to status: {}", tracker.getId(), status);
        }
    }
}
```

#### Refactored FileUploadService

**File:** `src/main/java/io/subbu/ai/firedrill/services/FileUploadService.java`

```java
package io.subbu.ai.firedrill.services;

import io.subbu.ai.firedrill.entities.JobQueue;
import io.subbu.ai.firedrill.entities.ProcessTracker;
import io.subbu.ai.firedrill.models.JobPriority;
import io.subbu.ai.firedrill.models.JobType;
import io.subbu.ai.firedrill.models.ProcessStatus;
import io.subbu.ai.firedrill.repos.ProcessTrackerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Service for handling file uploads with scheduler-based processing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileUploadService {

    private final JobQueueService jobQueueService;
    private final ProcessTrackerRepository trackerRepository;
    private final FileParserService fileParserService;

    @Value("${app.upload.allowed-extensions:.doc,.docx,.pdf,.zip}")
    private String allowedExtensions;

    /**
     * Handle multiple file upload
     */
    @Transactional
    public UUID handleMultipleFileUpload(List<MultipartFile> files) throws IOException {
        log.info("Handling multiple file upload, count: {}", files.size());

        if (files.isEmpty()) {
            throw new IllegalArgumentException("No files provided");
        }

        // Validate all files
        for (MultipartFile file : files) {
            validateFile(file);
        }

        // Create correlation ID for batch
        String correlationId = UUID.randomUUID().toString();

        // Create process tracker for user-facing status
        ProcessTracker tracker = ProcessTracker.builder()
            .status(ProcessStatus.INITIATED)
            .uploadedFilename(files.size() + " files")
            .correlationId(correlationId)
            .totalFiles(files.size())
            .processedFiles(0)
            .failedFiles(0)
            .message("Queued for processing (" + files.size() + " files)")
            .build();

        tracker = trackerRepository.save(tracker);

        // Create jobs for each file
        for (MultipartFile file : files) {
            Map<String, Object> metadata = Map.of(
                "trackerId", tracker.getId().toString(),
                "correlationId", correlationId,
                "uploadedBy", getCurrentUser(),
                "originalSize", file.getSize()
            );

            JobQueue job = jobQueueService.createJobWithCorrelation(
                JobType.RESUME_PROCESSING,
                file.getBytes(),
                file.getOriginalFilename(),
                metadata,
                JobPriority.NORMAL,
                correlationId
            );

            log.debug("Created job {} for file {}", job.getId(), file.getOriginalFilename());
        }

        log.info("Created {} jobs for batch upload (tracker: {})", 
            files.size(), tracker.getId());

        return tracker.getId();
    }

    /**
     * Handle single or ZIP file upload
     */
    @Transactional
    public UUID handleFileUpload(MultipartFile file) throws IOException {
        log.info("Handling file upload: {}", file.getOriginalFilename());

        // Validate file
        validateFile(file);

        // Create process tracker
        ProcessTracker tracker = ProcessTracker.builder()
            .status(ProcessStatus.INITIATED)
            .uploadedFilename(file.getOriginalFilename())
            .totalFiles(1)
            .processedFiles(0)
            .failedFiles(0)
            .message("Queued for processing")
            .build();

        tracker = trackerRepository.save(tracker);

        // Create job
        Map<String, Object> metadata = Map.of(
            "trackerId", tracker.getId().toString(),
            "uploadedBy", getCurrentUser(),
            "originalSize", file.getSize()
        );

        JobQueue job = jobQueueService.createJob(
            JobType.RESUME_PROCESSING,
            file.getBytes(),
            file.getOriginalFilename(),
            metadata,
            JobPriority.NORMAL
        );

        // Link tracker to job
        tracker.setJobId(job.getId());
        trackerRepository.save(tracker);

        log.info("Created job {} for file {} (tracker: {})", 
            job.getId(), file.getOriginalFilename(), tracker.getId());

        return tracker.getId();
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename is required");
        }

        // Check file extension
        String[] allowed = allowedExtensions.split(",");
        boolean validExtension = Arrays.stream(allowed)
            .anyMatch(ext -> filename.toLowerCase().endsWith(ext.trim()));

        if (!validExtension) {
            throw new IllegalArgumentException(
                "Invalid file type. Allowed: " + allowedExtensions
            );
        }

        // Check file size (50MB max)
        if (file.getSize() > 50_000_000) {
            throw new IllegalArgumentException("File too large. Maximum size: 50MB");
        }
    }

    /**
     * Get current user (placeholder - implement based on your auth system)
     */
    private String getCurrentUser() {
        // TODO: Implement based on your authentication system
        return "system";
    }
}
```

### Step 4: Configuration Class

**File:** `src/main/java/io/subbu/ai/firedrill/config/SchedulerConfig.java`

```java
package io.subbu.ai.firedrill.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for job scheduler
 */
@Configuration
@EnableScheduling
@Slf4j
public class SchedulerConfig {

    @Value("${app.scheduler.thread-pool-size:10}")
    private int threadPoolSize;

    /**
     * Executor service for parallel job processing
     */
    @Bean(name = "jobExecutorService", destroyMethod = "shutdown")
    public ExecutorService jobExecutorService() {
        log.info("Creating job executor service with {} threads", threadPoolSize);
        return Executors.newFixedThreadPool(
            threadPoolSize,
            r -> {
                Thread thread = new Thread(r);
                thread.setName("job-processor-" + thread.getId());
                thread.setDaemon(false);
                return thread;
            }
        );
    }
}
```

---

## Configuration

### application.yml

Add the following configuration to your `application.yml`:

```yaml
app:
  # Scheduler Configuration
  scheduler:
    enabled: true                    # Enable/disable scheduler
    interval: 10000                  # Run every 10 seconds (ms)
    initial-delay: 5000              # Wait 5 seconds after startup
    batch-size: 10                   # Process 10 jobs per cycle
    thread-pool-size: 10             # Parallel processing threads
    worker-id: ${HOSTNAME:worker-1}  # Worker identifier
    stale-job-check-interval: 300000 # Check for stale jobs every 5 minutes
  
  # Job Queue Configuration
  job-queue:
    max-retries: 3                   # Max retry attempts per job
    retry-delay-minutes: 5           # Delay before retry
    cleanup-enabled: true            # Enable automatic cleanup
    cleanup-retention-days: 7        # Keep completed jobs for 7 days
    cleanup-schedule: "0 0 2 * * *"  # Run cleanup at 2 AM daily
```

### Environment-Specific Configuration

**application-dev.yml:**
```yaml
app:
  scheduler:
    enabled: true
    interval: 5000      # Faster for development
    batch-size: 5
    thread-pool-size: 3
```

**application-prod.yml:**
```yaml
app:
  scheduler:
    enabled: true
    interval: 10000
    batch-size: 20
    thread-pool-size: 20
    worker-id: ${HOSTNAME}  # Use actual hostname in production
```

**application-staging.yml:**
```yaml
app:
  scheduler:
    enabled: true
    interval: 15000
    batch-size: 10
    thread-pool-size: 5
```

---

## Frontend Changes

### Minimal Changes Required

The ProcessTracker API remains the same, so frontend changes are minimal:

#### 1. Update ProcessTracker Interface

**File:** `src/main/frontend/src/models/ProcessTracker.ts`

```typescript
export interface ProcessTracker {
  id: string;
  status: string;
  totalFiles: number;
  processedFiles: number;
  failedFiles: number;
  message: string;
  uploadedFilename: string;
  createdAt: string;
  updatedAt: string;
  completedAt?: string;
  jobId?: string;              // NEW: Link to job queue
  correlationId?: string;      // NEW: For batch tracking
  queuePosition?: number;      // NEW: Position in queue (optional)
  estimatedStartTime?: string; // NEW: Estimated start (optional)
}
```

#### 2. Add Queue Position API (Optional)

**Backend Endpoint:**
```java
@GetMapping("/api/tracker/{id}/queue-position")
public ResponseEntity<Map<String, Object>> getQueuePosition(@PathVariable UUID id) {
    ProcessTracker tracker = trackerRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Tracker not found"));
    
    if (tracker.getJobId() == null) {
        return ResponseEntity.ok(Map.of("position", 0));
    }
    
    JobQueue job = jobQueueRepository.findById(tracker.getJobId())
        .orElseThrow(() -> new RuntimeException("Job not found"));
    
    if (job.getStatus() != JobStatus.PENDING) {
        return ResponseEntity.ok(Map.of("position", 0, "status", job.getStatus()));
    }
    
    // Count pending jobs ahead of this one
    long position = jobQueueRepository.countPendingJobsAhead(
        job.getPriority(),
        job.getCreatedAt()
    );
    
    return ResponseEntity.ok(Map.of(
        "position", position + 1,
        "estimatedMinutes", (position * 0.5) // Rough estimate
    ));
}
```

**Frontend Usage:**
```typescript
const fetchQueuePosition = async (trackerId: string) => {
  const response = await api.get(`/tracker/${trackerId}/queue-position`);
  return response.data;
};
```

---

## Testing Strategy

### Unit Tests

#### Test JobQueueService

**File:** `src/test/java/io/subbu/ai/firedrill/services/JobQueueServiceTest.java`

```java
@SpringBootTest
@Transactional
class JobQueueServiceTest {

    @Autowired
    private JobQueueService jobQueueService;

    @Autowired
    private JobQueueRepository jobQueueRepository;

    @Test
    void testCreateJob() {
        // Given
        byte[] fileData = "test content".getBytes();
        Map<String, Object> metadata = Map.of("key", "value");

        // When
        JobQueue job = jobQueueService.createJob(
            JobType.RESUME_PROCESSING,
            fileData,
            "test.pdf",
            metadata,
            JobPriority.NORMAL
        );

        // Then
        assertNotNull(job.getId());
        assertEquals(JobStatus.PENDING, job.getStatus());
        assertEquals(JobType.RESUME_PROCESSING, job.getJobType());
        assertEquals("test.pdf", job.getFilename());
        assertEquals(JobPriority.NORMAL.getValue(), job.getPriority());
    }

    @Test
    void testFetchAndLockPendingJobs() {
        // Given
        createTestJobs(5);

        // When
        List<JobQueue> jobs = jobQueueService.fetchAndLockPendingJobs(3);

        // Then
        assertEquals(3, jobs.size());
        jobs.forEach(job -> assertEquals(JobStatus.PENDING, job.getStatus()));
    }

    @Test
    void testJobRetryLogic() {
        // Given
        JobQueue job = createTestJob();
        Exception error = new RuntimeException("Test error");

        // When
        jobQueueService.handleFailure(job, error);

        // Then
        JobQueue updated = jobQueueRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.PENDING, updated.getStatus());
        assertEquals(1, updated.getRetryCount());
    }

    @Test
    void testJobFailsAfterMaxRetries() {
        // Given
        JobQueue job = createTestJob();
        job.setRetryCount(3);
        job.setMaxRetries(3);
        jobQueueRepository.save(job);

        Exception error = new RuntimeException("Final error");

        // When
        jobQueueService.handleFailure(job, error);

        // Then
        JobQueue updated = jobQueueRepository.findById(job.getId()).orElseThrow();
        assertEquals(JobStatus.FAILED, updated.getStatus());
        assertNotNull(updated.getCompletedAt());
    }

    private JobQueue createTestJob() {
        return jobQueueService.createJob(
            JobType.RESUME_PROCESSING,
            "test".getBytes(),
            "test.pdf",
            Map.of(),
            JobPriority.NORMAL
        );
    }

    private void createTestJobs(int count) {
        for (int i = 0; i < count; i++) {
            createTestJob();
        }
    }
}
```

### Integration Tests

```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class SchedulerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private JobSchedulerService schedulerService;

    @Autowired
    private JobQueueService jobQueueService;

    @Autowired
    private JobQueueRepository jobQueueRepository;

    @Test
    void testSchedulerProcessesJobs() throws Exception {
        // Given
        JobQueue job = jobQueueService.createJob(
            JobType.RESUME_PROCESSING,
            loadTestFile("resume.pdf"),
            "resume.pdf",
            Map.of(),
            JobPriority.NORMAL
        );

        // When
        schedulerService.processJobQueue();

        // Wait for processing
        Thread.sleep(2000);

        // Then
        JobQueue processed = jobQueueRepository.findById(job.getId()).orElseThrow();
        assertTrue(
            processed.getStatus() == JobStatus.COMPLETED ||
            processed.getStatus() == JobStatus.PROCESSING
        );
    }

    private byte[] loadTestFile(String filename) throws IOException {
        return Files.readAllBytes(
            Paths.get("src/test/resources/test-files/" + filename)
        );
    }
}
```

---

## Deployment Guide

### Step 1: Database Migration

```bash
# Run migration script
psql -h localhost -U postgres -d resume_analyzer -f src/main/resources/db/migration/V2__add_job_queue.sql

# Verify tables created
psql -h localhost -U postgres -d resume_analyzer -c "\dt job*"
```

### Step 2: Build Application

```bash
# Build with Maven
mvn clean package -DskipTests

# Or with tests
mvn clean package
```

### Step 3: Deploy Configuration

Update `application.yml` in production:

```yaml
app:
  scheduler:
    enabled: true
    worker-id: ${HOSTNAME}
    batch-size: 20
    thread-pool-size: 20
```

### Step 4: Deploy Application

```bash
# Stop current application
systemctl stop resume-analyzer

# Deploy new JAR
cp target/resume-analyzer-1.0.0.jar /opt/resume-analyzer/

# Start application
systemctl start resume-analyzer

# Check logs
journalctl -u resume-analyzer -f
```

### Step 5: Monitor

```bash
# Check job queue
psql -c "SELECT status, COUNT(*) FROM job_queue GROUP BY status;"

# Check recent jobs
psql -c "SELECT id, status, filename, created_at FROM job_queue ORDER BY created_at DESC LIMIT 10;"

# Monitor logs
tail -f /var/log/resume-analyzer/application.log | grep "JobScheduler"
```

---

## Migration Path

### From Async to Scheduler

#### Phase 1: Dual Mode (Week 1)

Run both async and scheduler side-by-side:

```java
@Service
public class HybridFileUploadService {
    
    @Value("${app.upload.use-scheduler:false}")
    private boolean useScheduler;
    
    public UUID handleFileUpload(MultipartFile file) throws IOException {
        if (useScheduler) {
            return schedulerBasedUpload(file);
        } else {
            return asyncUpload(file);
        }
    }
}
```

**Configuration:**
```yaml
app:
  upload:
    use-scheduler: false  # Start with false, gradually enable
```

#### Phase 2: A/B Testing (Week 2)

Route 50% of traffic to scheduler:

```java
public UUID handleFileUpload(MultipartFile file) throws IOException {
    boolean useScheduler = ThreadLocalRandom.current().nextBoolean();
    
    if (useScheduler) {
        return schedulerBasedUpload(file);
    } else {
        return asyncUpload(file);
    }
}
```

Monitor metrics:
- Processing time
- Success rate  
- Error rate
- User feedback

#### Phase 3: Full Migration (Week 3)

```yaml
app:
  upload:
    use-scheduler: true  # Enable for all traffic
```

Remove old async code after 2 weeks of stable operation.

---

## Monitoring & Operations

### Health Checks

```java
@RestController
@RequestMapping("/api/health")
public class SchedulerHealthController {
    
    @GetMapping("/scheduler")
    public Map<String, Object> getSchedulerHealth() {
        return Map.of(
            "status", schedulerEnabled ? "UP" : "DOWN",
            "queueStats", jobQueueService.getQueueStatistics(),
            "avgProcessingTime", getAvgProcessingTime(),
            "workerId", getWorkerId()
        );
    }
}
```

### Metrics

Add metrics endpoint:

```java
@GetMapping("/api/metrics/jobs")
public Map<String, Object> getJobMetrics() {
    return Map.of(
        "pending", jobQueueRepository.countByStatus(JobStatus.PENDING),
        "processing", jobQueueRepository.countByStatus(JobStatus.PROCESSING),
        "completed24h", getCompletedLast24Hours(),
        "failed24h", getFailedLast24Hours(),
        "avgProcessingTime", getAvgProcessingTime(),
        "queueDepth", getQueueDepth()
    );
}
```

### Cleanup Job

```java
@Scheduled(cron = "${app.job-queue.cleanup-schedule:0 0 2 * * *}")
public void cleanupOldJobs() {
    int days = retentionDays;
    LocalDateTime threshold = LocalDateTime.now().minusDays(days);
    
    List<JobQueue> oldJobs = jobQueueRepository.findJobsForCleanup(threshold);
    
    // Archive to history table
    oldJobs.forEach(job -> archiveJob(job));
    
    // Delete from main table
    jobQueueRepository.deleteByIdIn(
        oldJobs.stream().map(JobQueue::getId).toList()
    );
    
    log.info("Cleaned up {} old jobs", oldJobs.size());
}
```

---

## Conclusion

This implementation guide provides a complete, production-ready scheduler-based resume processing system. Key benefits:

✅ **Scalable**: Horizontal scaling across multiple instances  
✅ **Resilient**: Automatic retry with failure recovery  
✅ **Observable**: Comprehensive monitoring and metrics  
✅ **Flexible**: Priority handling and scheduled processing  
✅ **Maintainable**: Clean separation of concerns  

Follow the implementation steps in order, test thoroughly, and migrate gradually for a smooth transition.

---

**Document Version:** 1.0  
**Last Updated:** February 17, 2026  
**Next Review:** March 17, 2026
