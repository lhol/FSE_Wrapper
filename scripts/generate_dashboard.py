#!/usr/bin/env python3
import json
import re
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "performance_dashboard.md"
HISTORY = ROOT / "performance_history.json"

def parse_native(path):
    """Parse native_bench.txt"""
    results = {}
    if not path.exists():
        print(f"Warning: {path} not found, skipping native benchmarks")
        return results
    
    try:
        with open(path, "r") as f:
            for line in f:
                m = re.match(r"(Huff0|FSE) compress:\s+([\d\.]+) MB/s", line)
                if m:
                    codec = m.group(1)
                    mbps = float(m.group(2))
                    results[f"{codec}_native"] = mbps
    except Exception as e:
        print(f"Error parsing native benchmarks: {e}")
    return results

def parse_jmh(path):
    """Parse JMH JSON results"""
    results = {}
    if not path.exists():
        print(f"Warning: {path} not found, skipping Java benchmarks")
        return results
    
    try:
        with open(path, "r") as f:
            data = json.load(f)

        test_data = "The quick brown fox jumps over the lazy dog".encode()
        for bench in data:
            name = bench["benchmark"].split(".")[-1]
            score = bench["primaryMetric"]["score"]  # ops/us (throughput)
            # Convert ops/us to MB/s: score * 1e6 us/s * len(data) bytes/op / (1024*1024) bytes/MB
            mbps = (score * 1e6 * len(test_data)) / (1024 * 1024)
            results[f"{name}_java"] = mbps
    except Exception as e:
        print(f"Error parsing JMH results: {e}")
    return results

def parse_dotnet_huff0(path):
    """Parse BenchmarkDotNet JSON for Huff0"""
    results = {}
    json_file = path / "results.json"
    if not json_file.exists():
        print(f"Warning: {json_file} not found, skipping Huff0 .NET benchmarks")
        return results
    
    try:
        with open(json_file, "r") as f:
            data = json.load(f)

        test_data = "The quick brown fox jumps over the lazy dog".encode()
        for bench in data.get("Benchmarks", []):
            # Extract method name from DisplayInfo or MethodTitle
            # DisplayInfo example: "Huff0Compress"
            name = bench.get("DisplayInfo") or bench.get("MethodTitle", "Unknown")
            # Mean is in nanoseconds
            ns_per_op = bench.get("Statistics", {}).get("Mean")
            if ns_per_op and ns_per_op > 0:
                # Convert ns to seconds, data per op to MB/s
                mbps = (1e9 / ns_per_op) * len(test_data) / (1024 * 1024)
                results[f"{name}_dotnet"] = mbps
    except Exception as e:
        print(f"Error parsing Huff0 .NET results: {e}")
    return results

def parse_dotnet_fse(path):
    """Parse BenchmarkDotNet JSON for FSE"""
    results = {}
    json_file = path / "results.json"
    if not json_file.exists():
        print(f"Warning: {json_file} not found, skipping Fse .NET benchmarks")
        return results
    
    try:
        with open(json_file, "r") as f:
            data = json.load(f)

        test_data = "The quick brown fox jumps over the lazy dog".encode()
        for bench in data.get("Benchmarks", []):
            # Extract method name from DisplayInfo or MethodTitle
            name = bench.get("DisplayInfo") or bench.get("MethodTitle", "Unknown")
            ns_per_op = bench.get("Statistics", {}).get("Mean")
            if ns_per_op and ns_per_op > 0:
                mbps = (1e9 / ns_per_op) * len(test_data) / (1024 * 1024)
                results[f"{name}_dotnet"] = mbps
    except Exception as e:
        print(f"Error parsing Fse .NET results: {e}")
    return results

def load_history():
    if HISTORY.exists():
        return json.loads(HISTORY.read_text())
    return []

def save_history(history):
    HISTORY.write_text(json.dumps(history, indent=2))

def generate_markdown(timestamp, results, history):
    lines = []
    lines.append("# 📊 Huff0 & FSE Performance Dashboard\n")
    lines.append(f"**Last updated:** {timestamp}\n")

    # Current results table
    lines.append("## Current Benchmark Results\n")
    lines.append("| Codec | Native MB/s | Java MB/s | .NET MB/s |")
    lines.append("|-------|--------------|-----------|-----------|")

    def get(name):
        return f"{results.get(name, '—'):.2f}" if name in results else "—"

    lines.append(f"| Huff0 | {get('Huff0_native')} | {get('huff0Compress_java')} | {get('Huff0Compress_dotnet')} |")
    lines.append(f"| FSE   | {get('FSE_native')}   | {get('fseCompress_java')}   | {get('FseCompress_dotnet')}   |")

    # History table
    lines.append("\n## Historical Trends\n")
    lines.append("| Date | Huff0 Native | Huff0 Java | Huff0 .NET | FSE Native | FSE Java | FSE .NET |")
    lines.append("|------|--------------|-------------|-------------|-------------|-----------|-----------|")

    for entry in history[-20:]:  # last 20 runs
        lines.append(
            f"| {entry['timestamp']} | "
            f"{entry.get('Huff0_native','—')} | "
            f"{entry.get('huff0Compress_java','—')} | "
            f"{entry.get('Huff0Compress_dotnet','—')} | "
            f"{entry.get('FSE_native','—')} | "
            f"{entry.get('fseCompress_java','—')} | "
            f"{entry.get('FseCompress_dotnet','—')} |"
        )

    OUT.write_text("\n".join(lines))

def main():
    timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")

    results = {}
    
    # Parse each source and report what was found
    native_results = parse_native(ROOT / "native_bench.txt")
    if native_results:
        print(f"✓ Native benchmarks: {list(native_results.keys())}")
    results.update(native_results)
    
    jmh_results = parse_jmh(ROOT / "jmh_results.json")
    if jmh_results:
        print(f"✓ Java benchmarks: {list(jmh_results.keys())}")
    else:
        print("⚠ No Java benchmarks found")
    results.update(jmh_results)
    
    huff0_results = parse_dotnet_huff0(ROOT / "csharp/Huff0.net/Benchmarks/artifacts")
    if huff0_results:
        print(f"✓ Huff0 .NET benchmarks: {list(huff0_results.keys())}")
    else:
        print("⚠ No Huff0 .NET benchmarks found")
    results.update(huff0_results)
    
    fse_results = parse_dotnet_fse(ROOT / "csharp/Fse.net/Benchmarks/artifacts")
    if fse_results:
        print(f"✓ Fse .NET benchmarks: {list(fse_results.keys())}")
    else:
        print("⚠ No Fse .NET benchmarks found")
    results.update(fse_results)

    if not results:
        print("ERROR: No benchmark results were collected!")
        return

    history = load_history()
    history.append({"timestamp": timestamp, **results})
    save_history(history)

    generate_markdown(timestamp, results, history)

    print(f"✓ Dashboard generated: {OUT}")

if __name__ == "__main__":
    main()
