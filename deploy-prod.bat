@echo off
REM Production Deployment Script for SWS Gateway (Windows)
setlocal enabledelayedexpansion

echo Starting SWS Gateway Production Deployment...

REM Check if .env.prod exists
if not exist ".env.prod" (
    echo Error: .env.prod file not found. Please copy .env.prod.example to .env.prod and configure it.
    exit /b 1
)

REM Load environment variables from .env.prod
for /f "usebackq tokens=1,2 delims==" %%a in (".env.prod") do (
    if not "%%a"=="" if not "%%a:~0,1%"=="#" (
        set "%%a=%%b"
    )
)

REM Validate required environment variables
if "%DATABASE_URL%"=="" (
    echo Error: Required environment variable DATABASE_URL is not set in .env.prod
    exit /b 1
)
if "%DATABASE_USERNAME%"=="" (
    echo Error: Required environment variable DATABASE_USERNAME is not set in .env.prod
    exit /b 1
)
if "%DATABASE_PASSWORD%"=="" (
    echo Error: Required environment variable DATABASE_PASSWORD is not set in .env.prod
    exit /b 1
)

REM Build the application
echo Building application...
call mvn clean package -DskipTests -Pprod
if errorlevel 1 (
    echo Error: Build failed
    exit /b 1
)

REM Stop existing containers
echo Stopping existing containers...
docker-compose -f docker-compose.prod.yml down

REM Build and start new containers
echo Building and starting new containers...
docker-compose -f docker-compose.prod.yml up --build -d
if errorlevel 1 (
    echo Error: Failed to start containers
    exit /b 1
)

REM Wait for services to be healthy
echo Waiting for services to be healthy...
timeout /t 30 /nobreak > nul

REM Check health
echo Checking application health...
set max_attempts=30
set attempt=1

:health_check_loop
if !attempt! gtr !max_attempts! goto health_check_failed

curl -f http://localhost:%PORT%/api/v1/actuator/health > nul 2>&1
if errorlevel 1 (
    echo Attempt !attempt!/!max_attempts!: Application not ready yet...
    timeout /t 10 /nobreak > nul
    set /a attempt+=1
    goto health_check_loop
) else (
    echo Application is healthy!
    goto health_check_success
)

:health_check_failed
echo Error: Application failed to become healthy within expected time
echo Checking logs...
docker-compose -f docker-compose.prod.yml logs sws-gateway
exit /b 1

:health_check_success
REM Show running containers
echo Deployment completed successfully!
echo Running containers:
docker-compose -f docker-compose.prod.yml ps

echo.
echo Application is available at:
echo   HTTP:  http://localhost:%PORT%/api/v1
echo   Health: http://localhost:%PORT%/api/v1/actuator/health
echo   Metrics: http://localhost:%PORT%/api/v1/actuator/metrics

echo.
echo To view logs:
echo   docker-compose -f docker-compose.prod.yml logs -f sws-gateway

echo.
echo To stop the application:
echo   docker-compose -f docker-compose.prod.yml down

endlocal