# FSE_Wrapper

Cross-platform **Huff0** and **FSE** compression wrappers for **Java** and **.NET**, backed by Cyan4973's native [FiniteStateEntropy](https://github.com/Cyan4973/FiniteStateEntropy) C library.

## What this project does

FSE_Wrapper exposes two entropy-coding algorithms:

- **Huff0** — a Huffman entropy coder optimised for fast single-pass compression and decompression of byte streams.
- **FSE** (Finite State Entropy) — a table-based implementation of Asymmetric Numeral Systems (tANS) that achieves near-theoretical entropy with high throughput.

Both algorithms are used internally by Zstandard (zstd). This library makes them directly available as lightweight JVM and .NET APIs backed by the same native C implementation.

## Components

| Component | Package / Artifact | Language |
|---|---|---|
| Native shared libraries | `huff0.dll` / `libhuff0.so` / `.dylib` | C |
| Native shared libraries | `fse.dll` / `libfse.so` / `.dylib` | C |
| Java API (JNI) | `org.karenta.huff0.Huff0` / `org.karenta.fse.Fse` | Java / JNI |
| Java API (Panama FFI) | `org.karenta.huff0.Huff0Panama` / `org.karenta.fse.FsePanama` | Java 21+ / Panama |
| .NET Huff0 library | `Huff0.Net` (NuGet) | C# / P/Invoke |
| .NET FSE library | `Fse.Net` (NuGet) | C# / P/Invoke |

## Prerequisites

| Tool | Version | Required for |
|---|---|---|
| CMake | 3.10+ | native build |
| JDK | 22+ | native JNI headers + Java build (FFM API finalized in Java 22) |
| .NET SDK | 8.0+ | .NET build |
| Maven | 3.9+ | Java build (or use included `mvnw`) |
| MSVC / GCC / Clang | any modern | native C compiler |

> `JAVA_HOME` must be set before running the native build — it is used to locate JNI headers.

## Installation

### Java (Maven)
```xml
<dependency>
  <groupId>org.karenta</groupId>
  <artifactId>FSE_Wrapper</artifactId>
  <version>1.0.0</version>
</dependency>
```

### .NET (NuGet)
```bash
dotnet add package Huff0.Net
dotnet add package Fse.Net
```

The NuGet packages bundle the correct native library for your platform automatically via the `runtimes/{rid}/native/` mechanism — no manual setup required.

## Usage

### Java — Huff0 (JNI)
```java
import org.karenta.huff0.Huff0;

byte[] input      = "Hello, world!".getBytes(StandardCharsets.UTF_8);
byte[] compressed = Huff0.compress(input);
byte[] restored   = Huff0.decompress(compressed, input.length);
```

### Java — Huff0 (Panama FFI, Java 21+)
```java
import org.karenta.huff0.Huff0Panama;

// Requires JVM flag: --enable-native-access=ALL-UNNAMED
byte[] input      = "Hello, world!".getBytes(StandardCharsets.UTF_8);
byte[] compressed = Huff0Panama.compress(input);
byte[] restored   = Huff0Panama.decompress(compressed, input.length);
```

### Java — FSE (JNI)
```java
import org.karenta.fse.Fse;

byte[] input      = "Hello, world!".getBytes(StandardCharsets.UTF_8);
byte[] compressed = Fse.compress(input);
byte[] restored   = Fse.decompress(compressed, input.length);
```

### Java — FSE (Panama FFI, Java 21+)
```java
import org.karenta.fse.FsePanama;

// Requires JVM flag: --enable-native-access=ALL-UNNAMED
byte[] input      = "Hello, world!".getBytes(StandardCharsets.UTF_8);
byte[] compressed = FsePanama.compress(input);
byte[] restored   = FsePanama.decompress(compressed, input.length);
```

### C# — Huff0
```csharp
using Huff0.Net;

byte[] input      = Encoding.UTF8.GetBytes("Hello, world!");
byte[] compressed = Huff0Interop.Compress(input);
byte[] restored   = Huff0Interop.Decompress(compressed, input.Length);
```

### C# — FSE
```csharp
using Fse.Net;

byte[] input      = Encoding.UTF8.GetBytes("Hello, world!");
byte[] compressed = FseInterop.Compress(input);
byte[] restored   = FseInterop.Decompress(compressed, input.Length);
```

> **Note:** Both Huff0 and FSE work best on data with non-uniform byte distributions. Highly random or already-compressed data will not reduce in size.

## Building from source

### Clone with submodules
```bash
git clone --recurse-submodules https://github.com/lhol/FSE_Wrapper.git
cd FSE_Wrapper
```

