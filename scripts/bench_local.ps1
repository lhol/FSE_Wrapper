#Requires -Version 5.1
<#
.SYNOPSIS
    Run all benchmarks locally and produce performance_dashboard.md

.DESCRIPTION
    Builds and runs native, Java JMH, and .NET BenchmarkDotNet benchmarks,
    then generates performance_dashboard.md via generate_dashboard.py.

.PARAMETER SkipDotNet
    Skip .NET BenchmarkDotNet benchmarks.

.PARAMETER SkipJava
    Skip Java JMH benchmarks.

.PARAMETER SkipNative
    Skip native bench_codecs benchmarks.

.EXAMPLE
    .\scripts\bench_local.ps1
    .\scripts\bench_local.ps1 -SkipDotNet
    .\scripts\bench_local.ps1 -SkipNative -SkipDotNet

.NOTES
    After running, commit and upload results:
        git add performance_dashboard.md native_bench.txt jmh_results.json
        git commit -m "chore: local benchmark results for vX.Y.Z"
        gh release upload vX.Y.Z performance_dashboard.md native_bench.txt jmh_results.json
#>
param(
    [switch]$SkipDotNet,
    [switch]$SkipJava,
    [switch]$SkipNative
)

$ErrorActionPreference = 'Stop'

$RepoRoot = Split-Path -Parent $PSScriptRoot
Push-Location $RepoRoot

try {
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "FSE_Wrapper local benchmark runner"        -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan

    # --- Native ---
    if (-not $SkipNative) {
        Write-Host "`n>>> Building native libraries..." -ForegroundColor Yellow
        cmake -B native/build -S native
        if ($LASTEXITCODE -ne 0) { throw "cmake configure failed" }
        cmake --build native/build --config Release
        if ($LASTEXITCODE -ne 0) { throw "cmake build failed" }

        Write-Host ">>> Running native benchmarks..." -ForegroundColor Yellow
        $benchExe = "native\build\Release\bench_codecs.exe"
        if (-not (Test-Path $benchExe)) { $benchExe = "native\build\bench_codecs.exe" }
        & $benchExe | Tee-Object -FilePath native_bench.txt
        if ($LASTEXITCODE -ne 0) { throw "bench_codecs failed" }
        Write-Host "    -> native_bench.txt written"
    } else {
        Write-Host ">>> Skipping native benchmarks" -ForegroundColor DarkGray
    }

    # --- Java JMH ---
    if (-not $SkipJava) {
        Write-Host "`n>>> Building Java benchmarks JAR (-P bench)..." -ForegroundColor Yellow
        & .\mvnw.cmd -B package -DskipTests -P bench -q
        if ($LASTEXITCODE -ne 0) { throw "Maven bench build failed" }

        $jar = Get-ChildItem target\FSE_Wrapper-*-benchmarks.jar | Select-Object -First 1
        if (-not $jar) { throw "Benchmarks JAR not found in target/" }
        Write-Host ">>> Running JMH benchmarks (JAR: $($jar.Name))..." -ForegroundColor Yellow
        Write-Host "    Warmup: 3 iterations x 1s | Measurement: 5 iterations x 1s"
        java -jar $jar.FullName -rf json -rff jmh_results.json -wi 3 -i 5
        if ($LASTEXITCODE -ne 0) { throw "JMH benchmark run failed" }
        Write-Host "    -> jmh_results.json written"
    } else {
        Write-Host ">>> Skipping Java JMH benchmarks" -ForegroundColor DarkGray
    }

    # --- .NET BenchmarkDotNet ---
    if (-not $SkipDotNet) {
        $env:PATH = "native\build\Release;native\build;$env:PATH"

        Write-Host "`n>>> Running .NET Huff0 benchmarks..." -ForegroundColor Yellow
        dotnet run -c Release --project csharp\Huff0.net\Benchmarks\Benchmarks.csproj `
            -- --job short --exporters json --artifacts csharp\Huff0.net\Benchmarks\artifacts
        if ($LASTEXITCODE -ne 0) { throw ".NET Huff0 benchmarks failed" }

        Write-Host ">>> Running .NET Fse benchmarks..." -ForegroundColor Yellow
        dotnet run -c Release --project csharp\Fse.net\Benchmarks\Benchmarks.csproj `
            -- --job short --exporters json --artifacts csharp\Fse.net\Benchmarks\artifacts
        if ($LASTEXITCODE -ne 0) { throw ".NET Fse benchmarks failed" }
    } else {
        Write-Host ">>> Skipping .NET benchmarks" -ForegroundColor DarkGray
    }

    # --- Dashboard ---
    Write-Host "`n>>> Generating performance dashboard..." -ForegroundColor Yellow
    python3 scripts/generate_dashboard.py
    if ($LASTEXITCODE -ne 0) { throw "Dashboard generation failed" }
    Write-Host "    -> performance_dashboard.md written"

    $tag = (git describe --tags --abbrev=0 2>$null) -replace '^$','vX.Y.Z'
    Write-Host "`n==========================================" -ForegroundColor Cyan
    Write-Host "Done! To attach results to a release:"     -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  git add performance_dashboard.md native_bench.txt jmh_results.json"
    Write-Host "  git commit -m `"chore: local benchmark results for $tag`""
    Write-Host "  gh release upload $tag ``"
    Write-Host "    performance_dashboard.md native_bench.txt jmh_results.json"
    Write-Host "==========================================" -ForegroundColor Cyan

} finally {
    Pop-Location
}
