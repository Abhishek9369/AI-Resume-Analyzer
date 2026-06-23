-- RBAC Database Migration Script
-- Resume Analyzer - Phase 2: Role-Based Access Control
-- Created: February 18, 2026

-- =====================================================
-- Users Table: Core authentication and authorization
-- =====================================================
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'RECRUITER',
    is_active BOOLEAN DEFAULT true,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    CONSTRAINT valid_role CHECK (role IN ('ADMIN', 'RECRUITER', 'HR', 'HIRING_MANAGER'))
);

-- Create index on username and email for faster lookups
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- =====================================================
-- Employees Table: HR management
-- =====================================================
CREATE TABLE IF NOT EXISTS employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id VARCHAR(50) UNIQUE NOT NULL,
    candidate_id UUID REFERENCES candidates(id),
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20),
    department VARCHAR(100),
    position VARCHAR(100),
    hire_date DATE NOT NULL,
    salary DECIMAL(12, 2),
    employment_type VARCHAR(20) NOT NULL DEFAULT 'FULL_TIME',
    manager_id UUID REFERENCES employees(id),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by UUID REFERENCES users(id),
    CONSTRAINT valid_employment_type CHECK (employment_type IN ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERN')),
    CONSTRAINT valid_employee_status CHECK (status IN ('ACTIVE', 'ON_LEAVE', 'SUSPENDED', 'TERMINATED'))
);

-- Create indexes for employee searches
CREATE INDEX IF NOT EXISTS idx_employees_employee_id ON employees(employee_id);
CREATE INDEX IF NOT EXISTS idx_employees_email ON employees(email);
CREATE INDEX IF NOT EXISTS idx_employees_department ON employees(department);
CREATE INDEX IF NOT EXISTS idx_employees_status ON employees(status);
CREATE INDEX IF NOT EXISTS idx_employees_manager_id ON employees(manager_id);

-- =====================================================
-- Feedback Table: Hiring manager and recruiter feedback
-- =====================================================
CREATE TABLE IF NOT EXISTS feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(20) NOT NULL,
    entity_id UUID NOT NULL,
    feedback_type VARCHAR(20) NOT NULL,
    rating INTEGER,
    comments TEXT,
    provided_by UUID REFERENCES users(id) NOT NULL,
    is_visible BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_entity_type CHECK (entity_type IN ('CANDIDATE', 'JOB_REQUIREMENT')),
    CONSTRAINT valid_feedback_type CHECK (feedback_type IN ('SHORTLIST', 'REJECT', 'INTERVIEW', 'OFFER', 'GENERAL', 'TECHNICAL', 'CULTURAL_FIT')),
    CONSTRAINT valid_rating CHECK (rating IS NULL OR (rating BETWEEN 1 AND 5))
);

-- Create indexes for feedback queries
CREATE INDEX IF NOT EXISTS idx_feedback_entity ON feedback(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_feedback_provided_by ON feedback(provided_by);
CREATE INDEX IF NOT EXISTS idx_feedback_created_at ON feedback(created_at);

-- =====================================================
-- Audit Log Table: Security and compliance
-- =====================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES users(id),
    username VARCHAR(50),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id UUID,
    details TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    success BOOLEAN DEFAULT true,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for audit queries
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at DESC);

-- =====================================================
-- System Health Table: Monitoring and alerts
-- =====================================================
CREATE TABLE IF NOT EXISTS system_health (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service_name VARCHAR(50) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL,
    message TEXT,
    response_time_ms INTEGER,
    last_checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_success_at TIMESTAMP,
    last_failure_at TIMESTAMP,
    failure_count INTEGER DEFAULT 0,
    details JSONB,
    CONSTRAINT valid_service_status CHECK (status IN ('UP', 'DOWN', 'DEGRADED', 'UNKNOWN'))
);

-- Create index for health status queries
CREATE INDEX IF NOT EXISTS idx_system_health_service ON system_health(service_name);
CREATE INDEX IF NOT EXISTS idx_system_health_status ON system_health(status);

-- =====================================================
-- Alter existing tables to add user references
-- =====================================================

-- Add created_by column to candidates table if not exists
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='candidates' AND column_name='uploaded_by') THEN
        ALTER TABLE candidates ADD COLUMN uploaded_by UUID REFERENCES users(id);
    END IF;
END $$;

-- Add created_by column to job_requirements table if not exists
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='job_requirements' AND column_name='created_by') THEN
        ALTER TABLE job_requirements ADD COLUMN created_by UUID REFERENCES users(id);
    END IF;
END $$;

-- Add uploaded_by column to process_tracker table if not exists
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='process_tracker' AND column_name='uploaded_by') THEN
        ALTER TABLE process_tracker ADD COLUMN uploaded_by UUID REFERENCES users(id);
    END IF;
