param([string]$Version = "5.5.3-xugu")
$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $root
$dist = Join-Path $root "dist"
New-Item -ItemType Directory -Force -Path $dist | Out-Null
Get-ChildItem $dist -Force | Remove-Item -Recurse -Force

# Parent POM is required: every module POM inherits shardingsphere-dialect-xugu-parent.
Copy-Item (Join-Path $root "pom.xml") (Join-Path $dist "shardingsphere-dialect-xugu-parent-$Version.pom") -Force

$modules = @(
  @{ Dir = "connector-xugu"; Artifact = "shardingsphere-database-connector-xugu" },
  @{ Dir = "database-exception-xugu"; Artifact = "shardingsphere-database-exception-xugu" },
  @{ Dir = "infra-binder-xugu"; Artifact = "shardingsphere-infra-binder-xugu" },
  @{ Dir = "infra-rewrite-xugu"; Artifact = "shardingsphere-infra-rewrite-xugu" },
  @{ Dir = "infra-route-xugu"; Artifact = "shardingsphere-infra-route-xugu" },
  @{ Dir = "jdbc-dialect-xugu"; Artifact = "shardingsphere-jdbc-dialect-xugu" },
  @{ Dir = "parser-sql-engine-xugu"; Artifact = "shardingsphere-parser-sql-engine-xugu" },
  @{ Dir = "parser-sql-statement-xugu"; Artifact = "shardingsphere-parser-sql-statement-xugu" },
  @{ Dir = "proxy-backend-xugu"; Artifact = "shardingsphere-proxy-backend-xugu" },
  @{ Dir = "sharding-dialect-xugu"; Artifact = "shardingsphere-sharding-dialect-xugu" },
  @{ Dir = "sql-federation-xugu"; Artifact = "shardingsphere-sql-federation-xugu" },
  @{ Dir = "transaction-xugu"; Artifact = "shardingsphere-transaction-xugu" }
)

foreach ($m in $modules) {
  $jarRel = Join-Path $m.Dir "target/$($m.Artifact)-$Version.jar"
  $jarSrc = Join-Path $root $jarRel
  if (-not (Test-Path $jarSrc)) { throw "Missing artifact: $jarSrc" }
  Copy-Item $jarSrc $dist -Force

  $pomSrc = Join-Path $root (Join-Path $m.Dir "pom.xml")
  if (-not (Test-Path $pomSrc)) { throw "Missing module POM: $pomSrc" }
  Copy-Item $pomSrc (Join-Path $dist "$($m.Artifact)-$Version.pom") -Force
}

Copy-Item (Join-Path $root "proxy-dialect-xugu/pom.xml") (Join-Path $dist "shardingsphere-proxy-dialect-xugu-$Version.pom") -Force

# GitHub Release description (tracked source under docs/; dist/ is gitignored)
$releaseBody = Join-Path $root "docs/github-release-body.md"
if (Test-Path $releaseBody) {
  Copy-Item $releaseBody (Join-Path $dist "RELEASE-BODY.md") -Force
}

$zip = Join-Path $dist "shardingsphere-dialect-xugu-$Version-jars.zip"
$files = @(Get-ChildItem $dist -File | Where-Object { $_.Extension -ne ".zip" })
if (Test-Path $zip) { Remove-Item $zip -Force }
Compress-Archive -Path $files.FullName -DestinationPath $zip -Force
Get-ChildItem $dist | Sort-Object Name | Format-Table Name, Length
Write-Host "Packaged $($files.Count) files into $zip (includes parent POM + module POMs)."
