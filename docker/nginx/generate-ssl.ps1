# PowerShell script to generate self-signed SSL certificate for development
# For production, use proper SSL certificates from Let's Encrypt or a CA

Write-Host "Generating self-signed SSL certificate for development..." -ForegroundColor Cyan

# Get the script directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$sslDir = Join-Path $scriptDir "ssl"

# Create ssl directory if it doesn't exist
if (-not (Test-Path $sslDir)) {
    New-Item -ItemType Directory -Path $sslDir | Out-Null
    Write-Host "Created SSL directory: $sslDir" -ForegroundColor Green
}

# Check if OpenSSL is available
$opensslPath = Get-Command openssl -ErrorAction SilentlyContinue

if (-not $opensslPath) {
    Write-Host "`nERROR: OpenSSL is not installed or not in PATH." -ForegroundColor Red
    Write-Host "`nOptions to install OpenSSL on Windows:" -ForegroundColor Yellow
    Write-Host "  1. Using Chocolatey: choco install openssl" -ForegroundColor Yellow
    Write-Host "  2. Download from: https://slproweb.com/products/Win32OpenSSL.html" -ForegroundColor Yellow
    Write-Host "  3. Using Git for Windows (includes OpenSSL): https://git-scm.com/download/win" -ForegroundColor Yellow
    Write-Host "`nAlternatively, you can run the bash script in WSL or Git Bash." -ForegroundColor Yellow
    exit 1
}

# Certificate paths
$certPath = Join-Path $sslDir "cert.pem"
$keyPath = Join-Path $sslDir "key.pem"

# Generate private key and certificate
Write-Host "`nGenerating certificate and private key..." -ForegroundColor Cyan

$opensslCommand = @(
    "req",
    "-x509",
    "-nodes",
    "-days", "365",
    "-newkey", "rsa:2048",
    "-keyout", $keyPath,
    "-out", $certPath,
    "-subj", "/C=US/ST=State/L=City/O=Resume-Analyzer/OU=Development/CN=localhost"
)

& openssl $opensslCommand 2>&1 | Out-Null

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✓ SSL certificate generated successfully!" -ForegroundColor Green
    Write-Host "`nGenerated files:" -ForegroundColor Cyan
    Write-Host "  Certificate: $certPath" -ForegroundColor White
    Write-Host "  Private Key: $keyPath" -ForegroundColor White
    Write-Host "`nCertificate Details:" -ForegroundColor Cyan
    
    # Display certificate info
    & openssl x509 -in $certPath -noout -subject -issuer -dates
    
    Write-Host "`n" -NoNewline
    Write-Host "IMPORTANT NOTES:" -ForegroundColor Yellow
    Write-Host "  • This is a self-signed certificate for DEVELOPMENT ONLY" -ForegroundColor Yellow
    Write-Host "  • Your browser will show a security warning - this is expected" -ForegroundColor Yellow
    Write-Host "  • For production, use proper SSL certificates from Let's Encrypt or a CA" -ForegroundColor Yellow
    Write-Host "`nNext steps:" -ForegroundColor Cyan
    Write-Host "  1. Start Nginx with: docker-compose up -d" -ForegroundColor White
    Write-Host "  2. Access the app at: https://localhost" -ForegroundColor White
    Write-Host "  3. Accept the security warning in your browser" -ForegroundColor White
    
} else {
    Write-Host "`nERROR: Failed to generate SSL certificate." -ForegroundColor Red
    Write-Host "Exit Code: $LASTEXITCODE" -ForegroundColor Red
    exit 1
}
