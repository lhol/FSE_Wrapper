#!/usr/bin/env pwsh
# build.ps1 — Full local build and test for all components.
# Usage: ./build.ps1 [-SkipNative] [-SkipJava] [-SkipDotnet] [-SkipTests]
param(
    [switch]$SkipNative,
    [switch]$SkipJava,
    [switch]$SkipDotnet,
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

function Step([string]$msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Ok([string]$msg)   { Write-Host "    $msg" -ForegroundColor Green }
function Fail([string]$msg) { Write-Host "    ERROR: $msg" -ForegroundColor Red; exit 1 }

# Ensure JAVA_HOME is set (needed for both CMake JNI headers and Maven)
if (-not $env:JAVA_HOME) {
    $jdk = Get-ChildItem "$env:USERPROFILE\.jdks" -Directory -ErrorAction SilentlyContinue |
           Sort-Object Name -Descending | Select-Object -First 1
    if ($jdk) {
        $env:JAVA_HOME = $jdk.FullName
        $env:PATH = "$($jdk.FullName)\bin;$env:PATH"
        Write-Host "  Using JDK: $($env:JAVA_HOME)" -ForegroundColor DarkGray
    } else {
        Fail "JAVA_HOME is not set and no JDK found in ~/.jdks"
    }
}

# ---------------------------------------------------------------------------
# Native (CMake)
# ---------------------------------------------------------------------------
if (-not $SkipNative) {
    Step "Building native libraries"
    cmake -B "$root/native/build" -S "$root/native"
    if ($LASTEXITCODE -ne 0) { Fail "cmake configure failed" }

    cmake --build "$root/native/build" --config Release
    if ($LASTEXITCODE -ne 0) { Fail "cmake build failed" }
    Ok "Native build complete"

    if (-not $SkipTests) {
        Step "Running native tests"
        Push-Location "$root/native/build"
        ctest --output-on-failure -C Release
        if ($LASTEXITCODE -ne 0) { Pop-Location; Fail "Native tests failed" }
        Pop-Location
        Ok "Native tests passed"
    }
}

# ---------------------------------------------------------------------------
# Java (Maven)
# ---------------------------------------------------------------------------
if (-not $SkipJava) {
    Step "Building Java"
    $mvnw = if ($IsWindows -or $env:OS -eq 'Windows_NT') { "$root/mvnw.cmd" } else { "$root/mvnw" }

    if ($SkipTests) {
        & $mvnw -B clean package -DskipTests
    } else {
        Step "Building and testing Java"
        & $mvnw -B clean test
    }
    if ($LASTEXITCODE -ne 0) { Fail "Maven build/test failed" }
    Ok "Java build complete"
}

# ---------------------------------------------------------------------------
# .NET
# ---------------------------------------------------------------------------
if (-not $SkipDotnet) {
    Step "Building .NET libraries"
    dotnet build "$root/csharp/Huff0.net/src/Huff0.Net.csproj" -c Release
    if ($LASTEXITCODE -ne 0) { Fail "Huff0.Net build failed" }

    dotnet build "$root/csharp/Fse.net/src/Fse.Net.csproj" -c Release
    if ($LASTEXITCODE -ne 0) { Fail "Fse.Net build failed" }
    Ok ".NET build complete"
}

Write-Host "`nAll builds completed successfully." -ForegroundColor Green
