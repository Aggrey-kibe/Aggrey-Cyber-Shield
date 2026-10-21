<#
.SYNOPSIS
  Keeps the AGGREY SHIELD threat lists fresh by registering (or removing) a Windows scheduled task.

.DESCRIPTION
  Every hour the task runs:  python -m app.services.intel.cli refresh --all --if-stale
  That command does real work only when a list is older than 20 hours, so most runs finish in about a second.
  The task runs as YOU, only while you are logged in (no password stored), and catches up after sleep/shutdown.
  Output is appended to:  %LOCALAPPDATA%\AggreyShield\feed-refresh.log

.EXAMPLE
  .\scripts\windows\schedule-feed-refresh.ps1            # register (or update) the hourly task
  .\scripts\windows\schedule-feed-refresh.ps1 -Remove    # remove it again
#>
[CmdletBinding()]
param(
    [switch]$Remove,
    [ValidateRange(1, 24)][int]$EveryHours = 1
)

$ErrorActionPreference = "Stop"
$taskName = "AggreyShield-FeedRefresh"

if ($Remove) {
    if (Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue) {
        Unregister-ScheduledTask -TaskName $taskName -Confirm:$false
        Write-Host "Removed scheduled task '$taskName'."
    } else {
        Write-Host "No scheduled task named '$taskName' exists. Nothing to remove."
    }
    return
}

$backend = (Resolve-Path (Join-Path $PSScriptRoot "..\..\backend")).Path
$python  = Join-Path $backend ".venv\Scripts\python.exe"
if (-not (Test-Path $python)) {
    throw "Could not find $python. Create the virtual environment first (see README: Install)."
}

$logDir = Join-Path $env:LOCALAPPDATA "AggreyShield"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$log = Join-Path $logDir "feed-refresh.log"

# cmd.exe is used only to append the output to the log file.
$inner    = "`"$python`" -m app.services.intel.cli refresh --all --if-stale >> `"$log`" 2>&1"
$action   = New-ScheduledTaskAction -Execute "cmd.exe" -Argument "/c `"$inner`"" -WorkingDirectory $backend
$trigger  = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(2) `
            -RepetitionInterval (New-TimeSpan -Hours $EveryHours) -RepetitionDuration (New-TimeSpan -Days 3650)
$settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -MultipleInstances IgnoreNew `
            -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit (New-TimeSpan -Hours 2)

Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings `
    -Description "Refreshes the AGGREY SHIELD threat lists when they are older than 20 hours." -Force | Out-Null

Write-Host "Registered '$taskName': runs every $EveryHours hour(s), first run in about 2 minutes."
Write-Host "Log file : $log"
Write-Host "Run now  : Start-ScheduledTask -TaskName $taskName"
Write-Host "Check    : & `"$python`" -m app.services.intel.cli status   (run from $backend)"
Write-Host "Remove   : .\scripts\windows\schedule-feed-refresh.ps1 -Remove"
