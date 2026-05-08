#!/bin/bash

set -e

echo "=== 1. Maven clean package ==="
mvn clean package -DskipTests

echo "=== 2. stop old container ==="
docker-compose down

echo "=== 3. del old images ==="
docker rmi -f mysql redis eureka-service user-service moneytransfer-service gateway-service || true

echo "=== 4. use docker-compose build and start ==="
docker-compose up --build -d

echo "=== deploy finish ==="