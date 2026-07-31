# FSE_Wrapper Agent Notes

## Purpose
FSE_Wrapper is a cross-platform wrapper around Cyan4973's FiniteStateEntropy C library, exposing:

- **Huff0** to Java and .NET
- **FSE** to Java and .NET

The repository builds native shared libraries (`.dll`, `.so`, `.dylib`) and thin managed bindings:

- **JNI** for Java (Java 11+ compatible)
- **Panama FFI** for Java (Java 21+ only, in `java-panama/`)
- **P/Invoke** for .NET

## Main Components
1. **native/**  
   CMake-based native wrappers over the FiniteStateEntropy submodule. Produces the shared libraries consumed by Java and .NET.

2. **java/**  
   Java APIs for Huff0 and FSE using JNI. Bundles native libraries into the JAR.

3. **java-panama/**  
   Alternative Java 21+ API using the Panama Foreign Function & Memory API. Same native libraries, no JNI glue required. Requires `--enable-native-access=ALL-UNNAMED` JVM flag.

4. **csharp/**  
   .NET libraries and benchmarks. Uses P/Invoke and bundles native libraries in NuGet packages under `runtimes/{rid}/native/`.

## Repository Structure
```text
FSE_Wrapper/
├── native/
│   ├── FiniteStateEntropy/      # git submodule: Cyan4973/FiniteStateEntropy (BSD-2-Clause)
│   ├── huff0_jni.c              # Huff0 JNI + P/Invoke wrapper
│   ├── fse_jni.c                # FSE JNI + P/Invoke wrapper
│   ├── tests/test_codecs.c      # native unit tests run via ctest
│   ├── bench/bench_codecs.c     # multi-size/entropy microbenchmarks
│   └── CMakeLists.txt           # native build config; requires JNI headers
│
├── java/
│   └── src/
│       ├── main/java/org/karenta/huff0/Huff0.java       # JNI binding
│       ├── main/java/org/karenta/fse/Fse.java            # JNI binding
│       ├── test/java/org/karenta/*/CodecTests.java       # JUnit roundtrip tests
│       ├── test/java/org/karenta/*/PanamaTests.java      # Panama roundtrip tests
│       └── jmh/java/org/karenta/*/CodeBench.java        # JMH benchmarks (JNI + Panama)
│
├── java-panama/
│   └── src/main/java/
│       ├── org/karenta/huff0/Huff0Panama.java            # Panama FFI binding
│       └── org/karenta/fse/FsePanama.java                # Panama FFI binding
│
├── csharp/
│   ├── Huff0.net/
│   │   ├── src/                 # netstandard2.0 library project
│   │   ├── Benchmarks/          # net8.0 BenchmarkDotNet executable (multi-size, BenchmarkSwitcher)
│   │   └── runtimes/            # native libs packed into NuGet package
│   │       ├── win-x64/native/
│   │       ├── win-arm64/native/
│   │       ├── linux-x64/native/
│   │       ├── linux-arm64/native/
│   │       └── osx-arm64/native/
│   └── Fse.net/                 # identical structure for FSE
│
├── scripts/generate_dashboard.py   # aggregates benchmark results into Markdown
├── specs/                          # version spec and requirement files
├── pom.xml                         # Maven root; version=0.0.15; compiler=Java 21
├── mvnw / mvnw.cmd                 # Maven wrapper
├── .mvn/wrapper/                   # maven-wrapper.jar + properties
├── Dockerfile                      # multi-stage build for all three components
├── build.ps1                       # orchestrated local build/test script (PowerShell)
└── .github/workflows/
    ├── rc.yaml                     # triggered by RC-vX.Y.Z → build + bench verify
    └── release.yaml                # triggered by vX.Y.Z → GitHub Release + NuGet publish + bench
