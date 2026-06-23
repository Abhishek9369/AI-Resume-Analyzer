-- Migration script to add job queue tables for scheduler-based processing
-- Version: 2
-- Description: Adds job_queue, job_dead_letter_queue, and updates process_tracker

BEGIN;

-- 1. Create job queue table
CREATE TABLE IF NOT EXISTS job_queue (
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

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_job_queue_status_priority ON job_queue(status, priority DESC, created_at ASC)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_job_queue_scheduled_for ON job_queue(scheduled_for)
    WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_job_queue_assigned_to ON job_queue(assigned_to)
    WHERE status = 'PROCESSING';

CREATE INDEX IF NOT EXISTS idx_job_queue_correlation_id ON job_queue(correlation_id)
    WHERE correlation_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_job_queue_created_at ON job_queue(created_at DESC);

-- Create trigger to update updated_at
CREATE OR REPLACE FUNCTION update_job_queue_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_job_queue_updated_at ON job_queue;
CREATE TRIGGER trg_job_queue_updated_at
    BEFORE UPDATE ON job_queue
    FOR EACH ROW
    EXECUTE FUNCTION update_job_queue_timestamp();

-- 2. Modify process_tracker to link with job_queue
ALTER TABLE process_tracker 
    ADD COLUMN IF NOT EXISTS job_id UUID,
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(255);

-- Add foreign key only if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_process_tracker_job'
    ) THEN
        ALTER TABLE process_tracker
            ADD CONSTRAINT fk_process_tracker_job 
                FOREIGN KEY (job_id) REFERENCES job_queue(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_process_tracker_job_id ON process_tracker(job_id);
CREATE INDEX IF NOT EXISTS idx_process_tracker_correlation_id ON process_tracker(correlation_id);

-- 3. Create dead letter queue for failed jobs
CREATE TABLE IF NOT EXISTS job_dead_letter_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
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

CREATE INDEX IF NOT EXISTS idx_dlq_failed_at ON job_dead_letter_queue(failed_at DESC);
CREATE INDEX IF NOT EXISTS idx_dlq_resolved ON job_dead_letter_queue(resolved, failed_at DESC);

-- 4. Create job metrics table for monitoring
CREATE TABLE IF NOT EXISTS job_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_date DATE NOT NULL,
    job_type VARCHAR(50),
    total_jobs INT DEFAULT 0,
    completed_jobs INT DEFAULT 0,
    failed_jobs INT DEFAULT 0,
    avg_processing_time_seconds DECIMAL(10, 2),
    min_processing_time_seconds DECIMAL(10, 2),
    max_processing_time_seconds DECIMAL(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(metric_date, job_type)
);

CREATE INDEX IF NOT EXISTS idx_job_metrics_date ON job_metrics(metric_date DESC);

-- Add comments for documentation
COMMENT ON TABLE job_queue IS 'Queue for asynchronous job processing with scheduler';
COMMENT ON COLUMN job_queue.priority IS 'Higher number = higher priority (0=normal, 1=high, 2=urgent)';
COMMENT ON COLUMN job_queue.metadata IS 'JSON data specific to job type (trackerId, userId, etc)';
COMMENT ON COLUMN job_queue.correlation_id IS 'Groups related jobs (e.g., all files in one upload batch)';
COMMENT ON COLUMN job_queue.heartbeat_at IS 'Last heartbeat from worker processing this job';
COMMENT ON COLUMN job_queue.version IS 'Optimistic locking version for concurrent updates';

COMMENT ON TABLE job_dead_letter_queue IS 'Failed jobs that exceeded max retries';
COMMENT ON TABLE job_metrics IS 'Daily aggregated metrics for job processing monitoring';

COMMIT;
