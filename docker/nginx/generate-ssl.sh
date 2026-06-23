#!/bin/bash

# Generate self-signed SSL certificate for development
# For production, use proper SSL certificates from Let's Encrypt or a CA

echo "Generating self-signed SSL certificate for development..."

# Create ssl directory if it doesn't exist
mkdir -p "$(dirname "$0")/ssl"

# Generate private key and certificate
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout "$(dirname "$0")/ssl/key.pem" \
    -out "$(dirname "$0")/ssl/cert.pem" \
    -subj "/C=US/ST=State/L=City/O=Organization/OU=IT/CN=localhost"

# Set appropriate permissions
chmod 600 "$(dirname "$0")/ssl/key.pem"
chmod 644 "$(dirname "$0")/ssl/cert.pem"

echo "Certificate generated successfully!"
echo "Certificate: $(dirname "$0")/ssl/cert.pem"
echo "Private Key: $(dirname "$0")/ssl/key.pem"
echo ""
echo "Note: This is a self-signed certificate for development only."
echo "For production, use proper SSL certificates from Let's Encrypt or a CA."
