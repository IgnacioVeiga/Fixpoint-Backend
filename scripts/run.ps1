$EnvironmentName = if ($args.Count -gt 0) { $args[0] } else { "dev" }
$Mode = if ($args.Count -gt 1) { $args[1] } else { "local" }
$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = (Resolve-Path (Join-Path $ScriptRoot "..")).Path
$EnvFile = Join-Path $ProjectRoot ".env.$EnvironmentName"
$DbContainerName = "Fixpoint_DB"

function Load-EnvironmentFile {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    Get-Content $Path | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }

        $parts = $line.Split("=", 2)
        if ($parts.Count -ne 2) {
            return
        }

        $key = $parts[0].Trim()
        $value = $parts[1].Trim()
        if (-not $key) {
            return
        }

        if (
            ($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))
        ) {
            $value = $value.Substring(1, $value.Length - 2)
        }

        [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
    }
}

function Get-DatabaseEndpoint {
    param(
        [string]$DbUrl
    )

    if (-not $DbUrl) {
        return $null
    }

    if ($DbUrl -match '^jdbc:postgresql://(?<host>[^:/?#]+)(:(?<port>\d+))?/') {
        return [PSCustomObject]@{
            Host = $matches['host']
            Port = if ($matches['port']) { [int]$matches['port'] } else { 5432 }
            IsLocal = $matches['host'] -in @('localhost', '127.0.0.1')
        }
    }

    return $null
}

function Test-TcpEndpoint {
    param(
        [Parameter(Mandatory = $true)]
        [string]$HostName,
        [Parameter(Mandatory = $true)]
        [int]$Port
    )

    $client = $null
    try {
        $client = [System.Net.Sockets.TcpClient]::new()
        $async = $client.BeginConnect($HostName, $Port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne(1500, $false)) {
            return $false
        }

        $client.EndConnect($async)
        return $true
    } catch {
        return $false
    } finally {
        if ($client) {
            $client.Close()
            $client.Dispose()
        }
    }
}

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

if (-not (Test-Path $EnvFile)) {
    Write-Host "Environment file not found: $EnvFile"
    Write-Host "Copy .env.example to $EnvFile and complete required values."
    exit 1
}

Load-EnvironmentFile -Path $EnvFile
$DatabaseEndpoint = Get-DatabaseEndpoint -DbUrl $env:DB_URL

if ($Mode -eq "docker") {
    Write-Host "Starting PostgreSQL container for backend using '$EnvFile'..."
    Push-Location $ProjectRoot
    try {
        docker compose --env-file $EnvFile up -d postgres
    } finally {
        Pop-Location
    }
    exit $LASTEXITCODE
}

if ($Mode -eq "auto") {
    $ShouldStartDocker = $true

    if ($DatabaseEndpoint -and -not $DatabaseEndpoint.IsLocal) {
        Write-Host "Auto mode detected a non-local DB_URL ($($DatabaseEndpoint.Host):$($DatabaseEndpoint.Port)). Skipping Docker startup."
        $ShouldStartDocker = $false
    } elseif ($DatabaseEndpoint -and (Test-TcpEndpoint -HostName $DatabaseEndpoint.Host -Port $DatabaseEndpoint.Port)) {
        Write-Host "Auto mode detected an existing database listener on $($DatabaseEndpoint.Host):$($DatabaseEndpoint.Port). Skipping Docker startup."
        $ShouldStartDocker = $false
    }

    if ($ShouldStartDocker) {
        if ($DatabaseEndpoint) {
            Write-Host "Auto mode did not find a database on $($DatabaseEndpoint.Host):$($DatabaseEndpoint.Port). Starting Docker..."
        } else {
            Write-Host "Auto mode could not parse DB_URL. Starting Docker as fallback..."
        }

        Push-Location $ProjectRoot
        try {
            docker compose --env-file $EnvFile up -d postgres
        } finally {
            Pop-Location
        }
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Docker startup failed. IntelliJ can still work if your database is already running outside Docker."
            exit $LASTEXITCODE
        }

        if (-not (Wait-ForDatabase)) {
            exit 1
        }
    }
}

if (-not $env:SPRING_PROFILES_ACTIVE) {
    $env:SPRING_PROFILES_ACTIVE = $EnvironmentName
}

Write-Host "Starting backend locally with profile '$($env:SPRING_PROFILES_ACTIVE)' using '$EnvFile'..."
Push-Location $ProjectRoot
try {
    & .\mvnw.cmd spring-boot:run
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
