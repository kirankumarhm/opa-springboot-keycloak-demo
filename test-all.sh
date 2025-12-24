#!/bin/bash

echo "========================================="
echo "OPA Spring Boot Demo - Test Suite"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print test results
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASS${NC}: $2"
    else
        echo -e "${RED}✗ FAIL${NC}: $2"
    fi
}

# Check if services are running
echo "Step 1: Checking if services are running..."
echo ""

curl -s http://localhost:8080/health > /dev/null 2>&1
print_result $? "Keycloak is running"

curl -s http://localhost:8181/health > /dev/null 2>&1
print_result $? "OPA is running"

curl -s http://localhost:8081/actuator/health > /dev/null 2>&1
print_result $? "Spring Boot is running"

echo ""
echo "Step 2: Getting JWT tokens..."
echo ""

# Get tokens
ALICE_TOKEN=$(curl -s -X POST http://localhost:8080/realms/demo/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=opa-demo" \
  -d "username=alice" \
  -d "password=password" | jq -r '.access_token')

if [ "$ALICE_TOKEN" != "null" ] && [ -n "$ALICE_TOKEN" ]; then
    echo -e "${GREEN}✓${NC} Alice token obtained"
else
    echo -e "${RED}✗${NC} Failed to get Alice token"
    exit 1
fi

BOB_TOKEN=$(curl -s -X POST http://localhost:8080/realms/demo/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=opa-demo" \
  -d "username=bob" \
  -d "password=password" | jq -r '.access_token')

if [ "$BOB_TOKEN" != "null" ] && [ -n "$BOB_TOKEN" ]; then
    echo -e "${GREEN}✓${NC} Bob token obtained"
else
    echo -e "${RED}✗${NC} Failed to get Bob token"
    exit 1
fi

ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/realms/demo/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=opa-demo" \
  -d "username=admin" \
  -d "password=password" | jq -r '.access_token')

if [ "$ADMIN_TOKEN" != "null" ] && [ -n "$ADMIN_TOKEN" ]; then
    echo -e "${GREEN}✓${NC} Admin token obtained"
else
    echo -e "${RED}✗${NC} Failed to get Admin token"
    exit 1
fi

echo ""
echo "Step 3: Testing authorization scenarios..."
echo ""

# Test 1: Alice can read document 1
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ALICE_TOKEN" \
  http://localhost:8081/api/users/alice/documents/1)
if [ "$RESPONSE" = "200" ]; then
    echo -e "${GREEN}✓${NC} Alice can read document 1"
else
    echo -e "${RED}✗${NC} Alice cannot read document 1 (HTTP $RESPONSE)"
fi

# Test 2: Alice can read document 2
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ALICE_TOKEN" \
  http://localhost:8081/api/users/alice/documents/2)
if [ "$RESPONSE" = "200" ]; then
    echo -e "${GREEN}✓${NC} Alice can read document 2"
else
    echo -e "${RED}✗${NC} Alice cannot read document 2 (HTTP $RESPONSE)"
fi

# Test 3: Bob can read document 1
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $BOB_TOKEN" \
  http://localhost:8081/api/users/bob/documents/1)
if [ "$RESPONSE" = "200" ]; then
    echo -e "${GREEN}✓${NC} Bob can read document 1"
else
    echo -e "${RED}✗${NC} Bob cannot read document 1 (HTTP $RESPONSE)"
fi

# Test 4: Bob cannot read document 2
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $BOB_TOKEN" \
  http://localhost:8081/api/users/bob/documents/2)
if [ "$RESPONSE" = "403" ]; then
    echo -e "${GREEN}✓${NC} Bob correctly denied access to document 2"
else
    echo -e "${RED}✗${NC} Bob should be denied document 2 (HTTP $RESPONSE)"
fi

# Test 5: Admin can read any document
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8081/api/users/admin/documents/999)
if [ "$RESPONSE" = "200" ]; then
    echo -e "${GREEN}✓${NC} Admin can read any document"
else
    echo -e "${RED}✗${NC} Admin cannot read document (HTTP $RESPONSE)"
fi

# Test 6: Unauthenticated request should fail
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" \
  http://localhost:8081/api/users/alice/documents/1)
if [ "$RESPONSE" = "401" ]; then
    echo -e "${GREEN}✓${NC} Unauthenticated request correctly denied"
else
    echo -e "${RED}✗${NC} Unauthenticated request should return 401 (HTTP $RESPONSE)"
fi

# Test 7: Public endpoint works without token
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
  -H "Content-Type: application/json" \
  -d '{"user":"alice","action":"read","resource":"document:1"}' \
  http://localhost:8081/api/public/check-access)
if [ "$RESPONSE" = "200" ]; then
    echo -e "${GREEN}✓${NC} Public endpoint accessible without token"
else
    echo -e "${RED}✗${NC} Public endpoint failed (HTTP $RESPONSE)"
fi

# Test 8: Check access endpoint
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
  -H "Authorization: Bearer $ALICE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"action":"read","resource":"document:1"}' \
  http://localhost:8081/api/check-access)
if [ "$RESPONSE" = "200" ]; then
    echo -e "${GREEN}✓${NC} Check access endpoint works"
else
    echo -e "${RED}✗${NC} Check access endpoint failed (HTTP $RESPONSE)"
fi

echo ""
echo "========================================="
echo "Test Suite Complete!"
echo "========================================="
