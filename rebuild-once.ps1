$ErrorActionPreference = "Stop"

$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$LogDir = Join-Path $ProjectRoot "logs"
$LogFile = Join-Path $LogDir "rebuild-once.log"

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
Set-Location $ProjectRoot

Write-Host "Checking Docker Desktop..."
docker info *> $null
if ($LASTEXITCODE -ne 0) {
  Write-Host "Docker Desktop is not running yet."
  Write-Host "Open Docker Desktop first, wait until it says Engine running, then run:"
  Write-Host ".\rebuild-once.ps1"
  exit 1
}

$env:COMPOSE_PROGRESS = "quiet"

Write-Host "Rebuilding Cinema once. This can take a while. Output is saved to logs/rebuild-once.log"
docker compose up --build -d *> $LogFile
if ($LASTEXITCODE -ne 0) {
  Write-Host "Rebuild failed. Check log: $LogFile"
  exit $LASTEXITCODE
}

Write-Host "Rebuild complete:"
Write-Host "Frontend: http://localhost:3000"
Write-Host "Backend:  http://localhost:8080/api"
Write-Host "Logs:     $LogFile"
