$EnvironmentName = if ($args.Count -gt 0) { $args[0] } else { "dev" }
$Mode = if ($args.Count -gt 1) { $args[1] } else { "local" }
$EnvFile = ".env.$EnvironmentName"
$DbContainerName = "Fixpoint_DB"

function Wait-ForDatabase {
    $MaxAttempts = 30
    $DelaySeconds = 2

    for ($Attempt = 1; $Attempt -le $MaxAttempts; $Attempt++) {
        $Health = docker inspect --format "{{.State.Health.Status}}" $DbContainerName 2>$null
        if ($Health -eq "healthy") {
            Write-Host "Database container '$DbContainerName' is healthy."
            return $true
        }

        Write-Host "Waiting for database health ($Attempt/$MaxAttempts)..."
        Start-Sleep -Seconds $DelaySeconds
    }

    Write-Host "Database container '$DbContainerName' did not become healthy in time."
    return $false
}

if ($Mode -eq "docker" -or $Mode -eq "auto") {
    if (-not (Test-Path $EnvFile)) {
        Write-Host "Environment file not found: $EnvFile"
        Write-Host "Copy .env.example to $EnvFile and complete required values."
        exit 1
    }

    Write-Host "Starting PostgreSQL container for backend using '$EnvFile'..."
    docker compose --env-file $EnvFile up -d postgres
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }

    if ($Mode -eq "docker") {
        exit 0
    }

    if (-not (Wait-ForDatabase)) {
        exit 1
    }
}

if (-not (Test-Path $EnvFile)) {
    Write-Host "Environment file not found: $EnvFile"
    Write-Host "Copy .env.example to $EnvFile and complete required values."
    exit 1
}

Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#")) {
        $parts = $line.Split("=", 2)
        if ($parts.Count -eq 2) {
            [System.Environment]::SetEnvironmentVariable($parts[0], $parts[1])
        }
    }
}

if (-not $env:SPRING_PROFILES_ACTIVE) {
    $env:SPRING_PROFILES_ACTIVE = $EnvironmentName
}

Write-Host "Starting backend locally with profile '$($env:SPRING_PROFILES_ACTIVE)' using '$EnvFile'..."
.\mvnw.cmd spring-boot:run