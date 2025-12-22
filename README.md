# OPA Spring Boot Demo

This demo shows how to integrate Open Policy Agent (OPA) with Spring Boot for authorization.

## What is OPA?

Open Policy Agent (OPA) is an open-source, general-purpose policy engine that enables unified, context-aware policy enforcement across your entire technology stack. OPA provides:

- **Policy as Code**: Write authorization policies in Rego, a declarative language
- **Decoupled Architecture**: Separate policy decisions from application logic
- **Universal Integration**: Works with APIs, microservices, Kubernetes, CI/CD pipelines, and more
- **High Performance**: Fast policy evaluation with minimal latency
- **Cloud Native**: CNCF graduated project designed for modern architectures

OPA acts as a lightweight policy engine that your applications can query to make authorization decisions, removing the need to hardcode complex authorization logic in your business code.

## What This Application Does

This application demonstrates **externalized authorization** - separating authorization decisions from business logic. Instead of hardcoding "who can access what" in your Spring Boot code, we delegate these decisions to OPA (Open Policy Agent).

### The Problem We're Solving
Traditional applications embed authorization logic directly in code:
- Authorization rules scattered throughout the codebase
- Hard to change permissions without code deployment
- Difficult to audit and test authorization logic
- Can't reuse policies across different services

### Our Solution
We separate concerns:
- **Spring Boot**: Handles business logic (serving documents)
- **OPA Server**: Makes authorization decisions based on policies
- **Policy File**: Defines rules in declarative Rego language

### Example Flow
```
User Request → Spring Boot → "Can alice read document:1?" → OPA → Policy Check → Allow/Deny → Response
```

When Alice requests a document:
1. Spring Boot asks OPA: "Can alice read document:1?"
2. OPA checks policy: "Alice can read any document"
3. OPA returns: `{"result": true}`
4. Spring Boot serves the content

When Bob tries to access document:2:
1. Same process, but policy says "Bob can only read document:1"
2. OPA returns: `{"result": false}`
3. Spring Boot returns "Access denied" (403)

**Result**: Authorization rules are now version-controlled, testable, reusable, and updatable without code changes.

## Prerequisites

### Install OPA

#### macOS
```bash
# Using Homebrew
brew install opa

# Or download binary
curl -L -o opa https://openpolicyagent.org/downloads/v0.59.0/opa_darwin_amd64_static
chmod 755 ./opa
```

#### Linux
```bash
# Download binary
curl -L -o opa https://openpolicyagent.org/downloads/v0.59.0/opa_linux_amd64_static
chmod 755 ./opa

# Or using package manager (Ubuntu/Debian)
curl -L -o opa.deb https://openpolicyagent.org/downloads/v0.59.0/opa_linux_amd64.deb
sudo dpkg -i opa.deb
```

#### Windows
```powershell
# Download binary
Invoke-WebRequest -Uri https://openpolicyagent.org/downloads/v0.59.0/opa_windows_amd64.exe -OutFile opa.exe

# Or using Chocolatey
choco install opa

# Or using Scoop
scoop install opa
```

#### Docker
```bash
docker pull openpolicyagent/opa:latest
```

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

## Setup

1. **Start OPA server:**
   ```bash
   ./start-opa.sh
   ```
   This starts OPA on port 8181 with the policy loaded.

2. **Start Spring Boot application:**
   ```bash
   ./mvnw spring-boot:run
   ```
   Application runs on port 8081.

## Test the Demo

### Check access via REST API:
```bash
# Alice can read documents (returns {"allowed":true})
curl -X POST http://localhost:8081/api/check-access \
  -H "Content-Type: application/json" \
  -d '{"user":"alice","action":"read","resource":"document:1"}'

# Bob can only read document:1 (returns {"allowed":true})
curl -X POST http://localhost:8081/api/check-access \
  -H "Content-Type: application/json" \
  -d '{"user":"bob","action":"read","resource":"document:1"}'

# Bob cannot read document:2 (returns {"allowed":false})
curl -X POST http://localhost:8081/api/check-access \
  -H "Content-Type: application/json" \
  -d '{"user":"bob","action":"read","resource":"document:2"}'

# Admin can do anything (returns {"allowed":true})
curl -X POST http://localhost:8081/api/check-access \
  -H "Content-Type: application/json" \
  -d '{"user":"admin","action":"write","resource":"document:1"}'
```

### Access documents:
```bash
# Allowed: Alice reading document
curl http://localhost:8081/api/users/alice/documents/1
# Returns: "Document 1 content for user alice"

# Denied: Bob reading document 2
curl http://localhost:8081/api/users/bob/documents/2
# Returns: "Access denied" (HTTP 403)
```

## Policy Rules

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
