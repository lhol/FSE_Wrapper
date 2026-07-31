# Changelog

All notable changes to FSE_Wrapper are documented here.

Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).  
Versions follow [Semantic Versioning](https://semver.org/).

---

## [Unreleased]
---
## [0.1.1] — 2026-08-01
### Added
- Maven Release workflow (`release-maven.yaml`) for publishing both JNI and Panama JARs to Maven Central via the Central Publisher Portal.
---

## [0.0.17] — 2026

### Added
- NuGet packages `Huff0.Net` and `Fse.Net` published to nuget.org

---

## [0.0.16]

### Added
- Expanded NuGet native library matrix (linux-arm64, win-arm64)
- Version numbers in ZIP artifact filenames

---

## [0.0.15] — 2026-07-31

### Added
- Java 22+ Panama FFI bindings (`Huff0Panama`, `FsePanama`) as a separate Maven project (`FSE_Wrapper-panama`)
- Panama FFI tests (6 tests, roundtrip at small/medium/large sizes)
- Multi-size benchmarks (9 sizes: 512B → 16MB) with two entropy data types (LOW/MEDIUM)
- Native `bench_codecs` benchmark extended to all sizes and data types
- `.NET` `BenchmarkSwitcher` wiring so `--job`/`--exporters`/`--artifacts` CLI args work
- Performance dashboard (`scripts/generate_dashboard.py`) rewritten for parameterized multi-size output
- `specs/v0.1.1_req.md` and `specs/v0.1.1_spec.md` roadmap files
- Javadoc on all four public Java classes
- Maven Central metadata (`<licenses>`, `<developers>`, `<scm>`, `<distributionManagement>`)
- `maven-source-plugin`, `maven-javadoc-plugin`, `maven-gpg-plugin` in both pom files
- `release-maven.yaml` workflow for OSSRH/Maven Central publishing
- `CHANGELOG.md` (this file)

### Changed
- Separated JNI (Java 11+) and Panama (Java 22+) into two Maven projects with two JARs
- pom.xml compiler level: Java 11 (JNI); java-panama/pom.xml: Java 22
- GitHub Actions: `java-version: '22'` across all jobs
- `upload-artifact@v4` and `download-artifact@v4` (v4 targets Node.js 24)
- `rc.yaml` completely rewritten for clean YAML structure

### Fixed
- Panama FFM API compilation error — FFM was preview in Java 21, finalized in Java 22
- Node.js 20 deprecation warnings for `upload-artifact` and `download-artifact` actions

---

## [0.0.14]

### Fixed
- Benchmark dashboard script path references
- JMH BenchmarkList missing at runtime (AppendingTransformer added to maven-shade-plugin)
- Java PanamaTests class/filename mismatch

---

## [0.0.13]

### Added
- Multi-platform native build matrix: `linux-x64`, `linux-arm64`, `macos-arm64`, `win-x64`, `win-arm64`
- Architecture-specific ZIP artifact names
- NuGet package README files for Huff0.Net and Fse.Net

### Changed
- GitHub Actions upgraded to v5 (checkout, setup-java, setup-dotnet)

---

## [0.0.12]

### Added
- GitHub release workflow (`release.yaml`) triggered by `v*.*.*` tags
- RC workflow (`rc.yaml`) triggered by `RC-v*.*.*` tags
- NuGet packages bundled with all platform native libraries via `runtimes/{rid}/native/`
- `build.ps1` cross-platform local build script

---

## [0.0.1] — initial

### Added
- Native C wrappers for Huff0 and FSE (FiniteStateEntropy submodule)
- Java JNI bindings (`io.github.lhol.huff0.Huff0`, `io.github.lhol.fse.Fse`)
- .NET P/Invoke bindings (`Huff0.Net`, `Fse.Net`)
- CMake build for native shared libraries
- Basic JUnit 5 tests for JNI roundtrip

[Unreleased]: https://github.com/lhol/FSE_Wrapper/compare/v0.0.17...HEAD
[0.0.17]: https://github.com/lhol/FSE_Wrapper/compare/v0.0.16...v0.0.17
[0.0.16]: https://github.com/lhol/FSE_Wrapper/compare/v0.0.15...v0.0.16
[0.0.15]: https://github.com/lhol/FSE_Wrapper/compare/v0.0.14...v0.0.15
[0.0.14]: https://github.com/lhol/FSE_Wrapper/compare/v0.0.13...v0.0.14
[0.0.13]: https://github.com/lhol/FSE_Wrapper/compare/v0.0.12...v0.0.13
[0.0.12]: https://github.com/lhol/FSE_Wrapper/compare/v0.0.1...v0.0.12
[0.0.1]: https://github.com/lhol/FSE_Wrapper/releases/tag/v0.0.1
