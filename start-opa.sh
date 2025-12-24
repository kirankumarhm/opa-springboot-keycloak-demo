#!/bin/bash

# Start OPA server with the policy
echo "Starting OPA server..."

# Check if OPA is installed
if ! command -v opa &> /dev/null; then
    echo "Error: OPA is not installed or not in PATH"
    echo "Please install OPA first:"
    echo "  macOS: brew install opa"
    echo "  Linux: Download from https://openpolicyagent.org/downloads/"
    exit 1
fi

# Check if policy file exists
if [ ! -f "policy.rego" ]; then
    echo "Error: policy.rego file not found"
    echo "Please ensure policy.rego exists in the current directory"
    exit 1
fi

echo "Starting OPA server on port 8181...
opa run --server --addr :8181 policy.rego
