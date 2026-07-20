# G-006 Q-01 — Strong XA recovery attempt (prepare → kill TM → recover + heuristic).
# Prefer killing the client/TM JVM. Do NOT kill XuGu server.
#
# Usage (from repo root):
#   powershell -ExecutionPolicy Bypass -File .\scripts\xa-recovery-strong.ps1
#
# Honesty: STRONG_PASS only if recover() returns in-doubt Xid(s) after kill and
# heuristic commit/rollback clears them. Otherwise STRONG_BLOCKED with reason.
# Document in docs/xa-recovery-evidence.md.

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
$Mvn = "C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd"
$LogDir = Join-Path $RepoRoot "tests-it\logs\xa-strong-tm"
$OutLog = Join-Path $RepoRoot "tests-it\logs\xa-recovery-strong.log"

Set-Location $RepoRoot

Write-Host "== compile tests-it =="
& $Mvn -pl tests-it -am test-compile "-DskipTests" | Out-Host
if ($LASTEXITCODE -ne 0) { throw "mvn test-compile failed: $LASTEXITCODE" }

$CpFile = Join-Path $env:TEMP "xugu-xa-strong-cp.txt"
& $Mvn -pl tests-it -q dependency:build-classpath "-Dmdep.outputFile=$CpFile" "-DincludeScope=test" | Out-Host
if ($LASTEXITCODE -ne 0) { throw "dependency:build-classpath failed: $LASTEXITCODE" }

$TestClasses = Join-Path $RepoRoot "tests-it\target\test-classes"
$MainClasses = Join-Path $RepoRoot "tests-it\target\classes"
$Deps = Get-Content -Raw $CpFile
$Cp = "$TestClasses;$MainClasses;$Deps"

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path $OutLog) | Out-Null
if (Test-Path $OutLog) { Remove-Item $OutLog -Force }
Get-ChildItem $LogDir -Force -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

$Main = "com.xugudb.shardingsphere.it.xa.XARecoveryStrongMain"

Write-Host "== start Strong prepare-hold (Atomikos log dir preserved under $LogDir) =="
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "java"
$psi.Arguments = "-cp `"$Cp`" $Main prepare-hold `"$LogDir`""
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.CreateNoWindow = $true
$proc = New-Object System.Diagnostics.Process
$proc.StartInfo = $psi
[void]$proc.Start()

$ready = $false
$deadline = (Get-Date).AddSeconds(90)
while ((Get-Date) -lt $deadline -and -not $proc.HasExited) {
    $line = $proc.StandardOutput.ReadLine()
    if ($null -eq $line) { Start-Sleep -Milliseconds 100; continue }
    Add-Content -Path $OutLog -Value $line
    Write-Host $line
    if ($line -like "READY_FOR_KILL*") {
        $ready = $true
        break
    }
}

Start-Sleep -Milliseconds 500
while ($proc.StandardOutput.Peek() -ge 0) {
    $extra = $proc.StandardOutput.ReadLine()
    if ($null -eq $extra) { break }
    Add-Content -Path $OutLog -Value $extra
    Write-Host $extra
}

if (-not $ready) {
    $err = $proc.StandardError.ReadToEnd()
    if ($err) { Add-Content -Path $OutLog -Value $err; Write-Host $err }
    if (-not $proc.HasExited) { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue }
    throw "READY_FOR_KILL not observed; see $OutLog"
}

Write-Host "== Stop-Process TM/client PID=$($proc.Id) (server untouched; Atomikos logs kept) =="
Stop-Process -Id $proc.Id -Force
Start-Sleep -Seconds 3

Write-Host "== Atomikos log dir after kill =="
Get-ChildItem $LogDir -Force | ForEach-Object { Write-Host ("  " + $_.Name); Add-Content -Path $OutLog -Value ("TM_LOG_FILE=" + $_.Name) }

Write-Host "== recover-resolve in new JVM =="
$probeOut = $OutLog.Replace(".log", "-recover.log")
$probeErr = $OutLog.Replace(".log", "-recover.err.log")
$probe = Start-Process -FilePath "java" -ArgumentList @("-cp", $Cp, $Main, "recover-resolve", "$LogDir") `
    -NoNewWindow -Wait -PassThru -RedirectStandardOutput $probeOut -RedirectStandardError $probeErr
Get-Content $probeOut | Tee-Object -FilePath $OutLog -Append
if (Test-Path $probeErr) {
    $errText = Get-Content $probeErr -Raw
    if ($errText) { Add-Content -Path $OutLog -Value $errText; Write-Host $errText }
}
Write-Host "recover-resolve exit=$($probe.ExitCode)"
Write-Host "Full log: $OutLog"
Write-Host "Look for STRONG_VERDICT=STRONG_PASS|STRONG_BLOCKED in the log."
