param(
    [int]$IntervalSeconds = 60,
    [switch]$Once
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$Bash = "C:\Program Files\Git\bin\bash.exe"
$LogDir = Join-Path $Root ".data\logs"
$App = "./server/app/build/install/app/bin/app"

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$Worlds = @(
    @{
        Name = "W1"
        Port = 43594
        Command = "cd /c/Users/Mike/rsmod && exec $App"
        Out = Join-Path $LogDir "world1.out.log"
        Err = Join-Path $LogDir "world1.err.log"
    },
    @{
        Name = "W2"
        Port = 43595
        Command = "cd /c/Users/Mike/rsmod && export RSMOD_WORLD=2 RSMOD_PORT=43595 RSMOD_DB=.data/saves/game_w2.db && exec $App"
        Out = Join-Path $LogDir "world2.out.log"
        Err = Join-Path $LogDir "world2.err.log"
    }
)

function Test-PortListening {
    param([int]$Port)
    return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Start-World {
    param([hashtable]$World)

    $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    "[$timestamp] Starting $($World.Name) on port $($World.Port)" | Tee-Object -FilePath (Join-Path $LogDir "watchdog.log") -Append
    $arguments = "-lc ""$($World.Command)"""
    Start-Process `
        -FilePath $Bash `
        -ArgumentList $arguments `
        -WorkingDirectory $Root `
        -WindowStyle Hidden `
        -RedirectStandardOutput $World.Out `
        -RedirectStandardError $World.Err
}

do {
    foreach ($world in $Worlds) {
        if (-not (Test-PortListening -Port $world.Port)) {
            Start-World -World $world
            Start-Sleep -Seconds 5
        }
    }

    if ($Once) {
        break
    }

    Start-Sleep -Seconds $IntervalSeconds
} while ($true)
