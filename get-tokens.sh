#!/bin/bash

# Check if required tools are installed
check_dependencies() {
    if ! command -v curl &> /dev/null; then
        echo "❌ Error: curl is not installed"
        exit 1
    fi
    
    if ! command -v jq &> /dev/null; then
        echo "❌ Error: jq is not installed"
        echo "Install with: brew install jq (macOS) or apt install jq (Linux)"
        exit 1
    fi
}

# Check if services are running
check_services() {
    echo "🔍 Checking services..."
    
    # Check Keycloak
    if ! curl -s http://localhost:8080/health &> /dev/null; then
        echo "❌ Keycloak is not running on port 8080"
        echo "Please start Keycloak first: ./start-keycloak.sh"
        exit 1
    fi
    echo "✅ Keycloak is running"
    
    # Check Spring Boot application
    if ! curl -s http://localhost:8081/actuator/health &> /dev/null; then
        echo "❌ Spring Boot application is not running on port 8081"
        echo "Please start the application: ./mvnw spring-boot:run"
        exit 1
    fi
    echo "✅ Spring Boot application is running"
    
    # Check OPA
    if ! curl -s http://localhost:8181/health &> /dev/null; then
        echo "❌ OPA is not running on port 8181"
        echo "Please start OPA first: ./start-opa.sh"
        exit 1
    fi
    echo "✅ OPA is running"
    echo ""
}

# Get JWT token for a user
get_token() {
    local username=$1
    local password=$2
    
    local token=$(curl -s -X POST http://localhost:8080/realms/demo/protocol/openid-connect/token \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=opa-demo" \
        -d "username=$username" \
        -d "password=$password" | \
        jq -r '.access_token')
    
    if [ "$token" = "null" ] || [ -z "$token" ]; then
        echo "❌ Failed to get token for $username"
        echo "Please check if user exists and credentials are correct"
        return 1
    fi
    
    echo "$token"
}

echo "🎫 Getting JWT tokens for users..."
echo ""

# Check dependencies and services
check_dependencies
check_services

echo "📝 Retrieving tokens..."
echo ""

echo "👤 Alice token:"
ALICE_TOKEN=$(get_token "alice" "password")
if [ $? -eq 0 ]; then
    echo "$ALICE_TOKEN"
    echo ""
fi

echo "👤 Bob token:"
BOB_TOKEN=$(get_token "bob" "password")
if [ $? -eq 0 ]; then
    echo "$BOB_TOKEN"
    echo ""
fi

echo "👤 Admin token:"
ADMIN_TOKEN=$(get_token "admin" "password")
if [ $? -eq 0 ]; then
    echo "$ADMIN_TOKEN"
    echo ""
fi

echo "✅ Token retrieval completed!"
echo "💡 You can now use these tokens to test the API endpoints"
