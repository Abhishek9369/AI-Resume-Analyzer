# Docker Deployment Files

Quick reference for Resume Analyzer Docker deployment with HTTPS support.

## Directory Structure

```
docker/
├── Dockerfile                 # Multi-stage build (Maven + JRE)
├── .dockerignore             # Build optimization
├── .env.example              # Environment variable template
├── docker-compose.yml        # Development setup (with Nginx HTTPS)
├── docker-compose.prod.yml   # Production setup
├── init-db.sql              # PostgreSQL initialization
└── nginx/
    ├── nginx.conf           # Nginx reverse proxy configuration (HTTPS enabled)
    ├── generate-ssl.sh      # SSL certificate generation script (Bash)
    ├── generate-ssl.ps1     # SSL certificate generation script (PowerShell)
    └── ssl/                 # SSL certificates (generated)
```

## Quick Start with HTTPS

### 1. Generate SSL Certificates

**On Windows (PowerShell):**
```powershell
cd docker/nginx
./generate-ssl.ps1
```

**On Linux/Mac or Git Bash:**
```bash
cd docker/nginx
chmod +x generate-ssl.sh
./generate-ssl.sh
```

This creates:
- `docker/nginx/ssl/cert.pem` - Self-signed certificate
- `docker/nginx/ssl/key.pem` - Private key

### 2. Deploy the Application

```bash
# Setup environment
cp .env.example .env
# Edit .env with your settings (LLM Studio URL, DB credentials, etc.)

# Build and start all services (PostgreSQL, App, Nginx)
cd docker
docker-compose build
docker-compose up -d

# Verify all services are healthy
docker-compose ps
```

### 3. Access the Application

- **HTTPS (recommended):** https://localhost
- **HTTP (redirects to HTTPS):** http://localhost
- **Health Check:** https://localhost/actuator/health
- **GraphQL:** https://localhost/graphql
- **GraphiQL IDE:** https://localhost/graphiql

**Note:** Your browser will show a security warning because the certificate is self-signed. This is expected for development. Click "Advanced" → "Proceed to localhost" (or similar).

### 4. View Logs and Monitor

```bash
# View logs
docker-compose logs -f app
docker-compose logs -f nginx

# Check container stats
docker stats --no-stream

# Stop all services
docker-compose down
```

## Development (Alternative: Without Nginx)

```bash
# 1. Setup environment
cp .env.example .env
# Edit .env with your settings

# 2. Build and start
docker-compose build
docker-compose up -d

# 3. Verify
docker-compose ps
curl http://localhost:8080/actuator/health

# 4. View logs
docker-compose logs -f app

# 5. Stop
docker-compose down
```

### Production

```bash
# 1. Setup environment
cp .env.example .env.prod
# Edit .env.prod with production settings

# 2. Generate SSL certificates
cd nginx
./generate-ssl.sh  # For testing; use Let's Encrypt for production
cd ..

# 3. Start with production settings
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 4. Verify
curl https://localhost/actuator/health
```

## Services

### PostgreSQL
- **Image**: `pgvector/pgvector:pg15`
- **Port**: 5432
- **Extensions**: pgvector
- **Health**: `pg_isready -U postgres`

### Application
- **Build**: Multi-stage (Maven + JRE)
- **Port**: 8080
- **User**: appuser (non-root)
- **Health**: `/actuator/health`

### Nginx (Optional in dev, Required in prod)
- **Image**: `nginx:1.25-alpine`
- **Ports**: 80 (HTTP), 443 (HTTPS)
- **Config**: `nginx/nginx.conf`

## Key Commands

```bash
# Build specific service
docker-compose build app

# Rebuild without cache
docker-compose build --no-cache

# Start with Nginx in dev
docker-compose --profile with-nginx up -d

# View logs
docker-compose logs -f [service]

# Execute commands
docker-compose exec app bash
docker-compose exec postgres psql -U postgres

# Clean up
docker-compose down           # Stop and remove containers
docker-compose down -v        # Also remove volumes (data loss!)
```

## Environment Variables

See `.env.example` for full list. Key variables:

- `DB_PASSWORD` - Database password (CHANGE for production!)
- `LLM_STUDIO_BASE_URL` - LLM Studio endpoint
- `LLM_STUDIO_MODEL` - Chat model name
- `MAX_FILE_SIZE` - Maximum resume file size

## Volumes

- `postgres_data` - Database files (persistent)
- `app_uploads` - Uploaded resume files
- `app_logs` - Application logs
- `nginx_logs` - Nginx logs (production)

## Health Checks

- **App**: http://localhost:8080/actuator/health
- **Database**: `docker-compose exec postgres pg_isready -U postgres`
- **Nginx**: http://localhost/health

## Troubleshooting

### App won't start
```bash
# Check logs
docker-compose logs app

# Verify database is ready
docker-compose exec postgres pg_isready -U postgres

# Restart in order
docker-compose restart postgres
sleep 5
docker-compose restart app
```

### Cannot connect to LLM Studio (Linux)
```bash
# Option 1: Use host IP
LLM_STUDIO_BASE_URL=http://192.168.1.100:1234/v1

# Option 2: Add to docker-compose.yml
extra_hosts:
  - "host.docker.internal:172.17.0.1"
```

### Out of memory
```bash
# Check usage
docker stats

# Increase limits in docker-compose.prod.yml
services:
  app:
    deploy:
      resources:
        limits:
          memory: 4G
```

## Documentation

For complete documentation, see:
- [docs/DOCKER-DEPLOYMENT.md](../docs/DOCKER-DEPLOYMENT.md) - Full deployment guide
- [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md) - System architecture
- [docs/NEXT-STEPS.md](../docs/NEXT-STEPS.md) - Roadmap

## Architecture

```
┌─────────────┐
│   Nginx     │ :80, :443
└──────┬──────┘
       │
┌──────▼──────┐
│ Spring Boot │ :8080
│ Application │
└──────┬──────┘
       │
┌──────▼──────┐
│ PostgreSQL  │ :5432
│ + pgvector  │
└─────────────┘
```

## Resource Limits (Production)

| Service | CPU | Memory | Storage |
|---------|-----|--------|---------|
| PostgreSQL | 2 cores | 2GB | Volume |
| Application | 2 cores | 3GB | - |
| Nginx | 1 core | 512MB | - |

## Security Notes

1. **Change default passwords** in production
2. **Use HTTPS** with proper SSL certificates
3. **Restrict actuator endpoints** (uncomment in nginx.conf)
4. **Keep images updated**: `docker-compose pull`
5. **Run security scans**: `docker scan resume-analyzer:latest`

## Next Steps

1. ✅ Docker setup complete
2. ⏳ Run first deployment test
3. ⏳ Set up monitoring (Prometheus + Grafana)
4. ⏳ Configure automated backups
5. ⏳ Set up CI/CD pipeline
