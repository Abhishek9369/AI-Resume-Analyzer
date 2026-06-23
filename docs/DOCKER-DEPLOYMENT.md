# Docker Deployment Guide

## Overview

This guide covers deploying the Resume Analyzer application using Docker and Docker Compose. The setup includes:
- PostgreSQL database with pgvector extension
- Spring Boot application with Spring AI
- Nginx reverse proxy (optional for development, required for production)

## Prerequisites

- Docker Desktop 20.10+ or Docker Engine 20.10+
- Docker Compose 2.0+
- At least 4GB RAM available for containers
- LLM Studio running on host machine (http://localhost:1234) with:
  - Chat model: `mistralai/mistral-7b-instruct-v0.3` or `meta-llama/llama-3.1-8b-instruct`
  - Embedding model: `text-embedding-nomic-embed-text-v1.5`

## Quick Start (Development)

### 1. Setup Environment Variables

```bash
# Copy the example environment file
cp docker/.env.example docker/.env

# Edit docker/.env with your settings
# At minimum, change the DB_PASSWORD
```

### 2. Build and Start Services

```bash
# Navigate to docker directory
cd docker

# Build the application image (first time or after code changes)
docker-compose build

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f app
```

### 3. Verify Deployment

```bash
# Check service health
docker-compose ps

# Test database connection
docker-compose exec postgres pg_isready -U postgres

# Test application health
curl http://localhost:8080/actuator/health

# Access the application
# Frontend: http://localhost:8080
# GraphQL IDE: http://localhost:8080/graphiql
# Health Check: http://localhost:8080/actuator/health
```

### 4. Stop Services

```bash
# Stop containers (preserves data)
docker-compose stop

# Stop and remove containers (preserves volumes)
docker-compose down

# Stop and remove containers AND volumes (clean slate)
docker-compose down -v
```

## Production Deployment

### 1. Prepare Environment

```bash
# Copy production environment template
cp docker/.env.example docker/.env.prod

# Edit docker/.env.prod with production settings:
# - Strong database password
# - Production LLM Studio URL (if using remote server)
# - Appropriate resource limits
# - Logging levels
```

### 2. Generate SSL Certificates

For production, use Let's Encrypt or a proper CA. For testing:

```bash
# Generate self-signed certificate (development/testing only)
cd docker/nginx
chmod +x generate-ssl.sh
./generate-ssl.sh

# For Let's Encrypt (recommended for production):
# 1. Install certbot
# 2. Run: certbot certonly --standalone -d your-domain.com
# 3. Copy certificates to docker/nginx/ssl/
# 4. Update nginx.conf to use proper certificate paths
```

### 3. Configure Nginx for HTTPS

Edit `docker/nginx/nginx.conf`:
- Uncomment the HTTPS server block
- Update `server_name` with your domain
- Verify SSL certificate paths
- Consider restricting `/actuator/` endpoints

### 4. Deploy with Production Compose

```bash
cd docker

# Build with production settings
docker-compose -f docker-compose.yml -f docker-compose.prod.yml build

# Start services
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# Verify
docker-compose -f docker-compose.yml -f docker-compose.prod.yml ps
```

## Configuration Reference

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_NAME` | `resume_analyzer` | PostgreSQL database name |
| `DB_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` | `postgres` | Database password (change for production!) |
| `DB_PORT` | `5432` | PostgreSQL port |
| `LLM_STUDIO_BASE_URL` | `http://host.docker.internal:1234/v1` | LLM Studio API endpoint |
| `LLM_STUDIO_API_KEY` | `not-needed` | API key (not needed for local LLM Studio) |
| `LLM_STUDIO_MODEL` | `mistralai/mistral-7b-instruct-v0.3` | Chat model name |
| `LLM_STUDIO_EMBEDDING_MODEL` | `text-embedding-nomic-embed-text-v1.5` | Embedding model |
| `APP_PORT` | `8080` | Application port |
| `NGINX_HTTP_PORT` | `80` | Nginx HTTP port |
| `NGINX_HTTPS_PORT` | `443` | Nginx HTTPS port |
| `LOGGING_LEVEL_ROOT` | `INFO` | Root logging level |
| `MAX_FILE_SIZE` | `50MB` | Maximum resume file size |

### Service Endpoints

- **Frontend**: http://localhost (with Nginx) or http://localhost:8080
- **GraphQL IDE**: http://localhost:8080/graphiql
- **GraphQL API**: http://localhost:8080/graphql
- **REST API**: http://localhost:8080/api/*
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics (if enabled)

## Docker Architecture

### Multi-Stage Build

The Dockerfile uses a multi-stage build:

1. **Builder Stage**: 
   - Base: `maven:3.9-eclipse-temurin-21`
   - Builds backend with Maven
   - Builds frontend with Node.js (via frontend-maven-plugin)
   - Creates executable JAR

2. **Runtime Stage**:
   - Base: `eclipse-temurin:21-jre-jammy`
   - Copies only the JAR file
   - Runs as non-root user (`appuser`)
   - Significantly smaller image size

### Networking

- **Development**: All services on default bridge network, ports exposed
- **Production**: Nginx is the only service with exposed ports (80/443), proxies to app

### Volumes

- `postgres_data`: Persistent database storage
- `app_uploads`: Resume file uploads
- `app_logs`: Application logs
- `nginx_logs`: Nginx access/error logs (production)

### Health Checks

- **PostgreSQL**: `pg_isready` command
- **Application**: `curl http://localhost:8080/actuator/health`
- **Nginx**: TCP check on port 80

## Common Tasks

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f app
docker-compose logs -f postgres

# Last 100 lines
docker-compose logs --tail=100 app
```

### Access Database

```bash
# PostgreSQL shell
docker-compose exec postgres psql -U postgres -d resume_analyzer

# Run SQL query
docker-compose exec postgres psql -U postgres -d resume_analyzer -c "SELECT COUNT(*) FROM resume;"

# Backup database
docker-compose exec postgres pg_dump -U postgres resume_analyzer > backup.sql

# Restore database
docker-compose exec -T postgres psql -U postgres resume_analyzer < backup.sql
```

### Rebuild Application

```bash
# After code changes
docker-compose build app
docker-compose up -d app

# Force rebuild (no cache)
docker-compose build --no-cache app
```

### Scale Services (if needed)

```bash
# Run multiple app instances
docker-compose up -d --scale app=3

# Note: Requires load balancer configuration in nginx
```

### Clean Up

```bash
# Remove stopped containers
docker-compose rm

# Remove unused images
docker image prune

# Remove unused volumes (WARNING: deletes data!)
docker volume prune

# Complete cleanup
docker-compose down -v
docker system prune -a --volumes
```

## Troubleshooting

### Application won't start

**Check logs:**
```bash
docker-compose logs app
```

**Common issues:**
- Database not ready: Wait for PostgreSQL health check to pass
- Port conflict: Another service using port 8080
- LLM Studio not accessible: Check `host.docker.internal` resolves to host machine

**Solution:**
```bash
# Restart services in order
docker-compose restart postgres
docker-compose restart app
```

### Cannot connect to LLM Studio

**Linux users:** `host.docker.internal` doesn't work by default.

**Solution:**
```bash
# Option 1: Add to docker-compose.yml extra_hosts
extra_hosts:
  - "host.docker.internal:172.17.0.1"

# Option 2: Use host network mode (Linux only)
network_mode: host

# Option 3: Use host IP directly
LLM_STUDIO_BASE_URL=http://192.168.1.100:1234/v1
```

### Database connection errors

**Check PostgreSQL is running:**
```bash
docker-compose exec postgres pg_isready -U postgres
```

**Verify pgvector extension:**
```bash
docker-compose exec postgres psql -U postgres -d resume_analyzer -c "SELECT * FROM pg_extension WHERE extname = 'vector';"
```

**Reset database:**
```bash
docker-compose down -v  # Removes all data!
docker-compose up -d
```

### Out of memory errors

**Check container memory:**
```bash
docker stats
```

**Increase limits in docker-compose.prod.yml:**
```yaml
services:
  app:
    deploy:
      resources:
        limits:
          memory: 4G  # Increase from 3G
```

### Nginx 502 Bad Gateway

**Check application health:**
```bash
curl http://localhost:8080/actuator/health
```

**Verify nginx can reach app:**
```bash
docker-compose exec nginx ping app
docker-compose exec nginx curl http://app:8080/actuator/health
```

**Check nginx configuration:**
```bash
docker-compose exec nginx nginx -t
```

### File upload fails

**Check volume permissions:**
```bash
docker-compose exec app ls -la /app/uploads
```

**Increase size limits:**
Edit `docker/.env`:
```
MAX_FILE_SIZE=100MB
MAX_REQUEST_SIZE=200MB
```

## Performance Optimization

### JVM Tuning

Edit `docker-compose.prod.yml`:
```yaml
environment:
  JAVA_TOOL_OPTIONS: >-
    -Xmx4g
    -Xms2g
    -XX:+UseG1GC
    -XX:MaxGCPauseMillis=200
    -XX:+UseStringDeduplication
```

### PostgreSQL Tuning

Edit `docker-compose.prod.yml`:
```yaml
command: >
  postgres
  -c shared_buffers=512MB
  -c effective_cache_size=2GB
  -c work_mem=16MB
```

### Nginx Caching

Edit `docker/nginx/nginx.conf`:
```nginx
# Add caching for static assets
location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}
```

## Security Best Practices

1. **Change default passwords**: Never use default `DB_PASSWORD` in production
2. **Use HTTPS**: Always enable SSL/TLS for production
3. **Restrict actuator endpoints**: Limit access to `/actuator/` paths
4. **Keep images updated**: Regularly pull latest base images
5. **Scan for vulnerabilities**: Use `docker scan resume-analyzer:latest`
6. **Use secrets management**: Consider Docker secrets or Vault for production
7. **Run as non-root**: Already configured in Dockerfile
8. **Limit resources**: Prevent DoS with memory/CPU limits

## Monitoring

### Health Checks

```bash
# Application health
curl http://localhost:8080/actuator/health

