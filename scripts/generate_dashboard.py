#!/usr/bin/env python3
"""
generate_dashboard.py — Collects benchmark results from native, JMH, and BenchmarkDotNet
JSON outputs and generates a Markdown performance dashboard with per-size tables.

Expected inputs (all relative to repo root):
  native_bench.txt                            — native bench_codecs output
  jmh_results.json                            — JMH -rf json output
  csharp/Huff0.net/Benchmarks/artifacts/      — BenchmarkDotNet artifacts dir
  csharp/Fse.net/Benchmarks/artifacts/        — BenchmarkDotNet artifacts dir
"""

import json
import re
import glob
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "performance_dashboard.md"
HISTORY = ROOT / "performance_history.json"

SIZES = [512, 1024, 4096, 16384, 65536, 262144, 1048576, 4194304, 16777216]
SIZE_LABELS = {
    512: "512B", 1024: "1KB", 4096: "4KB", 16384: "16KB",
    65536: "64KB", 262144: "256KB", 1048576: "1MB",
    4194304: "4MB", 16777216: "16MB"
}
DATA_TYPES = ["LOW_ENTROPY", "MEDIUM_ENTROPY"]


def _mbps(ops_per_sec, size_bytes):
    """Convert throughput (ops/s) to MB/s for a given data size."""
    return ops_per_sec * size_bytes / (1024 * 1024)


def parse_native(path):
    """
    Parse native bench_codecs output.
    Expected line format: 'Huff0 compress [4KB, LOW_ENTROPY]: 1234.56 MB/s'
    Also accepts legacy single-value format: 'Huff0 compress: 1234.56 MB/s'

    Returns dict: {codec: {size: {datatype: mbps}}}
    """
    results = {}
    if not path.exists():
        print(f"Warning: {path} not found, skipping native benchmarks")
        return results
    try:
        size_map = {v: k for k, v in SIZE_LABELS.items()}
        with open(path) as f:
            for line in f:
                # Multi-size format
                m = re.match(
                    r"(Huff0|FSE) compress \[([^,]+),\s*(LOW_ENTROPY|MEDIUM_ENTROPY)\]:\s*([\d.]+) MB/s",
                    line)
                if m:
                    codec, size_str, dt, mbps_str = m.groups()
                    size = size_map.get(size_str.strip())
                    if size is not None:
                        results.setdefault(codec, {}).setdefault(size, {})[dt] = float(mbps_str)
                    continue
                # Legacy single-value format
                m2 = re.match(r"(Huff0|FSE) compress:\s+([\d.]+) MB/s", line)
                if m2:
                    codec, mbps_str = m2.groups()
                    # Store under a generic size=0 for backwards compat
                    results.setdefault(codec, {}).setdefault(0, {})["LEGACY"] = float(mbps_str)
    except Exception as e:
        print(f"Error parsing native benchmarks: {e}")
    return results


def parse_jmh(path):
    """
    Parse JMH JSON results (from -rf json -rff <path>).
    Handles both parameterized (size/dataType params) and legacy single-value benchmarks.

    Returns dict: {method: {size: {datatype: mbps}}}
    """
    results = {}
    if not path.exists():
        print(f"Warning: {path} not found, skipping Java benchmarks")
        return results
    try:
        with open(path) as f:
            data = json.load(f)
        for bench in data:
            method = bench["benchmark"].split(".")[-1]
            score = bench["primaryMetric"]["score"]   # ops/s (Throughput/SECONDS)
            params = bench.get("params") or {}
            size = int(params.get("size", 0)) if params.get("size") else 0
            dt = params.get("dataType", "LEGACY")
            mbps = _mbps(score, size) if size else score
            results.setdefault(method, {}).setdefault(size, {})[dt] = mbps
    except Exception as e:
        print(f"Error parsing JMH results: {e}")
    return results


def parse_bdn(artifacts_dir):
    """
    Parse BenchmarkDotNet JSON results from artifacts/results/*.json.
    BenchmarkDotNet exports to <artifacts>/results/<ClassName>-report.json.

    Returns dict: {method: {size: {datatype: mbps}}}
    """
    results = {}
    results_dir = Path(artifacts_dir) / "results"
    if not results_dir.exists():
        print(f"Warning: {results_dir} not found, skipping .NET benchmarks")
        return results
    json_files = list(results_dir.glob("*.json"))
    if not json_files:
        print(f"Warning: no JSON files in {results_dir}")
        return results
    try:
        for jf in json_files:
            with open(jf) as f:
                data = json.load(f)
            for bench in data.get("Benchmarks", []):
                method = bench.get("Method") or bench.get("MethodTitle", "Unknown")
                # Parameters string: "DataType=LOW_ENTROPY, Size=512"
                params_str = bench.get("Parameters", "")
                size, dt = 0, "LEGACY"
                for part in params_str.split(","):
                    part = part.strip()
                    if part.startswith("Size="):
                        size = int(part.split("=")[1])
                    elif part.startswith("DataType="):
                        dt = part.split("=")[1]
                ns_mean = bench.get("Statistics", {}).get("Mean")
                if ns_mean and ns_mean > 0:
                    # Mean in nanoseconds → ops/s → MB/s
                    ops_per_sec = 1e9 / ns_mean
                    mbps = _mbps(ops_per_sec, size) if size else ops_per_sec
                    results.setdefault(method, {}).setdefault(size, {})[dt] = mbps
    except Exception as e:
        print(f"Error parsing BenchmarkDotNet results: {e}")
    return results


