-- Resume Analyzer Database Initialization Script
-- This script runs automatically when the PostgreSQL container is first created

-- Enable pgvector extension for vector similarity search
CREATE EXTENSION IF NOT EXISTS vector;

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON DATABASE resume_analyzer TO postgres;

-- Create schema if not exists (optional, Spring Boot will create tables)
-- We rely on Spring Boot JPA to create the schema via hibernate.ddl-auto=update

-- Verify pgvector installation
SELECT extversion FROM pg_extension WHERE extname = 'vector';

-- Output confirmation
\echo 'Database initialized successfully with pgvector extension'
\echo 'Application will create tables automatically via JPA'
