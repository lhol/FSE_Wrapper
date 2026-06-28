# FSE_Wrapper Agent Notes

## Purpose
FSE_Wrapper is a cross-platform wrapper around Cyan4973's FiniteStateEntropy C library, exposing:

- **Huff0** to Java and .NET
- **FSE** to Java and .NET

The repository builds native shared libraries (`.dll`, `.so`, `.dylib`) and thin managed bindings:

- **JNI** for Java
- **P/Invoke** for .NET

## Main Components
1. **native/**  
   CMake-based native wrappers over the FiniteStateEntropy submodule. Produces the shared libraries consumed by Java and .NET.

2. **java/**  
   Java APIs for Huff0 and FSE. Uses JNI and bundles native libraries into the JAR.

3. **csharp/**  
   .NET libraries and benchmarks. Uses P/Invoke and bundles native libraries in NuGet packages under `runtimes/{rid}/native/`.

## Repository Structure
```text
FSE_Wrapper/
├── native/
│   ├── FiniteStateEntropy/      # git submodule: upstream C implementation
│   ├── huff0_jni.c              # Huff0 JNI + P/Invoke wrapper
│   ├── fse_jni.c                # FSE JNI + P/Invoke wrapper
│   ├── tests/test_codecs.c      # native unit tests run via ctest
│   ├── bench/bench_codecs.c     # native microbenchmarks
│   └── CMakeLists.txt           # native build config; requires JNI headers
│
├── java/
│   └── src/
│       ├── main/java/org/karenta/huff0/Huff0.java
│       ├── main/java/org/karenta/fse/Fse.java
│       ├── test/java/org/karenta/*/CodecTests.java
│       └── jmh/java/org/karenta/*/CodeBench.java
│
├── csharp/
│   ├── Huff0.net/
│   │   ├── src/                 # netstandard2.0 library project
│   │   ├── Benchmarks/          # net8.0 BenchmarkDotNet executable
│   │   └── runtimes/            # native libs packed into NuGet package
│   │       ├── win-x64/native/
│   │       ├── linux-x64/native/
│   │       └── osx-arm64/native/
│   └── Fse.net/                 # identical structure for FSE
│
├── scripts/generate_dashboard.py
├── pom.xml                      # Maven root; sources live under java/src/...
├── mvnw / mvnw.cmd              # Maven wrapper
├── .mvn/wrapper/                # maven-wrapper.jar + properties
├── Dockerfile                   # multi-stage build for all three components
├── build.ps1                    # orchestrated local build/test script (cross-platform PowerShell)
└── .github/workflows/
    ├── rc.yaml                  # triggered by RC-vX.Y.Z → build + test + NuGet pack + bench verify
    └── release.yaml             # triggered by vX.Y.Z → GitHub Release + NuGet publish + bench
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

#### .NET
```bash
dotnet build csharp/Huff0.net/src/Huff0.Net.csproj -c Release
dotnet build csharp/Fse.net/src/Fse.Net.csproj -c Release
```

## Architecture Notes

### JNI (Java)
- `System.loadLibrary("huff0")` and `System.loadLibrary("fse")` load the native libs at class-init time.
- Maven `copy-resources` copies `.so`/`.dylib`/`.dll` from `native/build/` and `native/build/Release/` into `target/classes/natives/` so they ship inside the JAR.
- Maven Surefire sets `-Djava.library.path=native/build:native/build/Release` for tests.

### P/Invoke (.NET)
- `[DllImport("huff0")]` and `[DllImport("fse")]` — use the **bare logical name**, never the platform-specific filename.
- .NET runtime resolves `"huff0"` → `huff0.dll` / `libhuff0.so` / `libhuff0.dylib` automatically.
- Native binaries ship in NuGet packages under `runtimes/{rid}/native/`, which the .NET tooling copies to the output directory.

### CMake / JNI headers
- `JAVA_HOME` must be set (env var) for the CMake configure step to find JNI headers.
- `CMakeLists.txt` reads `$ENV{JAVA_HOME}` on Windows; on Linux/macOS it calls `find_package(JNI)`.
- `WINDOWS_EXPORT_ALL_SYMBOLS ON` is set so all C functions are exported from the DLL.

### Maven source layout
The `pom.xml` is at the repo root but Java sources are **not** in the default Maven paths:
| Maven concept | Actual path |
|---|---|
| Main sources | `java/src/main/java` |
| Test sources | `java/src/test/java` |
| JMH sources | `java/src/jmh/java` |

Do not add a `src/main/java` directory assuming Maven will pick it up.

### Windows vs Linux native output paths
| Platform | Library location |
|---|---|
| Linux / macOS | `native/build/libhuff0.so` (or `.dylib`) |
| Windows (MSVC) | `native/build/Release/huff0.dll` |

The pom.xml `copy-resources` handles both paths with two separate `<resource>` entries.

## Testing
| Layer | Framework | How to run |
|---|---|---|
| Native C | CTest | `ctest --test-dir native/build -C Release` |
| Java | JUnit 5 / Maven Surefire | `./mvnw test` |
| .NET benchmarks | BenchmarkDotNet | `dotnet run -c Release --project csharp/Huff0.net/Benchmarks/Benchmarks.csproj` |

For .NET on Linux, set `LD_LIBRARY_PATH=$PWD/native/build` before running benchmarks.

## Common Gotchas
- **`JAVA_HOME` must be set** before running cmake — it's checked in `CMakeLists.txt` as a FATAL_ERROR on Windows.
- **Windows DLLs are in `Release/` subdirectory**, not directly in `native/build/`.
- **`DllImport` must use bare name** `"huff0"` / `"fse"` — never `"huff0.dll"` or `"libhuff0.so"`.
- **`#if WINDOWS` / `#elif OSX`** are not defined by the .NET compiler — do not use them for runtime platform detection; use `[DllImport("huff0")]` instead.
- **Maven sources are under `java/src/...`** — the root `src/` directory does not exist and should not be created.
- **`LD_LIBRARY_PATH`** is needed for .NET P/Invoke on Linux when native libs aren't in a system path.
- **The `native/FiniteStateEntropy` submodule** must be checked out (`git submodule update --init --recursive`) before building.
- **`bench_codecs.c`** uses `CLOCK_MONOTONIC` on Linux/macOS and `QueryPerformanceCounter` on Windows — do not remove the `#ifdef _WIN32` guard.
- **Variable name `MSG`** conflicts with `windows.h` macro — in C files, avoid using `MSG` as a variable name.
- **JNI array pointer must not be used after `ReleaseByteArrayElements`** — copy data before releasing, not after.

## Release Process
1. **`RC-vX.Y.Z`** tag → `rc.yaml`:
   - Builds + tests native on Linux / macOS / Windows (parallel matrix)
   - Builds Java (tests + multi-platform JAR)
   - Builds .NET + populates `runtimes/` + packs NuGet `.nupkg`
   - Verifies benchmarks build and run
   - Uploads ZIPs + `.nupkg` as workflow artifacts (not published)

2. **`vX.Y.Z`** tag → `release.yaml`:
   - Same build + test as RC
   - Creates GitHub Release with ZIPs attached
   - Publishes NuGet packages to nuget.org via `NUGET_API_KEY` secret
   - Runs benchmarks and uploads results to the GitHub Release

### Required secret
`NUGET_API_KEY` — obtain from nuget.org → Account → API Keys.

## Do Not Change Without Understanding Impact
- `DllImport` library names (`"huff0"`, `"fse"`)
- `System.loadLibrary(...)` names
- Maven source/test directory configuration in `pom.xml`
- CMake `JAVA_HOME` handling and JNI include logic
- `runtimes/{rid}/native/` NuGet layout
- Workflow tag conventions (`RC-vX.Y.Z` vs `vX.Y.Z`)
- The `native/FiniteStateEntropy` submodule and its BSD-2-Clause license

## Licensing
- **FiniteStateEntropy** submodule: BSD 2-Clause — Copyright (c) 2013-2015, Yann Collet
- **FSE_Wrapper** wrapper code: BSD 2-Clause