```

## Local Build
### Preferred — use build.ps1
```powershell
./build.ps1
```

Flags:
```powershell
./build.ps1 -SkipNative
./build.ps1 -SkipJava
./build.ps1 -SkipDotnet
./build.ps1 -SkipTests
```

`build.ps1` auto-detects `JAVA_HOME` from `~/.jdks` if not already set.

### Manual steps

#### Native (CMake)
```bash
cmake -S native -B native/build
cmake --build native/build --config Release
ctest --test-dir native/build -C Release --output-on-failure
```

#### Java (Maven)
```bash
./mvnw test
# or just build without tests:
./mvnw -B package -DskipTests
```
Note: Compiles at Java 21. Sources include `java/src/main/java`, `java/src/jmh/java`, and `java-panama/src/main/java`.

#### .NET
```bash
dotnet build csharp/Huff0.net/src/Huff0.Net.csproj -c Release
dotnet build csharp/Fse.net/src/Fse.Net.csproj -c Release
```

## Architecture Notes

### JNI (Java)
- `System.loadLibrary("huff0")` / `"fse"` load the native libs at class-init time.
- Maven `copy-resources` copies `.so`/`.dylib`/`.dll` from `native/build/` into `target/classes/natives/`.
- Maven Surefire sets `-Djava.library.path=native/build:native/build/Release` and `--enable-native-access=ALL-UNNAMED` for tests.

### Panama FFI (Java 21+)
- `java-panama/src/main/java/` is included in the Maven source set via `build-helper-maven-plugin`.
- Uses `java.lang.foreign.Linker`, `Arena`, `MemorySegment` — no C glue code needed.
- Requires `--enable-native-access=ALL-UNNAMED` JVM flag at runtime.
- JMH benchmarks use `@Fork(jvmArgsPrepend = "--enable-native-access=ALL-UNNAMED")`.
- Tests in `java/src/test/java/.../PanamaTests.java` verify roundtrip behaviour.

### P/Invoke (.NET)
- `[DllImport("huff0")]` / `"fse"` — use the **bare logical name**, never the platform-specific filename.
- Native binaries ship in NuGet packages under `runtimes/{rid}/native/`.
- Benchmark Main uses `BenchmarkSwitcher` to properly route CLI args (`--job`, `--exporters`, `--artifacts`).

### CMake / JNI headers
- `JAVA_HOME` must be set before CMake configure.
- `WINDOWS_EXPORT_ALL_SYMBOLS ON` ensures all C functions are exported from DLL.

### Maven source layout
| Maven concept | Actual path |
|---|---|
| Main sources | `java/src/main/java` |
| Panama sources | `java-panama/src/main/java` (added via build-helper-plugin) |
| Test sources | `java/src/test/java` |
| JMH sources | `java/src/jmh/java` (added via build-helper-plugin) |

Do not add a `src/main/java` directory — Maven will NOT pick it up; all source paths are explicitly configured.

### Windows vs Linux native output paths
| Platform | Library location |
|---|---|
| Linux / macOS | `native/build/libhuff0.so` (or `.dylib`) |
| Windows (MSVC) | `native/build/huff0.dll` (no Release/ subdirectory with default generator) |

## Testing
| Layer | Framework | How to run |
|---|---|---|
| Native C | CTest | `ctest --test-dir native/build -C Release` |
| Java JNI | JUnit 5 / Maven Surefire | `./mvnw test` |
| Java Panama | JUnit 5 / Maven Surefire | included in `./mvnw test` |
| .NET benchmarks | BenchmarkDotNet | `dotnet run -c Release --project csharp/Huff0.net/Benchmarks/Benchmarks.csproj -- --job short` |

For .NET on Linux, set `LD_LIBRARY_PATH=$PWD/native/build` before running benchmarks.

## Benchmark Output Format
### Native (`native_bench.txt`)
```
Huff0 compress [4KB, LOW_ENTROPY]: 1234.56 MB/s
FSE compress [4KB, LOW_ENTROPY]: 1234.56 MB/s
```
Parsed by `scripts/generate_dashboard.py` → `parse_native()`.

### JMH (`jmh_results.json`)
Standard JMH JSON format with `params: {"size": "4096", "dataType": "LOW_ENTROPY"}`.
Parsed by `parse_jmh()` — converts ops/s to MB/s per size.

### BenchmarkDotNet (`csharp/*/Benchmarks/artifacts/results/*.json`)
Standard BDN JSON with `Parameters: "DataType=LOW_ENTROPY, Size=4096"` and `Statistics.Mean` in ns.
Parsed by `parse_bdn()` — globs for `*.json` in `artifacts/results/`.

## Release Process
1. **`RC-vX.Y.Z`** tag → `rc.yaml`:
   - Builds + tests native on Linux x64, Linux arm64, macOS arm64, Win x64, Win arm64
   - Builds Java (tests + multi-platform JAR versioned from tag)
   - Builds .NET + populates `runtimes/` + packs NuGet `.nupkg`
   - Verifies benchmarks build and run; generates performance_dashboard.md
   - Uploads ZIPs + `.nupkg` as workflow artifacts (not published)

2. **`vX.Y.Z`** tag → `release.yaml`:
   - Same build + test as RC
   - Creates GitHub Release with ZIPs attached
   - Publishes NuGet packages to nuget.org via OIDC (NuGet trusted publishing)
   - Runs benchmarks and attaches performance_dashboard.md to GitHub Release

### Required setup
- NuGet: Create trusted publisher policy on nuget.org linking to `lhol/FSE_Wrapper` + `release.yaml`

## Common Gotchas
- **`JAVA_HOME` must be set** before running cmake.
- **`--enable-native-access=ALL-UNNAMED`** is required for Panama tests and benchmarks.
- **`DllImport` must use bare name** `"huff0"` / `"fse"` — never `"huff0.dll"` or `"libhuff0.so"`.
- **Maven sources are under `java/src/...` and `java-panama/src/...`** — not the default Maven layout.
- **`LD_LIBRARY_PATH`** needed for .NET P/Invoke on Linux when native libs aren't in a system path.
- **The `native/FiniteStateEntropy` submodule** must be checked out (`git submodule update --init --recursive`) before building.
- **JMH shaded JAR** requires `AppendingTransformer` for `META-INF/BenchmarkList` in maven-shade-plugin.
- **BenchmarkDotNet Main** must use `BenchmarkSwitcher.FromAssembly(...).Run(args)` to properly handle CLI args.

## Licensing
- **FiniteStateEntropy** submodule: BSD 2-Clause — Copyright (c) 2013-2015, Yann Collet
- **FSE_Wrapper** wrapper code: MIT — Copyright (c) Lars Holzinger