# Detailed health with database
curl http://localhost:8080/actuator/health/db

# Liveness probe
curl http://localhost:8080/actuator/health/liveness

# Readiness probe
curl http://localhost:8080/actuator/health/readiness
```

### Metrics

```bash
# JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP metrics
curl http://localhost:8080/actuator/metrics/http.server.requests

# Database connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections
```

### Prometheus Integration

Metrics are exposed in Prometheus format at `/actuator/prometheus`. Configure Prometheus to scrape:

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'resume-analyzer'
    static_configs:
      - targets: ['app:8080']
    metrics_path: '/actuator/prometheus'
```

## Backup and Recovery

### Database Backup

```bash
# Create backup
docker-compose exec postgres pg_dump -U postgres resume_analyzer | gzip > backup_$(date +%Y%m%d).sql.gz

# Automated daily backups (add to crontab)
0 2 * * * cd /path/to/resume-analyzer/docker && docker-compose exec -T postgres pg_dump -U postgres resume_analyzer | gzip > /backups/resume_analyzer_$(date +\%Y\%m\%d).sql.gz
```

### Volume Backup

```bash
# Backup volumes
docker run --rm \
  -v resume-analyzer_postgres_data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/postgres_data_backup.tar.gz -C /data .

# Restore volumes
docker run --rm \
  -v resume-analyzer_postgres_data:/data \
  -v $(pwd):/backup \
  alpine tar xzf /backup/postgres_data_backup.tar.gz -C /data
```

## Next Steps

After successful deployment:

1. **Configure monitoring**: Set up Prometheus + Grafana for metrics
2. **Add authentication**: Implement JWT-based authentication (Phase 2)
3. **Set up CI/CD**: Automate builds and deployments
4. **Load testing**: Test with realistic resume volumes
5. **Implement caching**: Add Redis for session/cache management
6. **Enable HTTPS**: Configure proper SSL certificates
7. **Backup automation**: Schedule regular database backups

## Support

For issues or questions:
- Check [ARCHITECTURE.md](ARCHITECTURE.md) for system design
- Review [NEXT-STEPS.md](NEXT-STEPS.md) for roadmap
- See [LLM-STUDIO-SETUP.md](LLM-STUDIO-SETUP.md) for AI configuration
