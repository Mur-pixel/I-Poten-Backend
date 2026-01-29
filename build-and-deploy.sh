#!/bin/bash

# Build and deploy script for multi-architecture Docker image

set -e

echo "Building multi-architecture Docker image..."

# Build for both AMD64 and ARM64
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t ghcr.io/imcoder0000/spring-backend:latest \
  -f Dockerfile.multiarch \
  --push .

echo "Multi-architecture image built and pushed successfully!"

# Alternative: Build only for AMD64 (EC2 x86_64)
echo "Building AMD64-only image for EC2..."
docker buildx build \
  --platform linux/amd64 \
  -t ghcr.io/imcoder0000/spring-backend:amd64 \
  -f Dockerfile \
  --push .

echo "AMD64 image built and pushed successfully!"
