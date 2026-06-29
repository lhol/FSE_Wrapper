# Huff0.Net

**Fast Huffman entropy coding for .NET**, backed by Cyan4973's native [FiniteStateEntropy](https://github.com/Cyan4973/FiniteStateEntropy) C library.

## What is Huff0?

**Huff0** is a Huffman entropy coder optimized for fast single-pass compression and decompression of byte streams. It achieves excellent compression on data with non-uniform byte distributions and is used internally by Zstandard (zstd).

## Installation

```bash
dotnet add package Huff0.Net
```

## Usage

```csharp
using Huff0.Net;

// Compress
byte[] input = Encoding.UTF8.GetBytes("Hello, world!");
byte[] compressed = Huff0Interop.Compress(input);

// Decompress
byte[] restored = Huff0Interop.Decompress(compressed, input.Length);
```

## Platform Support

This package includes native libraries for:
- **Windows**: x64, ARM64
- **Linux**: x64, ARM64
- **macOS**: ARM64 (Apple Silicon)

The correct native binary is automatically selected at runtime based on your platform.

## Performance

Huff0 typically achieves:
- **Compression ratio**: 30-70% depending on data entropy
- **Speed**: 300-500 MB/s compression, 1000+ MB/s decompression
- **Best for**: Text, logs, structured data with repetitive patterns

> **Note:** Highly random or already-compressed data may not reduce in size. Test with your own data to determine suitability.

## How it Works

1. Analyze the input stream to build a frequency table
2. Generate optimal Huffman codes based on byte frequencies
3. Encode the table in the compressed stream
4. Compress the input using the generated codes
5. On decompression, read the table and decode the stream

## License

This project is licensed under the **MIT License**. See the LICENSE file in the repository root for details.

### Included Libraries

The native Huff0 library is built from [FiniteStateEntropy](https://github.com/Cyan4973/FiniteStateEntropy), which is licensed under the **BSD 2-Clause License**. See the LICENSE file for full attribution.

## Related Projects

- **Fse.Net** — FSE (Finite State Entropy) for .NET
- **[Zstandard](https://github.com/facebook/zstd)** — Modern compression using both Huff0 and FSE
- **[FiniteStateEntropy](https://github.com/Cyan4973/FiniteStateEntropy)** — Original C library

## Contributing

Issues and pull requests are welcome at [lhol/FSE_Wrapper](https://github.com/lhol/FSE_Wrapper).