If you already cloned without `--recurse-submodules`:
```bash
git submodule update --init --recursive
```

### Build everything at once (recommended)
```powershell
./build.ps1
```

Useful flags:
```powershell
./build.ps1 -SkipNative    # skip native CMake build
./build.ps1 -SkipJava      # skip Maven build
./build.ps1 -SkipDotnet    # skip .NET build
./build.ps1 -SkipTests     # build without running tests
```

The script auto-detects `JAVA_HOME` from `~/.jdks` if it is not already set.

### Manual build

#### 1. Native libraries
```bash
cmake -S native -B native/build
cmake --build native/build --config Release
ctest --test-dir native/build -C Release --output-on-failure
```

#### 2. Java
```bash
./mvnw test                        # build + run JUnit tests
./mvnw -B package -DskipTests      # build shaded JAR without tests
```

#### 3. .NET
```bash
dotnet build csharp/Huff0.net/src/Huff0.Net.csproj -c Release
dotnet build csharp/Fse.net/src/Fse.Net.csproj    -c Release
```

## Architecture

### Current: JNI / P/Invoke

```
┌──────────────────────────────────────────────────────────┐
│  Java / .NET application code                            │
├─────────────────────────┬────────────────────────────────┤
│  Java API               │  .NET API                      │
│  org.karenta.huff0.*    │  Huff0.Net / Fse.Net           │
│  org.karenta.fse.*      │  (P/Invoke: [DllImport("huff0")]│
│  (JNI: loadLibrary)     │             [DllImport("fse")] │
├─────────────────────────┴────────────────────────────────┤
│  Native shared libraries                                 │
│  huff0_jni.c → huff0.dll / libhuff0.so / libhuff0.dylib  │
│  fse_jni.c   → fse.dll   / libfse.so   / libfse.dylib    │
├──────────────────────────────────────────────────────────┤
│  FiniteStateEntropy (git submodule, BSD-2-Clause)        │
│  huf_compress.c / huf_decompress.c / fse_compress.c …   │
└──────────────────────────────────────────────────────────┘
```

### Alternative: Java 21+ Panama FFI (available now)

This project **includes a working Panama FFI implementation** in `java-panama/src/main/java/` alongside the JNI bindings. Panama uses Java's native Foreign Function & Memory API — no C glue code is required.

```
┌──────────────────────────────────────────────────────────┐
│  Java 21+ application code                               │
├──────────────────────────────────────────────────────────┤
│  Panama API                                              │
│  org.karenta.huff0.Huff0Panama                           │
│  org.karenta.fse.FsePanama                               │
│  (java.lang.foreign.Linker — no JNI glue needed)         │
├──────────────────────────────────────────────────────────┤
│  Same native shared libraries as JNI path                │
│  huff0.dll / libhuff0.so / libhuff0.dylib                │
│  fse.dll   / libfse.so   / libfse.dylib                  │
├──────────────────────────────────────────────────────────┤
│  FiniteStateEntropy (git submodule, BSD-2-Clause)        │
└──────────────────────────────────────────────────────────┘
```

**Panama API example:**
```java
import org.karenta.huff0.Huff0Panama;

// Add JVM flag: --enable-native-access=ALL-UNNAMED
byte[] compressed = Huff0Panama.compress(data);
byte[] restored   = Huff0Panama.decompress(compressed, data.length);
```

Panama bindings are compiled and tested in CI alongside JNI. Choose JNI for Java 11+ compatibility; choose Panama for Java 21+ with potentially lower per-call overhead.

See [Foreign Function & Memory API (Java 21)](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/foreign/package-summary.html) for details.

## Release workflow

Releases are driven by Git tags and GitHub Actions:

| Tag | Workflow | Effect |
|---|---|---|
| `RC-vX.Y.Z` | `rc.yaml` | Build + test + NuGet pack + benchmark verification → workflow artifacts |
| `vX.Y.Z` | `release.yaml` | Build + test + GitHub Release + NuGet publish + benchmark results attached |

### NuGet publishing secret
Add a repository secret named **`NUGET_API_KEY`**.  
Obtain it from [nuget.org](https://www.nuget.org) → Account → API Keys → Create (Push permission).

## License

This project builds upon a BSD 2-Clause licensed part:

### FiniteStateEntropy (submodule)
```
Copyright (c) 2013-2015, Yann Collet
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
* Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

### FSE_Wrapper (this repository)
MIT Licence, copyright holder: Lars Holzinger.

## Credits

Built on top of **[FiniteStateEntropy](https://github.com/Cyan4973/FiniteStateEntropy)** by **Yann Collet (Cyan4973)** — the same entropy coding engine used inside Zstandard (zstd).