def load_history():
    if HISTORY.exists():
        try:
            return json.loads(HISTORY.read_text())
        except Exception:
            return []
    return []


def save_history(history):
    HISTORY.write_text(json.dumps(history, indent=2))


def _fmt(val):
    if val is None:
        return "—"
    if val >= 1000:
        return f"{val/1000:.1f} GB/s"
    return f"{val:.1f} MB/s"


def generate_markdown(timestamp, native, jmh, huff0_bdn, fse_bdn):
    lines = [
        "# 📊 Huff0 & FSE Performance Dashboard\n",
        f"**Last updated:** {timestamp}\n",
        "> All throughput values are in **MB/s** (higher = faster).\n",
    ]

    def bench_table(codec, native_data, jni_data, panama_data, bdn_data, dt):
        """Generate a table for one codec + data type across all sizes."""
        label_col = f"{'Size':<8}"
        header = f"| {label_col} | Native | Java JNI | Java Panama | .NET |"
        sep    = f"|{'-'*10}|{'-'*10}|{'-'*12}|{'-'*15}|{'-'*10}|"
        rows = [header, sep]
        for sz in SIZES:
            sl = SIZE_LABELS[sz]
            nat   = _fmt(native_data.get(sz, {}).get(dt))
            jni   = _fmt(jni_data.get(sz, {}).get(dt))
            pan   = _fmt(panama_data.get(sz, {}).get(dt))
            net   = _fmt(bdn_data.get(sz, {}).get(dt))
            rows.append(f"| {sl:<8} | {nat:<8} | {jni:<10} | {pan:<13} | {net:<8} |")
        return "\n".join(rows)

    huff0_native = native.get("Huff0", {})
    fse_native   = native.get("FSE", {})
    huff0_jni    = jmh.get("huff0CompressJNI", {})
    huff0_pan    = jmh.get("huff0CompressPanama", {})
    fse_jni      = jmh.get("fseCompressJNI", {})
    fse_pan      = jmh.get("fseCompressPanama", {})
    huff0_net    = huff0_bdn.get("Huff0Compress", {})
    fse_net      = fse_bdn.get("FseCompress", {})

    for dt in DATA_TYPES:
        label = "Low Entropy (repeating pattern)" if dt == "LOW_ENTROPY" else "Medium Entropy (text)"
        lines.append(f"\n## {label}\n")
        lines.append("### Huff0\n")
        lines.append(bench_table("Huff0", huff0_native, huff0_jni, huff0_pan, huff0_net, dt))
        lines.append("\n\n### FSE\n")
        lines.append(bench_table("FSE", fse_native, fse_jni, fse_pan, fse_net, dt))

    OUT.write_text("\n".join(lines) + "\n")


def flatten(native, jmh, huff0_bdn, fse_bdn):
    """Flatten nested dicts to a single dict for history tracking."""
    flat = {}
    for codec, sizes in native.items():
        for sz, dts in sizes.items():
            for dt, mbps in dts.items():
                flat[f"native_{codec}_{sz}_{dt}"] = mbps
    for method, sizes in jmh.items():
        for sz, dts in sizes.items():
            for dt, mbps in dts.items():
                flat[f"jmh_{method}_{sz}_{dt}"] = mbps
    for method, sizes in huff0_bdn.items():
        for sz, dts in sizes.items():
            for dt, mbps in dts.items():
                flat[f"huff0net_{method}_{sz}_{dt}"] = mbps
    for method, sizes in fse_bdn.items():
        for sz, dts in sizes.items():
            for dt, mbps in dts.items():
                flat[f"fsenet_{method}_{sz}_{dt}"] = mbps
    return flat


def main():
    timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")

    native = parse_native(ROOT / "native_bench.txt")
    jmh    = parse_jmh(ROOT / "jmh_results.json")
    huff0_bdn = parse_bdn(ROOT / "csharp/Huff0.net/Benchmarks/artifacts")
    fse_bdn   = parse_bdn(ROOT / "csharp/Fse.net/Benchmarks/artifacts")

    # Diagnostic output
    print(f"Native codecs found: {list(native.keys())}")
    print(f"JMH methods found:   {list(jmh.keys())}")
    print(f"Huff0 .NET methods:  {list(huff0_bdn.keys())}")
    print(f"Fse   .NET methods:  {list(fse_bdn.keys())}")

    if not any([native, jmh, huff0_bdn, fse_bdn]):
        print("ERROR: No benchmark results collected!")
        return

    flat = flatten(native, jmh, huff0_bdn, fse_bdn)
    history = load_history()
    history.append({"timestamp": timestamp, **flat})
    save_history(history)

    generate_markdown(timestamp, native, jmh, huff0_bdn, fse_bdn)
    print(f"✓ Dashboard written to {OUT}")


if __name__ == "__main__":
    main()

