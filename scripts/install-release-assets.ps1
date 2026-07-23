<#
.SYNOPSIS
  Install GitHub Release ZIP assets into a Maven repository (default: local ~/.m2).

.DESCRIPTION
  Order matters:
    1) parent POM
    2) XuGu JDBC as 12.3.6 with generated POM (embedded driver POM may say 12.3.4)
    3) each module JAR + matching POM
    4) proxy-dialect-xugu aggregate POM

.PARAMETER ZipPath
  Path to shardingsphere-dialect-xugu-*-jars.zip (or an already-extracted directory).

.PARAMETER JdbcJar
  Path to xugu-jdbc-12.3.6.jar (required).

.PARAMETER MavenRepo
  Target Maven repository. Default: $HOME/.m2/repository
#>
[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$ZipPath,

    [Parameter(Mandatory = $true)]
    [string]$JdbcJar,

    [string]$Version = "5.5.3-xugu",

    [string]$MavenRepo = (Join-Path $HOME ".m2\repository"),

    [string]$MavenCmd = "mvn"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $ZipPath)) { throw "Zip/dir not found: $ZipPath" }
if (-not (Test-Path -LiteralPath $JdbcJar)) { throw "JDBC jar not found: $JdbcJar" }

$work = Join-Path ([System.IO.Path]::GetTempPath()) ("ss-xugu-release-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $work | Out-Null
try {
    if ((Get-Item -LiteralPath $ZipPath).PSIsContainer) {
        Copy-Item -Path (Join-Path $ZipPath "*") -Destination $work -Recurse -Force
    } else {
        Expand-Archive -LiteralPath $ZipPath -DestinationPath $work -Force
    }

    $parentPom = Join-Path $work "shardingsphere-dialect-xugu-parent-$Version.pom"
    if (-not (Test-Path -LiteralPath $parentPom)) {
        throw "Parent POM missing from assets: shardingsphere-dialect-xugu-parent-$Version.pom — re-run scripts/package-release-assets.ps1"
    }

    function Install-File {
        param([Parameter(Mandatory)][string[]]$Arguments)
        & $MavenCmd -q "-Dmaven.repo.local=$MavenRepo" `
            "org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file" @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Maven install-file failed: $($Arguments -join ' ')"
        }
    }

    Write-Host "[1/4] Installing parent POM..."
    Install-File @(
        "-Dfile=$parentPom",
        "-DgroupId=com.xugudb.shardingsphere",
        "-DartifactId=shardingsphere-dialect-xugu-parent",
        "-Dversion=$Version",
        "-Dpackaging=pom"
    )

    Write-Host "[2/4] Installing XuGu JDBC as 12.3.6 (explicit coords; ignore embedded 12.3.4)..."
    Install-File @(
        "-Dfile=$JdbcJar",
        "-DgroupId=com.xugudb",
        "-DartifactId=xugu-jdbc",
        "-Dversion=12.3.6",
        "-Dpackaging=jar",
        "-DgeneratePom=true"
    )

    Write-Host "[3/4] Installing module JARs + POMs..."
    Get-ChildItem -LiteralPath $work -Filter "*.jar" | Sort-Object Name | ForEach-Object {
        $base = [System.IO.Path]::GetFileNameWithoutExtension($_.Name)
        $pom = Join-Path $work "$base.pom"
        Write-Host "  $($_.Name)"
        if (Test-Path -LiteralPath $pom) {
            Install-File @("-Dfile=$($_.FullName)", "-DpomFile=$pom")
        } else {
            Install-File @("-Dfile=$($_.FullName)")
        }
    }

    $proxyPom = Join-Path $work "shardingsphere-proxy-dialect-xugu-$Version.pom"
    if (Test-Path -LiteralPath $proxyPom) {
        Write-Host "[4/4] Installing proxy-dialect aggregate POM..."
        Install-File @(
            "-Dfile=$proxyPom",
            "-DgroupId=com.xugudb.shardingsphere",
            "-DartifactId=shardingsphere-proxy-dialect-xugu",
            "-Dversion=$Version",
            "-Dpackaging=pom"
        )
    } else {
        Write-Host "[4/4] Skip: proxy-dialect POM not found"
    }

    Write-Host "Done. Repository: $MavenRepo"
}
finally {
    Remove-Item -LiteralPath $work -Recurse -Force -ErrorAction SilentlyContinue
}
