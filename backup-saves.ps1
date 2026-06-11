param(
    [string]$BackupRoot = ".data\saves\backup"
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$SaveDir = Join-Path $Root ".data\saves"
$BackupDir = Join-Path $Root (Join-Path $BackupRoot (Get-Date -Format "yyyyMMdd-HHmmss"))
$Sqlite = Get-Command sqlite3 -ErrorAction SilentlyContinue

New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null

$Databases = @(
    "game.db",
    "game_w2.db"
)

foreach ($dbName in $Databases) {
    $source = Join-Path $SaveDir $dbName
    if (-not (Test-Path -LiteralPath $source)) {
        Write-Warning "Skipped missing database: $source"
        continue
    }

    $target = Join-Path $BackupDir $dbName
    if ($Sqlite) {
        & $Sqlite.Source $source ".backup '$target'"
        if ($LASTEXITCODE -ne 0) {
            throw "sqlite3 backup failed for $dbName"
        }
        Write-Host "Backed up $dbName with sqlite3 .backup"
        continue
    }

    Copy-Item -LiteralPath $source -Destination $target -Force
    foreach ($suffix in @("-wal", "-shm")) {
        $sidecar = "$source$suffix"
        if (Test-Path -LiteralPath $sidecar) {
            Copy-Item -LiteralPath $sidecar -Destination (Join-Path $BackupDir "$dbName$suffix") -Force
        }
    }
    Write-Warning "sqlite3 not found; copied $dbName and WAL/SHM sidecars instead."
}

Write-Host "Backup complete: $BackupDir"
