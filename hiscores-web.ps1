$ErrorActionPreference = "Stop"

$Repo = Split-Path -Parent $MyInvocation.MyCommand.Path
$Server = Join-Path $Repo "tools\hiscores-web\server.py"
$Port = if ($env:HISCORES_PORT) { [int]$env:HISCORES_PORT } else { 8088 }
$Python = Get-Command python -ErrorAction SilentlyContinue

if (-not $Python) {
    $Python = Get-Command py -ErrorAction SilentlyContinue
}

if (-not $Python) {
    throw "Python was not found on PATH. Install Python or run tools\hiscores-web\server.py with your Python runtime."
}

& $Python.Source $Server --repo $Repo --port $Port