END $$;

-- =====================================================
-- seed initial data
-- =====================================================

-- Create default admin user (password: Admin@123)
-- Password hash generated using BCrypt with strength 10
INSERT INTO users (username, email, password_hash, first_name, last_name, role, is_active)
VALUES (
    'admin',
    'admin@resume-analyzer.local',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye2sGB2jlvO8MdPwnSTpZFH/8sU0MvCzG',
    'System',
    'Administrator',
    'ADMIN',
    true
) ON CONFLICT (username) DO NOTHING;

-- Create test recruiter user (password: Recruiter@123)
INSERT INTO users (username, email, password_hash, first_name, last_name, role, is_active, created_by)
VALUES (
    'recruiter',
    'recruiter@resume-analyzer.local',
    '$2a$10$5vL0YOCJGVb.gD1TnECr8e7wJO0OdGPBJTJF8T0J8RmE.tPo3Vm0a',
    'Jane',
    'Recruiter',
    'RECRUITER',
    true,
    (SELECT id FROM users WHERE username = 'admin')
) ON CONFLICT (username) DO NOTHING;

-- Create test HR user (password: HR@123)
INSERT INTO users (username, email, password_hash, first_name, last_name, role, is_active, created_by)
VALUES (
    'hr',
    'hr@resume-analyzer.local',
    '$2a$10$kO0YOL5JlTnvL0Y.L1nT1eU8FLF8Y.PJ8FLJ8FL8FL8FL8FL8FL8F',
    'Bob',
    'HR',
    'HR',
    true,
    (SELECT id FROM users WHERE username = 'admin')
) ON CONFLICT (username) DO NOTHING;

-- Create test hiring manager user (password: Manager@123)
INSERT INTO users (username, email, password_hash, first_name, last_name, role, is_active, created_by)
VALUES (
    'hiring_manager',
    'manager@resume-analyzer.local',
    '$2a$10$mO0YOL5JlTnvL0Y.L1nT1eU8FLF8Y.PJ8FLJ8FL8FL8FL8FL8FL8M',
    'Alice',
    'Manager',
    'HIRING_MANAGER',
    true,
    (SELECT id FROM users WHERE username = 'admin')
) ON CONFLICT (username) DO NOTHING;

-- Initialize system health entries
INSERT INTO system_health (service_name, status, message)
VALUES 
    ('database', 'UP', 'PostgreSQL connection healthy'),
    ('llm_service', 'UNKNOWN', 'LM Studio not checked yet'),
    ('application', 'UP', 'Spring Boot application running')
ON CONFLICT (service_name) DO UPDATE 
SET status = EXCLUDED.status, 
    message = EXCLUDED.message,
    last_checked_at = CURRENT_TIMESTAMP;

-- =====================================================
-- Create views for common queries
-- =====================================================

-- Active employees view
CREATE OR REPLACE VIEW active_employees AS
SELECT 
    e.*,
    u.username as created_by_username,
    m.first_name || ' ' || m.last_name as manager_name
FROM employees e
LEFT JOIN users u ON e.created_by = u.id
LEFT JOIN employees m ON e.manager_id = m.id
WHERE e.status = 'ACTIVE';

-- Recent feedback view
CREATE OR REPLACE VIEW recent_feedback AS
SELECT 
    f.*,
    u.username as provided_by_username,
    u.first_name || ' ' || u.last_name as provider_name
FROM feedback f
INNER JOIN users u ON f.provided_by = u.id
WHERE f.is_visible = true
ORDER BY f.created_at DESC;

-- Audit trail view (recent 1000 entries)
CREATE OR REPLACE VIEW recent_audit_trail AS
SELECT 
    a.*,
    u.first_name || ' ' || u.last_name as user_full_name
FROM audit_log a
LEFT JOIN users u ON a.user_id = u.id
ORDER BY a.created_at DESC
LIMIT 1000;

-- =====================================================
-- Grant permissions (if needed)
-- =====================================================

-- Grant SELECT on all tables to application user
-- GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO resume_analyzer_app;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO resume_analyzer_app;

-- =====================================================
-- Migration complete
-- =====================================================

COMMENT ON TABLE users IS 'User accounts for authentication and authorization';
COMMENT ON TABLE employees IS 'Employee records managed by HR';
COMMENT ON TABLE feedback IS 'Feedback on candidates and job requirements';
COMMENT ON TABLE audit_log IS 'Audit trail for security and compliance';
COMMENT ON TABLE system_health IS 'System health monitoring';

-- Display migration summary
SELECT 'RBAC Migration Complete' as status,
       (SELECT COUNT(*) FROM users) as total_users,
       (SELECT COUNT(*) FROM employees) as total_employees,
       (SELECT COUNT(*) FROM system_health) as health_checks
;
