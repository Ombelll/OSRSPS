param(
    [string]$LogRoot = ".data\logs",
    [string]$ArchiveRoot = ".data\logs\archive",
    [int]$MaxLogMegabytes = 10,
    [int]$KeepDays = 14
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$LogDir = Join-Path $Root $LogRoot
$ArchiveDir = Join-Path $Root $ArchiveRoot
$MaxBytes = $MaxLogMegabytes * 1MB
$Now = Get-Date

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
New-Item -ItemType Directory -Force -Path $ArchiveDir | Out-Null

Get-ChildItem -LiteralPath $LogDir -File -Filter "*.log" | ForEach-Object {
    if ($_.Length -lt $MaxBytes) {
        return
    }

    $stamp = $Now.ToString("yyyyMMdd-HHmmss")
    $archiveName = "{0}.{1}.log" -f $_.BaseName, $stamp
    $archivePath = Join-Path $ArchiveDir $archiveName
    try {
        Move-Item -LiteralPath $_.FullName -Destination $archivePath -Force
        New-Item -ItemType File -Path $_.FullName -Force | Out-Null
        Write-Host "Rotated $($_.Name) -> $archiveName"
    } catch {
        Write-Warning "Skipped locked log $($_.Name): $($_.Exception.Message)"
    }
}

$cutoff = $Now.AddDays(-$KeepDays)
Get-ChildItem -LiteralPath $ArchiveDir -File -Filter "*.log" |
    Where-Object { $_.LastWriteTime -lt $cutoff } |
    Remove-Item -Force

Write-Host "Log rotation complete: $LogDir"
