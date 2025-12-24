#!/bin/bash

echo "Starting Keycloak with Docker..."

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed or not in PATH"
    echo "Please install Docker first: https://www.docker.com/get-started"
    exit 1
fi

# Check if Docker daemon is running
if ! docker info &> /dev/null; then
    echo "Error: Docker daemon is not running"
    echo "Please start Docker Desktop or Docker daemon"
    exit 1
fi

# Check if container already exists
if docker ps -a --format "table {{.Names}}" | grep -q "keycloak-demo"; then
    echo "Keycloak container already exists. Stopping and removing..."
    docker stop keycloak-demo &> /dev/null
    docker rm keycloak-demo &> /dev/null
fi

# Check if port 8080 is already in use
if lsof -i :8080 &> /dev/null; then
    echo "Warning: Port 8080 is already in use"
    echo "Please stop the service using port 8080 or change the port"
    exit 1
fi

echo "Starting Keycloak container..."
docker run -d \
  --name keycloak-demo \
  -p 8080:8080 \
  -e KEYCLOAK_ADMIN=admin \
  -e KEYCLOAK_ADMIN_PASSWORD=admin \
  quay.io/keycloak/keycloak:latest \
  start-dev

if [ $? -eq 0 ]; then
    echo "✅ Keycloak container started successfully"
    echo "🌐 Keycloak will be available at http://localhost:8080"
    echo "🔑 Admin credentials: admin/admin"
    echo ""
    echo "⏳ Please wait 30-60 seconds for Keycloak to fully start"
    echo ""
    echo "📋 After Keycloak starts, create:"
    echo "   1. Realm: 'demo'"
    echo "   2. Client: 'opa-demo' (public client)"
    echo "   3. Users: alice, bob, admin"
else
    echo "❌ Failed to start Keycloak container"
    exit 1
fi
