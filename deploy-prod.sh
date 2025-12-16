#!/bin/bash

# Production Deployment Script for SWS Gateway
set -e

echo "Starting SWS Gateway Production Deployment..."

# Check if .env.prod exists
if [ ! -f ".env.prod" ]; then
    echo "Error: .env.prod file not found. Please copy .env.prod.example to .env.prod and configure it."
    exit 1
fi

# Load environment variables
source .env.prod

# Validate required environment variables
required_vars=("DATABASE_URL" "DATABASE_USERNAME" "DATABASE_PASSWORD")
for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "Error: Required environment variable $var is not set in .env.prod"
        exit 1
    fi
done

# Create log directory if it doesn't exist
sudo mkdir -p /var/log/sws-gateway
sudo chown -R $USER:$USER /var/log/sws-gateway

# Build the application
echo "Building application..."
mvn clean package -DskipTests -Pprod

# Stop existing containers
echo "Stopping existing containers..."
docker-compose -f docker-compose.prod.yml down

# Build and start new containers
echo "Building and starting new containers..."
docker-compose -f docker-compose.prod.yml up --build -d

# Wait for services to be healthy
echo "Waiting for services to be healthy..."
sleep 30

# Check health
echo "Checking application health..."
max_attempts=30
attempt=1

while [ $attempt -le $max_attempts ]; do
    if curl -f http://localhost:${PORT:-8080}/api/v1/actuator/health > /dev/null 2>&1; then
        echo "Application is healthy!"
        break
    else
        echo "Attempt $attempt/$max_attempts: Application not ready yet..."
        sleep 10
        ((attempt++))
    fi
done

if [ $attempt -gt $max_attempts ]; then
    echo "Error: Application failed to become healthy within expected time"
    echo "Checking logs..."
    docker-compose -f docker-compose.prod.yml logs sws-gateway
    exit 1
fi

# Show running containers
echo "Deployment completed successfully!"
echo "Running containers:"
docker-compose -f docker-compose.prod.yml ps

echo ""
echo "Application is available at:"
echo "  HTTP:  http://localhost:${PORT:-8080}/api/v1"
echo "  Health: http://localhost:${PORT:-8080}/api/v1/actuator/health"
echo "  Metrics: http://localhost:${PORT:-8080}/api/v1/actuator/metrics"

echo ""
echo "To view logs:"
echo "  docker-compose -f docker-compose.prod.yml logs -f sws-gateway"

echo ""
echo "To stop the application:"
echo "  docker-compose -f docker-compose.prod.yml down"