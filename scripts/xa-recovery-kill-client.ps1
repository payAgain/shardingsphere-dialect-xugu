# G-005 T2 / P1-1 — client JVM kill AFTER XA prepare (before commit).
# Prefer killing the client/TM JVM on the lab workstation. Do NOT kill XuGu server.
#
# Prerequisites:
#   - Lab reachable per tests-it/src/test/resources/it-xugu.properties
#   - Maven: C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd
#
# Usage (from repo root):
#   powershell -ExecutionPolicy Bypass -File .\scripts\xa-recovery-kill-client.ps1
#
# Evidence: PREPARED + READY_FOR_KILL phase=AFTER_PREPARE → Stop-Process →
# PROBE_COUNT / PROBE_RECOVER / disposition. Document honestly in xa-recovery-evidence.md.

$ErrorActionPreference = "Stop"
$RepoRoot = Split-Path -Parent $PSScriptRoot
$Mvn = "C:\Users\admin\tools\apache-maven-3.9.9\bin\mvn.cmd"
$HoldMs = 20000

Set-Location $RepoRoot

Write-Host "== compile tests-it (xa-recovery classpath) =="
& $Mvn -pl tests-it -am test-compile "-DskipTests" | Out-Host
if ($LASTEXITCODE -ne 0) { throw "mvn test-compile failed: $LASTEXITCODE" }

# Resolve runtime classpath via dependency:build-classpath + test-classes
$CpFile = Join-Path $env:TEMP "xugu-xa-recovery-cp.txt"
& $Mvn -pl tests-it -q dependency:build-classpath "-Dmdep.outputFile=$CpFile" "-DincludeScope=test" | Out-Host
if ($LASTEXITCODE -ne 0) { throw "dependency:build-classpath failed: $LASTEXITCODE" }

$TestClasses = Join-Path $RepoRoot "tests-it\target\test-classes"
$MainClasses = Join-Path $RepoRoot "tests-it\target\classes"
$Deps = Get-Content -Raw $CpFile
$Cp = "$TestClasses;$MainClasses;$Deps"

$Main = "com.xugudb.shardingsphere.it.xa.XARecoveryKillClientMain"
$OutLog = Join-Path $RepoRoot "tests-it\logs\xa-recovery-kill-client.log"
New-Item -ItemType Directory -Force -Path (Split-Path $OutLog) | Out-Null
if (Test-Path $OutLog) { Remove-Item $OutLog -Force }

Write-Host "== start kill-client main holdMs=$HoldMs =="
$psi = New-Object System.Diagnostics.ProcessStartInfo
$psi.FileName = "java"
$psi.Arguments = "-cp `"$Cp`" $Main $HoldMs"
$psi.UseShellExecute = $false
$psi.RedirectStandardOutput = $true
$psi.RedirectStandardError = $true
$psi.CreateNoWindow = $true
$proc = New-Object System.Diagnostics.Process
$proc.StartInfo = $psi
[void]$proc.Start()

$ready = $false
$deadline = (Get-Date).AddSeconds(60)
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

if (-not $ready) {
    if (-not $proc.HasExited) { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue }
    throw "READY_FOR_KILL not observed; see $OutLog"
}

Write-Host "== Stop-Process client PID=$($proc.Id) (TM/client kill; server untouched) =="
Stop-Process -Id $proc.Id -Force
Start-Sleep -Seconds 2

Write-Host "== probe residual rows / recover() =="
$probe = Start-Process -FilePath "java" -ArgumentList @("-cp", $Cp, $Main, "probe") `
    -NoNewWindow -Wait -PassThru -RedirectStandardOutput $OutLog.Replace(".log", "-probe.log") `
    -RedirectStandardError $OutLog.Replace(".log", "-probe.err.log")
Get-Content $OutLog.Replace(".log", "-probe.log") | Tee-Object -FilePath $OutLog -Append
Write-Host "probe exit=$($probe.ExitCode)"
Write-Host "Full log: $OutLog"
Write-Host "Document observed PROBE_* / disposition in docs/xa-recovery-evidence.md."
Write-Host "Honesty: IN_DOUBT_VIA_RECOVER => medium; CLEAN with count=0 after prepare-kill => medium (RM abort); Strong TM-log replay still NOT claimed."
