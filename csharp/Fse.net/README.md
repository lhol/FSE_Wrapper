# Fse.Net

**Modern Finite State Entropy (tANS) compression for .NET**, backed by Cyan4973's native [FiniteStateEntropy](https://github.com/Cyan4973/FiniteStateEntropy) C library.

## What is FSE?

**FSE** (Finite State Entropy) is a table-based implementation of **Asymmetric Numeral Systems (tANS)** that achieves near-theoretical entropy limits with high throughput. It's used internally by Zstandard (zstd) for superior compression on highly repetitive data.

## Installation

```bash
dotnet add package Fse.Net
```

## Usage

```csharp
using Fse.Net;

// Compress
byte[] input = Encoding.UTF8.GetBytes("Hello, world!");
byte[] compressed = FseInterop.Compress(input);

// Decompress
byte[] restored = FseInterop.Decompress(compressed, input.Length);
```

## Platform Support

This package includes native libraries for:
- **Windows**: x64, ARM64
- **Linux**: x64, ARM64
- **macOS**: ARM64 (Apple Silicon)

The correct native binary is automatically selected at runtime based on your platform.

## Performance

FSE typically achieves:
- **Compression ratio**: 40-80% on repetitive data
- **Speed**: 150-300 MB/s compression, 400-600 MB/s decompression
- **Best for**: Highly repetitive data, logs, structured text with high entropy
- **Advantage**: Better ratio than Huff0 on data with long symbol runs

> **Note:** Random or already-compressed data may not reduce in size. Test with your own data to determine suitability.

## FSE vs. Huff0

| Aspect | Huff0 | FSE |
|--------|-------|-----|
| Compression ratio | Good (30-70%) | Excellent (40-80%) |
| Speed | Very fast | Fast |
| Use case | General data | Highly repetitive data |
| Complexity | Simple Huffman | Asymmetric Numeral Systems |

## How it Works

FSE uses **Asymmetric Numeral Systems (tANS)**:

1. Analyze input to build frequency distribution
2. Create finite state machine with symbol-to-state transitions
3. Encode stream backwards using state machine
4. On decompression, walk forward through the state machine
5. Generates optimal compressed representation

This approach achieves entropy limits with single-pass compression/decompression.

## License

This project is licensed under the **MIT License**. See the LICENSE file in the repository root for details.

### Included Libraries

The native FSE library is built from [FiniteStateEntropy](https://github.com/Cyan4973/FiniteStateEntropy), which is licensed under the **BSD 2-Clause License**. See the LICENSE file for full attribution.

## Related Projects

- **Huff0.Net** — Huffman entropy coding for .NET
- **[Zstandard](https://github.com/facebook/zstd)** — Modern compression using both Huff0 and FSE
- **[FiniteStateEntropy](https://github.com/Cyan4973/FiniteStateEntropy)** — Original C library

## Contributing

Issues and pull requests are welcome at [lhol/FSE_Wrapper](https://github.com/lhol/FSE_Wrapper).
