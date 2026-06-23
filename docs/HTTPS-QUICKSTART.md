# HTTPS Quick Start - Resume Analyzer

Successfully configured! 🎉

## ✅ What's Configured

- **Nginx reverse proxy** with HTTPS support
- **Self-signed SSL certificate** for development
- **HTTP to HTTPS redirect** (automatic)
- **Security headers** (HSTS, X-Frame-Options, CSP, etc.)
- **All services running** and healthy

## 🚀 Access the Application

### Primary Access (HTTPS - Recommended)
```
https://localhost/                      # Main application
https://localhost/graphql               # GraphQL API
https://localhost/graphiql              # GraphQL IDE
https://localhost/actuator/health       # Health check
```

### HTTP Access
```
http://localhost/                       # Automatically redirects to HTTPS
```

### Direct App Access (Bypass Nginx)
```
http://localhost:8080/                  # Direct to Spring Boot app (for debugging)
```

## 🔐 Browser Security Warning

**Expected Behavior:** Your browser will show a security warning because we're using a self-signed certificate.

**How to Proceed:**

### Chrome / Edge
1. Click **"Advanced"**
2. Click **"Proceed to localhost (unsafe)"**
3. Or type `thisisunsafe` anywhere on the warning page

### Firefox  
1. Click **"Advanced..."**
2. Click **"Accept the Risk and Continue"**

### Safari
1. Click **"Show Details"**
2. Click **"visit this website"**

**This is normal and safe for development!** The certificate is valid, just not signed by a recognized authority.

## 📊 Services Status

Check all services are running:
```powershell
cd docker
docker-compose ps
```

Expected output:
```
NAME                    STATUS
resume-analyzer-app     Up (healthy)
resume-analyzer-db      Up (healthy)
resume-analyzer-nginx   Up
```

## 🛠️ Management Commands

### View Logs
```powershell
# All services
docker-compose logs -f

# Nginx only
docker-compose logs -f nginx

# Application only
docker-compose logs -f app
```

### Restart Services
```powershell
# Restart all
docker-compose restart

# Restart Nginx only
docker-compose restart nginx

# Restart app only
docker-compose restart app
```

### Stop Services
```powershell
# Stop all (preserves data)
docker-compose down

# Stop and remove volumes (deletes data)
docker-compose down -v
```

### Rebuild and Restart
```powershell
# Rebuild app after code changes
docker-compose build app
docker-compose up -d

# Full rebuild
docker-compose build
docker-compose up -d
```

## 🔧 Testing HTTPS

### Test Health Endpoint
```powershell
# HTTPS (skip certificate verification with -k)
curl -k https://localhost/actuator/health

# Should return:
# {"status":"UP","groups":["liveness","readiness"]}
```

### Test Redirect
```powershell
# HTTP should redirect to HTTPS
curl -I http://localhost/

# Should show:
# HTTP/1.1 301 Moved Permanently
# Location: https://localhost/
```

### Test Frontend
```powershell
# Access frontend over HTTPS
curl -k https://localhost/
```

## 📁 Configuration Files

| File | Purpose |
|------|---------|
| [docker/nginx/nginx.conf](../docker/nginx/nginx.conf) | Nginx configuration with HTTPS |
| [docker/nginx/ssl/cert.pem](../docker/nginx/ssl/cert.pem) | SSL certificate |
| [docker/nginx/ssl/key.pem](../docker/nginx/ssl/key.pem) | Private key |
| [docker/docker-compose.yml](../docker/docker-compose.yml) | Nginx service enabled |

## 🔄 Regenerate SSL Certificates

If you need to regenerate the certificates (e.g., after expiration):

**Windows (PowerShell):**
```powershell
cd docker/nginx
./generate-ssl.ps1
docker-compose restart nginx
```

**Linux/Mac or Git Bash:**
```bash
cd docker/nginx
./generate-ssl.sh
docker-compose restart nginx
```

Certificates are valid for **365 days** from generation.

## 🏗️ Architecture

```
Internet/Browser
       ↓ (HTTPS - Port 443)
    Nginx (reverse proxy)
       ↓ (HTTP - Internal)
  Spring Boot App (Port 8080)
       ↓
  PostgreSQL (Port 5432)
```

**Security Features:**
- ✅ TLS 1.2 and 1.3 support
- ✅ Strong cipher suites
- ✅ HSTS for browser security
- ✅ Security headers (XSS, clickjacking protection)
- ✅ Gzip compression
- ✅ Large file upload support (100MB)
- ✅ Extended timeouts for AI operations

## 📚 Additional Documentation

- **Detailed HTTPS Guide:** [docs/HTTPS-SETUP.md](HTTPS-SETUP.md)
- **Docker Deployment:** [docs/DOCKER-DEPLOYMENT.md](DOCKER-DEPLOYMENT.md)
- **Docker Quick Reference:** [docker/README.md](../docker/README.md)

## 🚨 Troubleshooting

### Nginx won't start
```powershell
# Check logs
docker-compose logs nginx

# Common issues:
# 1. SSL certificates missing → Run generate-ssl.ps1
# 2. Port 443 in use → Stop other services using: netstat -ano | findstr :443
# 3. Config syntax error → Test with: docker-compose exec nginx nginx -t
```

### Can't access HTTPS
```powershell
# Check if port 443 is exposed
docker-compose ps

# Check firewall (Windows)
netsh advfirewall firewall add rule name="HTTPS" dir=in action=allow protocol=TCP localport=443
```

### Certificate errors in browser
This is **expected** for self-signed certificates. See "Browser Security Warning" section above.

## 🎯 Production Deployment

For production, replace self-signed certificates with:
- **Let's Encrypt** (free, automated) - Recommended
- **Commercial SSL certificate** from a CA
- **Cloud provider certificate** (AWS ACM, Azure, etc.)

See [docs/HTTPS-SETUP.md](HTTPS-SETUP.md) for production setup instructions.

---

**Status:** ✅ HTTPS successfully configured and tested  
**Generated:** February 16, 2026  
**Certificate Validity:** 365 days from generation
