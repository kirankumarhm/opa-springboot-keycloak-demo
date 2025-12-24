#!/bin/bash

echo "========================================="
echo "OPA Spring Boot Demo - Debug Script"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "Step 1: Checking services..."
echo ""

# Check Keycloak
echo -n "Keycloak: "
if curl -s http://localhost:8080/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Running${NC}"
else
    echo -e "${RED}✗ Not running${NC}"
    exit 1
fi

# Check OPA
echo -n "OPA: "
if curl -s http://localhost:8181/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Running${NC}"
else
    echo -e "${RED}✗ Not running${NC}"
    exit 1
fi

# Check Spring Boot
echo -n "Spring Boot: "
if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Running${NC}"
else
    echo -e "${RED}✗ Not running${NC}"
    exit 1
fi

echo ""
echo "Step 2: Testing OPA policy directly..."
echo ""

# Test OPA policy directly
echo "Testing Alice access to document:1:"
RESULT=$(curl -s -X POST http://localhost:8181/v1/data/authz/allow \
  -H "Content-Type: application/json" \
  -d '{"input":{"user":"alice","action":"read","resource":"document:1"}}')
echo "OPA Response: $RESULT"

echo ""
echo "Testing Bob access to document:2:"
RESULT=$(curl -s -X POST http://localhost:8181/v1/data/authz/allow \
  -H "Content-Type: application/json" \
  -d '{"input":{"user":"bob","action":"read","resource":"document:2"}}')
echo "OPA Response: $RESULT"

echo ""
echo "Step 3: Getting JWT token for Alice..."
echo ""

ALICE_TOKEN=$(curl -s -X POST http://localhost:8080/realms/demo/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=opa-demo" \
  -d "username=alice" \
  -d "password=password" | jq -r '.access_token')

if [ "$ALICE_TOKEN" = "null" ] || [ -z "$ALICE_TOKEN" ]; then
    echo -e "${RED}✗ Failed to get Alice token${NC}"
    echo "Full response:"
    curl -s -X POST http://localhost:8080/realms/demo/protocol/openid-connect/token \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "grant_type=password" \
      -d "client_id=opa-demo" \
      -d "username=alice" \
      -d "password=password"
    exit 1
else
    echo -e "${GREEN}✓ Alice token obtained${NC}"
    echo "Token (first 50 chars): ${ALICE_TOKEN:0:50}..."
fi

echo ""
echo "Step 4: Testing API endpoint with verbose output..."
echo ""

echo "Testing Alice access to document 1:"
curl -v -H "Authorization: Bearer $ALICE_TOKEN" \
  http://localhost:8081/api/users/alice/documents/1

echo ""
echo ""
echo "Step 5: Checking Spring Boot logs..."
echo "Look for any ERROR messages in your Spring Boot terminal"
echo ""
echo "Step 6: If still failing, check JWT token claims:"
echo "Decode your token at https://jwt.io to see the claims"
echo "Look for 'preferred_username', 'sub', or 'name' fields"
