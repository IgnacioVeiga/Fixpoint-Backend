@echo off
setlocal EnableDelayedExpansion

set ENVIRONMENT=%1
if "%ENVIRONMENT%"=="" set ENVIRONMENT=dev
set MODE=%2
if "%MODE%"=="" set MODE=local
set ENV_FILE=.env.%ENVIRONMENT%
set DB_CONTAINER_NAME=Fixpoint_DB

if /I "%MODE%"=="docker" (
    if not exist "%ENV_FILE%" (
        echo Environment file not found: %ENV_FILE%
        echo Copy .env.example to %ENV_FILE% and complete required values.
        exit /b 1
    )

    echo Starting PostgreSQL container for backend using '%ENV_FILE%'...
    docker compose --env-file "%ENV_FILE%" up -d postgres
    exit /b %ERRORLEVEL%
)

if /I "%MODE%"=="auto" (
    if not exist "%ENV_FILE%" (
        echo Environment file not found: %ENV_FILE%
        echo Copy .env.example to %ENV_FILE% and complete required values.
        exit /b 1
    )

    echo Starting PostgreSQL container for backend using '%ENV_FILE%'...
    docker compose --env-file "%ENV_FILE%" up -d postgres
    if errorlevel 1 exit /b %ERRORLEVEL%

    call :wait_for_db
    if errorlevel 1 exit /b 1
)

if not exist "%ENV_FILE%" (
    echo Environment file not found: %ENV_FILE%
    echo Copy .env.example to %ENV_FILE% and complete required values.
    exit /b 1
)

for /f "usebackq tokens=1* delims==" %%A in ("%ENV_FILE%") do (
    set KEY=%%A
    set VALUE=%%B
    if not "!KEY!"=="" (
        if not "!KEY:~0,1!"=="#" (
            set "%%A=%%B"
        )
    )
)

if "%SPRING_PROFILES_ACTIVE%"=="" set SPRING_PROFILES_ACTIVE=%ENVIRONMENT%

echo Starting backend locally with profile '%SPRING_PROFILES_ACTIVE%' using '%ENV_FILE%'...
call mvnw.cmd spring-boot:run
exit /b %ERRORLEVEL%

:wait_for_db
set ATTEMPT=1
set MAX_ATTEMPTS=30

:health_loop
set HEALTH=
for /f %%S in ('docker inspect --format "{{.State.Health.Status}}" %DB_CONTAINER_NAME% 2^>nul') do set HEALTH=%%S

if /I "!HEALTH!"=="healthy" (
    echo Database container '%DB_CONTAINER_NAME%' is healthy.
    exit /b 0
)

if !ATTEMPT! GEQ !MAX_ATTEMPTS! (
    echo Database container '%DB_CONTAINER_NAME%' did not become healthy in time.
    exit /b 1
)

echo Waiting for database health (!ATTEMPT!/!MAX_ATTEMPTS!)...
set /a ATTEMPT+=1
timeout /t 2 /nobreak >nul
goto :health_loop