# OPA Spring Boot Integration Demo

## Overview
This demo demonstrates how to integrate Open Policy Agent (OPA) with Spring Boot for authorization decisions. OPA acts as an external policy engine that evaluates authorization requests based on defined policies.

## Architecture
```
Spring Boot App ──HTTP──> OPA Server
     │                        │
     │                    policy.rego
     │                        │
User Request ────────> Authorization Decision
```

## Components

### 1. OPA Policy (`policy.rego`)
Defines authorization rules using Rego language:
- `admin`: Full access to everything
- `alice`: Can read any document
- `bob`: Can only read document:1

### 2. OpaService
Spring service that communicates with OPA server via HTTP:
- Sends authorization requests to OPA
- Returns boolean decision (allow/deny)

### 3. DemoController
REST endpoints that demonstrate OPA integration:
- `/api/check-access` - Direct policy evaluation
- `/api/users/{userId}/documents/{docId}` - Document access with authorization

## Setup & Usage

### Start OPA Server
```bash
./start-opa.sh
```
This starts OPA on port 8181 with the policy loaded.

### Start Spring Boot
```bash
./mvnw spring-boot:run
```
Application runs on port 8081.

### Test Authorization

#### Check Access Endpoint
```bash
# Alice can read documents (returns {"allowed":true})
curl -X POST http://localhost:8081/api/check-access \
  -H "Content-Type: application/json" \
  -d '{"user":"alice","action":"read","resource":"document:1"}'

# Bob cannot read document:2 (returns {"allowed":false})
curl -X POST http://localhost:8081/api/check-access \
  -H "Content-Type: application/json" \
  -d '{"user":"bob","action":"read","resource":"document:2"}'

# Admin can do anything (returns {"allowed":true})
curl -X POST http://localhost:8081/api/check-access \
  -H "Content-Type: application/json" \
  -d '{"user":"admin","action":"write","resource":"document:1"}'
```

#### Document Access Endpoint
```bash
# Allowed: Alice reading document
curl http://localhost:8081/api/users/alice/documents/1
# Returns: "Document 1 content for user alice"

# Denied: Bob reading document 2
curl http://localhost:8081/api/users/bob/documents/2
# Returns: "Access denied" (HTTP 403)
```

## Policy Rules Explained

The Rego policy defines three authorization rules:

1. **Admin Rule**: `input.user == "admin"` - Admin has unrestricted access
2. **Alice Rule**: Alice can read any document that starts with "document:"
3. **Bob Rule**: Bob can only read "document:1"

## Key Features

- **Externalized Authorization**: Policy decisions are made outside the application
- **Policy as Code**: Authorization rules are version-controlled and testable
- **Flexible Policies**: Easy to modify rules without changing application code
- **Centralized Decisions**: Single policy engine for multiple services

## Benefits of OPA Integration

1. **Separation of Concerns**: Business logic separate from authorization logic
2. **Policy Reusability**: Same policies can be used across multiple services
3. **Dynamic Policies**: Policies can be updated without redeploying applications
4. **Audit Trail**: All authorization decisions are logged by OPA
5. **Testing**: Policies can be unit tested independently
