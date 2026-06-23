# HTTPS Setup Guide - Resume Analyzer

This guide explains how to set up HTTPS for the Resume Analyzer application using Nginx with self-signed certificates for development and proper certificates for production.

## 📋 Table of Contents

- [Development Setup (Self-Signed SSL)](#development-setup-self-signed-ssl)
- [Browser Security Warnings](#browser-security-warnings)
- [Production Setup](#production-setup)
- [Certificate Management](#certificate-management)
- [Troubleshooting](#troubleshooting)

---

## Development Setup (Self-Signed SSL)

### Prerequisites

**Windows:**
- PowerShell (comes with Windows)
- OpenSSL (see installation options below)

**Linux/Mac:**
- Bash shell
- OpenSSL (usually pre-installed)

### Installing OpenSSL on Windows

Choose one of these options:

**Option 1: Using Chocolatey (Recommended)**
```powershell
choco install openssl
```

**Option 2: Using Scoop**
```powershell
scoop install openssl
```

**Option 3: Using Git for Windows**
- Download and install [Git for Windows](https://git-scm.com/download/win)
- OpenSSL is included, accessible via Git Bash

**Option 4: Manual Installation**
- Download from [https://slproweb.com/products/Win32OpenSSL.html](https://slproweb.com/products/Win32OpenSSL.html)
- Install and add to PATH

### Step 1: Generate SSL Certificates

**Windows (PowerShell):**
```powershell
cd docker/nginx
./generate-ssl.ps1
```

**Linux/Mac or Git Bash:**
```bash
cd docker/nginx
chmod +x generate-ssl.sh
./generate-ssl.sh
```

**Output:**
```
✓ SSL certificate generated successfully!

Generated files:
  Certificate: /path/to/docker/nginx/ssl/cert.pem
  Private Key: /path/to/docker/nginx/ssl/key.pem

Certificate Details:
  Subject: C=US, ST=State, L=City, O=Resume-Analyzer, OU=Development, CN=localhost
  Valid: from Feb 16 2026 to Feb 16 2027
```

### Step 2: Start Services with Nginx

```bash
cd docker

# Copy and configure environment
cp .env.example .env
# Edit .env if needed (LLM Studio URL, DB credentials, etc.)

# Build and start all services
docker-compose build
docker-compose up -d

# Verify all services are running
docker-compose ps
```

**Expected output:**
```
NAME                      STATUS
resume-analyzer-app       Up (healthy)
resume-analyzer-db        Up (healthy)
resume-analyzer-nginx     Up
```

### Step 3: Access the Application

Open your browser and navigate to:
- **Main app:** https://localhost
- **GraphQL API:** https://localhost/graphql
- **GraphiQL IDE:** https://localhost/graphiql
- **Health check:** https://localhost/actuator/health

---

## Browser Security Warnings

### Why You See Warnings

Self-signed certificates are not trusted by browsers because:
1. They're not issued by a recognized Certificate Authority (CA)
2. They can't be verified through the standard trust chain
3. Anyone can create them (no identity verification)

**This is NORMAL and EXPECTED for development!**

### How to Proceed in Each Browser

#### Google Chrome / Microsoft Edge
1. Click **"Advanced"** or **"Show details"**
2. Click **"Proceed to localhost (unsafe)"** or **"Continue to site"**
3. (Optional) Type `thisisunsafe` while on the warning page to bypass

#### Mozilla Firefox
1. Click **"Advanced..."**
2. Click **"Accept the Risk and Continue"**

#### Safari
1. Click **"Show Details"**
2. Click **"visit this website"**
3. Confirm in the popup

### Making Warnings Go Away (Optional)

**For Chrome/Edge on Windows:**
1. Export the certificate:
   ```powershell
   cd docker/nginx/ssl
   certutil -addstore "Root" cert.pem
   ```
2. Restart your browser

**For Firefox:**
1. Go to Settings → Privacy & Security → Certificates → View Certificates
2. Click "Authorities" tab → "Import"
3. Select `docker/nginx/ssl/cert.pem`
4. Check "Trust this CA to identify websites"

**Note:** Only do this for development certificates you created yourself!

---

## Production Setup

### Option 1: Let's Encrypt (Free, Recommended)

Let's Encrypt provides free, automated SSL certificates that are trusted by all browsers.

**Prerequisites:**
- A domain name (e.g., resume-analyzer.yourdomain.com)
- Port 80 accessible from the internet (for verification)

**Using Certbot:**

```bash
# Install Certbot
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install certbot

# CentOS/RHEL
sudo yum install certbot

# Generate certificate
sudo certbot certonly --standalone -d resume-analyzer.yourdomain.com

# Certificates will be in:
# /etc/letsencrypt/live/resume-analyzer.yourdomain.com/fullchain.pem
# /etc/letsencrypt/live/resume-analyzer.yourdomain.com/privkey.pem
```

**Update docker-compose.prod.yml:**

```yaml
  nginx:
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - /etc/letsencrypt/live/resume-analyzer.yourdomain.com:/etc/nginx/ssl:ro
```

**Update nginx.conf:**

```nginx
server {
    listen 443 ssl http2;
    server_name resume-analyzer.yourdomain.com;

    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    
    # ... rest of configuration
}
```

**Set up auto-renewal:**

```bash
# Test renewal
sudo certbot renew --dry-run

# Add to crontab (runs twice daily)
sudo crontab -e
# Add: 0 0,12 * * * certbot renew --quiet && docker-compose restart nginx
```

### Option 2: Commercial SSL Certificate

If you purchase an SSL certificate from a CA:

1. **Generate CSR (Certificate Signing Request):**
   ```bash
   openssl req -new -newkey rsa:2048 -nodes \
     -keyout docker/nginx/ssl/server.key \
     -out docker/nginx/ssl/server.csr
   ```

2. **Submit CSR to your CA** and receive certificate files

3. **Place certificates in `docker/nginx/ssl/`:**
   - `server.crt` (your certificate)
   - `server.key` (private key)
   - `ca-bundle.crt` (CA intermediate certificates)

4. **Update nginx.conf:**
   ```nginx
   ssl_certificate /etc/nginx/ssl/server.crt;
   ssl_certificate_key /etc/nginx/ssl/server.key;
   ssl_trusted_certificate /etc/nginx/ssl/ca-bundle.crt;
   ```

### Option 3: Cloud Provider Certificates

**AWS Certificate Manager (ACM):**
- Use ALB/NLB with ACM certificate
- Terminate SSL at load balancer
- Configure Nginx for HTTP only
- Let AWS handle HTTPS

**Azure Application Gateway:**
- Similar to AWS ALB
- SSL termination at gateway
- Nginx serves HTTP internally

**Cloudflare:**
- Use Cloudflare's Universal SSL
- Set SSL mode to "Full" or "Full (strict)"
- Generate origin certificate for Nginx

---

## Certificate Management

### Viewing Certificate Details

```bash
# View certificate information
openssl x509 -in docker/nginx/ssl/cert.pem -text -noout

# Check certificate expiration
openssl x509 -in docker/nginx/ssl/cert.pem -noout -dates

# Verify certificate and key match
openssl x509 -noout -modulus -in docker/nginx/ssl/cert.pem | openssl md5
openssl rsa -noout -modulus -in docker/nginx/ssl/key.pem | openssl md5
# MD5 hashes should match
```

### Renewing Self-Signed Certificate

```bash
# Regenerate (overwrites existing)
cd docker/nginx
./generate-ssl.ps1  # Windows
./generate-ssl.sh   # Linux/Mac

# Restart Nginx to load new certificate
cd ..
docker-compose restart nginx
```

### Certificate File Permissions

Ensure proper security:

```bash
# Set restrictive permissions on private key
chmod 600 docker/nginx/ssl/key.pem

# Certificate can be world-readable
chmod 644 docker/nginx/ssl/cert.pem
```

---

## Troubleshooting

### Issue: "No such file or directory" for SSL certificates

**Symptom:**
```
nginx: [emerg] cannot load certificate "/etc/nginx/ssl/cert.pem"
```

**Solution:**
```bash
# Generate certificates first
cd docker/nginx
./generate-ssl.ps1  # or ./generate-ssl.sh

# Verify files exist
ls -la ssl/
# Should show cert.pem and key.pem

# Restart
cd ..
docker-compose restart nginx
```

### Issue: "Connection refused" on port 443

**Check 1: Is Nginx running?**
```bash
docker-compose ps nginx
# Should show "Up"
```

**Check 2: Is port 443 available?**
```bash
# Windows
netstat -ano | findstr :443

# Linux/Mac
sudo lsof -i :443
```

**Check 3: Firewall blocking?**
```bash
# Windows
netsh advfirewall firewall add rule name="HTTPS" dir=in action=allow protocol=TCP localport=443

# Linux (ufw)
sudo ufw allow 443/tcp
```

### Issue: "SSL handshake failed"

**Check certificate validity:**
```bash
openssl s_client -connect localhost:443 -servername localhost

# Look for:
# - "Verify return code: 0 (ok)" or expected error for self-signed
# - Certificate chain
# - No "ssl handshake failure"
```

**Check Nginx logs:**
```bash
docker-compose logs nginx | grep -i ssl
docker-compose logs nginx | grep -i error
```

### Issue: Browser shows ERR_SSL_VERSION_OR_CIPHER_MISMATCH

**Update nginx.conf SSL protocols:**
```nginx
ssl_protocols TLSv1.2 TLSv1.3;
ssl_ciphers HIGH:!aNULL:!MD5:!3DES;
```

**Restart Nginx:**
```bash
docker-compose restart nginx
```

### Issue: Mixed content warnings (HTTP resources on HTTPS page)

**Solution:** Ensure all resources use relative URLs or HTTPS:
```javascript
// Bad
<script src="http://example.com/script.js"></script>

// Good
<script src="https://example.com/script.js"></script>
// Or use protocol-relative
<script src="//example.com/script.js"></script>
```

### Issue: SSL certificate expired

**Check expiration:**
```bash
openssl x509 -in docker/nginx/ssl/cert.pem -noout -dates
```

**Regenerate:**
```bash
cd docker/nginx
./generate-ssl.ps1  # or ./generate-ssl.sh
docker-compose restart nginx
```

---

## Configuration Reference

### Current HTTPS Setup

**Nginx Configuration:** [docker/nginx/nginx.conf](nginx/nginx.conf)
- ✅ HTTP (port 80) redirects to HTTPS
- ✅ HTTPS (port 443) with TLS 1.2/1.3
- ✅ HTTP/2 support
- ✅ HSTS header for security
- ✅ All application endpoints proxied

**Docker Compose:** [docker-compose.yml](docker-compose.yml)
- ✅ Nginx enabled by default
- ✅ SSL volume mounted
- ✅ Ports 80 and 443 exposed

### Security Headers

The following security headers are configured:

```nginx
X-Frame-Options: SAMEORIGIN
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
Referrer-Policy: no-referrer-when-downgrade
Strict-Transport-Security: max-age=31536000; includeSubDomains (HTTPS only)
```

### SSL Configuration

```nginx
ssl_protocols: TLSv1.2 TLSv1.3
ssl_ciphers: HIGH:!aNULL:!MD5
ssl_prefer_server_ciphers: on
ssl_session_cache: shared:SSL:10m
ssl_session_timeout: 10m
```

---

## Quick Commands Reference

```bash
# Generate SSL certificates
cd docker/nginx && ./generate-ssl.ps1 && cd ../..

# Start with HTTPS
docker-compose up -d

# Check Nginx status
docker-compose ps nginx
docker-compose logs nginx

# Test HTTPS connection
curl -k https://localhost/actuator/health

# Restart Nginx only
docker-compose restart nginx

# View certificate details
openssl x509 -in docker/nginx/ssl/cert.pem -text -noout

# Stop all services
docker-compose down
```

---

## Additional Resources

- [Nginx SSL Module Documentation](https://nginx.org/en/docs/http/ngx_http_ssl_module.html)
- [Let's Encrypt Documentation](https://letsencrypt.org/docs/)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
- [SSL Labs Server Test](https://www.ssllabs.com/ssltest/) (for public servers)

---

**For more information, see:**
- [Docker Deployment Guide](../docs/DOCKER-DEPLOYMENT.md)
- [Main README](docker/README.md)
