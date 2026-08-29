#!/usr/bin/env bash
# bench_local.sh — Run all benchmarks locally and produce performance_dashboard.md
#
# Usage:
#   bash scripts/bench_local.sh [--skip-dotnet] [--skip-java] [--skip-native]
#
# After running, commit and upload results:
#   git add performance_dashboard.md native_bench.txt jmh_results.json
#   git commit -m "chore: local benchmark results for vX.Y.Z"
#   gh release upload vX.Y.Z performance_dashboard.md native_bench.txt jmh_results.json

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

SKIP_DOTNET=false
SKIP_JAVA=false
SKIP_NATIVE=false

for arg in "$@"; do
  case "$arg" in
    --skip-dotnet) SKIP_DOTNET=true ;;
    --skip-java)   SKIP_JAVA=true   ;;
    --skip-native) SKIP_NATIVE=true ;;
    *) echo "Unknown argument: $arg"; exit 1 ;;
  esac
done

echo "=========================================="
echo "FSE_Wrapper local benchmark runner"
echo "=========================================="

# --- Native ---
if [ "$SKIP_NATIVE" = false ]; then
  echo ""
  echo ">>> Building native libraries..."
  cmake -B native/build -S native
  cmake --build native/build --config Release

  echo ">>> Running native benchmarks..."
  chmod +x native/build/bench_codecs 2>/dev/null || true
  cd native/build
  ./bench_codecs | tee ../../native_bench.txt
  cd "$REPO_ROOT"
  echo "    → native_bench.txt written"
else
  echo ">>> Skipping native benchmarks"
fi

# --- Java JMH ---
if [ "$SKIP_JAVA" = false ]; then
  echo ""
  echo ">>> Building Java benchmarks JAR (-P bench)..."
  ./mvnw -B package -DskipTests -P bench -q

  JAR=$(ls target/FSE_Wrapper-*-benchmarks.jar | head -1)
  echo ">>> Running JMH benchmarks (JAR: $JAR)..."
  echo "    Warmup: 3 iterations × 1s | Measurement: 5 iterations × 1s"
  java -jar "$JAR" -rf json -rff jmh_results.json -wi 3 -i 5
  echo "    → jmh_results.json written"
else
  echo ">>> Skipping Java JMH benchmarks"
fi

# --- .NET BenchmarkDotNet ---
if [ "$SKIP_DOTNET" = false ]; then
  echo ""
  echo ">>> Running .NET Huff0 benchmarks..."
  export LD_LIBRARY_PATH="${REPO_ROOT}/native/build${LD_LIBRARY_PATH:+:$LD_LIBRARY_PATH}"
  export DYLD_LIBRARY_PATH="${REPO_ROOT}/native/build${DYLD_LIBRARY_PATH:+:$DYLD_LIBRARY_PATH}"
  dotnet run -c Release --project csharp/Huff0.net/Benchmarks/Benchmarks.csproj \
    -- --job short --exporters json --artifacts csharp/Huff0.net/Benchmarks/artifacts

  echo ">>> Running .NET Fse benchmarks..."
  dotnet run -c Release --project csharp/Fse.net/Benchmarks/Benchmarks.csproj \
    -- --job short --exporters json --artifacts csharp/Fse.net/Benchmarks/artifacts
else
  echo ">>> Skipping .NET benchmarks"
fi

# --- Dashboard ---
echo ""
echo ">>> Generating performance dashboard..."
python3 scripts/generate_dashboard.py
echo "    → performance_dashboard.md written"

echo ""
echo "=========================================="
echo "Done! To attach results to a release:"
echo ""
echo "  git add performance_dashboard.md native_bench.txt jmh_results.json"
echo "  git commit -m \"chore: local benchmark results for \$(git describe --tags --abbrev=0)\""
echo "  gh release upload \$(git describe --tags --abbrev=0) \\"
echo "    performance_dashboard.md native_bench.txt jmh_results.json"
echo "=========================================="
