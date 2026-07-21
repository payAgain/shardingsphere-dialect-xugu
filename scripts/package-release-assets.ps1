param([string]$Version = "5.5.3-xugu")
$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $root
$dist = Join-Path $root "dist"
New-Item -ItemType Directory -Force -Path $dist | Out-Null
Get-ChildItem $dist -Force | Remove-Item -Recurse -Force
$modules = @(
  "connector-xugu/target/shardingsphere-database-connector-xugu-$Version.jar",
  "database-exception-xugu/target/shardingsphere-database-exception-xugu-$Version.jar",
  "infra-binder-xugu/target/shardingsphere-infra-binder-xugu-$Version.jar",
  "infra-rewrite-xugu/target/shardingsphere-infra-rewrite-xugu-$Version.jar",
  "infra-route-xugu/target/shardingsphere-infra-route-xugu-$Version.jar",
  "jdbc-dialect-xugu/target/shardingsphere-jdbc-dialect-xugu-$Version.jar",
  "parser-sql-engine-xugu/target/shardingsphere-parser-sql-engine-xugu-$Version.jar",
  "parser-sql-statement-xugu/target/shardingsphere-parser-sql-statement-xugu-$Version.jar",
  "proxy-backend-xugu/target/shardingsphere-proxy-backend-xugu-$Version.jar",
  "sharding-dialect-xugu/target/shardingsphere-sharding-dialect-xugu-$Version.jar",
  "sql-federation-xugu/target/shardingsphere-sql-federation-xugu-$Version.jar",
  "transaction-xugu/target/shardingsphere-transaction-xugu-$Version.jar"
)
foreach ($rel in $modules) {
  $src = Join-Path $root $rel
  if (-not (Test-Path $src)) { throw "Missing artifact: $src" }
  Copy-Item $src $dist -Force
}
Copy-Item (Join-Path $root "proxy-dialect-xugu/pom.xml") (Join-Path $dist "shardingsphere-proxy-dialect-xugu-$Version.pom") -Force
$zip = Join-Path $dist "shardingsphere-dialect-xugu-$Version-jars.zip"
$files = @(Get-ChildItem $dist -File)
Compress-Archive -Path $files.FullName -DestinationPath $zip -Force
Get-ChildItem $dist | Format-Table Name, Length
