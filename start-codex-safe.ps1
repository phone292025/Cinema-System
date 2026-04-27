$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$LogDir = Join-Path $ProjectRoot "logs"
$LogFile = Join-Path $LogDir "start-codex-safe.log"

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
Set-Location $ProjectRoot

Write-Host "Checking Docker Desktop..."
docker info *> $null
if ($LASTEXITCODE -ne 0) {
  Write-Host "Docker Desktop is not running yet."
  Write-Host "Open Docker Desktop first, wait until it says Engine running, then run:"
  Write-Host ".\start-codex-safe.ps1"
  exit 1
}

$env:COMPOSE_PROGRESS = "quiet"

Write-Host "Starting Cinema without rebuild. Output is saved to logs/start-codex-safe.log"
docker compose up -d --no-build *> $LogFile
if ($LASTEXITCODE -ne 0) {
  Write-Host "Start failed. The Docker images may not exist yet."
  Write-Host "Run .\rebuild-once.ps1 one time, then use .\start-codex-safe.ps1 after that."
  Write-Host "Log: $LogFile"
  exit $LASTEXITCODE
}

Write-Host "Cinema is starting:"
Write-Host "Frontend: http://localhost:3000"
Write-Host "Backend:  http://localhost:8080/api"
Write-Host "Logs:     $LogFile"
