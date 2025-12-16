# SWS Gateway - Production Deployment Guide

This guide provides instructions for deploying the SWS Gateway to production environments.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- PostgreSQL database
- SSL certificates (for HTTPS)

## Configuration Files

### 1. Environment Configuration

Copy the example environment file and configure it for your production environment:

```bash
cp .env.prod.example .env.prod
```

Edit `.env.prod` with your production values:

```bash
# Database Configuration
DATABASE_URL=jdbc:postgresql://your-prod-db-host:5432/sws_gateway
DATABASE_USERNAME=sws_gateway_user
DATABASE_PASSWORD=your_secure_password

# Application Configuration
PORT=8080
CONTEXT_PATH=/api/v1

# Security and Performance
DB_POOL_MAX_SIZE=20
PROXY_TIMEOUT=30000
```

### 2. SSL Certificates

Place your SSL certificates in the `ssl/` directory:
- `ssl/cert.pem` - SSL certificate
- `ssl/key.pem` - Private key

## Deployment Options

### Option 1: Docker Compose (Recommended)

1. **Build and Deploy**:
   ```bash
   # Linux/Mac
   ./deploy-prod.sh
   
   # Windows
   deploy-prod.bat
   ```

2. **Manual Deployment**:
   ```bash
   # Build the application
   mvn clean package -DskipTests -Pprod
   
   # Start services
   docker-compose -f docker-compose.prod.yml up -d
   ```

### Option 2: Standalone JAR

1. **Build the application**:
   ```bash
   mvn clean package -DskipTests -Pprod
   ```

2. **Run with production profile**:
   ```bash
   java -Xms512m -Xmx1024m \
        -XX:+UseG1GC \
        -Dspring.profiles.active=prod \
        -jar target/sws-gateway-*.jar
   ```

## Production Configuration Details

### Application Properties

The production configuration (`application-prod.properties`) includes:

- **Database Connection Pooling**: Optimized HikariCP settings
- **Logging**: Structured logging with file rotation
- **Security**: Secure cookie settings and headers
- **Monitoring**: Actuator endpoints for health checks and metrics
- **Performance**: HTTP compression and timeout configurations

### Logging

Production logs are configured with:
- **File Location**: `/var/log/sws-gateway/application.log`
- **Rotation**: Daily rotation with 30-day retention
- **Size Limit**: 100MB per file, 1GB total
- **Error Logs**: Separate error log file for critical issues

### Monitoring and Health Checks

Available endpoints:
- **Health Check**: `GET /api/v1/actuator/health`
- **Metrics**: `GET /api/v1/actuator/metrics`
- **Prometheus**: `GET /api/v1/actuator/prometheus`

### Security Features

- **HTTPS Only**: Automatic HTTP to HTTPS redirect
- **Security Headers**: HSTS, X-Frame-Options, CSP
- **Rate Limiting**: API rate limiting via nginx
- **Secure Cookies**: HttpOnly and Secure flags enabled

## Database Setup

### PostgreSQL Configuration

1. **Create Database**:
   ```sql
   CREATE DATABASE sws_gateway;
   CREATE USER sws_gateway_user WITH PASSWORD 'your_secure_password';
   GRANT ALL PRIVILEGES ON DATABASE sws_gateway TO sws_gateway_user;
   ```

2. **Run Database Migrations**:
   The application uses Hibernate with `validate` mode, so ensure your database schema is up to date.

## Load Balancing with Nginx

The included nginx configuration provides:
- **SSL Termination**: HTTPS handling
- **Load Balancing**: Multiple backend instances
- **Rate Limiting**: API protection
- **Static Content**: Efficient serving
- **Health Checks**: Upstream monitoring

## Monitoring and Alerting

### Prometheus Metrics

The application exposes metrics at `/api/v1/actuator/prometheus` including:
- HTTP request metrics
- Database connection pool metrics
- JVM metrics
- Custom business metrics

### Health Checks

Health check endpoint provides:
- Database connectivity status
- Application status
- Disk space status

## Troubleshooting

### Common Issues

1. **Database Connection Issues**:
   - Verify database URL and credentials
   - Check network connectivity
   - Ensure database is running

2. **SSL Certificate Issues**:
   - Verify certificate files exist and are readable
   - Check certificate validity
   - Ensure private key matches certificate

3. **Memory Issues**:
   - Adjust JVM heap settings (`-Xms`, `-Xmx`)
   - Monitor memory usage via metrics
   - Consider increasing container memory limits

### Log Analysis

View application logs:
```bash
# Docker logs
docker-compose -f docker-compose.prod.yml logs -f sws-gateway

# File logs
tail -f /var/log/sws-gateway/application.log
```

### Performance Tuning

1. **Database Connection Pool**:
   - Adjust `DB_POOL_MAX_SIZE` based on load
   - Monitor connection usage

2. **JVM Tuning**:
   - Use G1GC for better performance
   - Adjust heap size based on memory usage

3. **Nginx Tuning**:
   - Adjust worker connections
   - Configure appropriate buffer sizes

## Backup and Recovery

### Database Backup

```bash
pg_dump -h your-db-host -U sws_gateway_user sws_gateway > backup.sql
```

### Configuration Backup

Ensure the following files are backed up:
- `.env.prod`
- `ssl/` directory
- Custom configuration files

## Security Considerations

1. **Environment Variables**: Never commit `.env.prod` to version control
2. **SSL Certificates**: Keep private keys secure and rotate regularly
3. **Database Credentials**: Use strong passwords and rotate regularly
4. **Network Security**: Use firewalls and VPNs for database access
5. **Updates**: Keep dependencies and base images updated

## Scaling

### Horizontal Scaling

To scale horizontally:
1. Update docker-compose to run multiple instances
2. Configure nginx load balancing
3. Ensure database can handle increased connections

### Vertical Scaling

To scale vertically:
1. Increase JVM heap size
2. Increase database connection pool size
3. Adjust container resource limits

## Support

For production support:
- Check application logs first
- Monitor health check endpoints
- Review metrics for performance issues
- Contact development team with specific error messages and logs