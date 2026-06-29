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

# Full Licence File Contents
## MIT License

Copyright (c) 2026 Lars Holzinger

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

---

## Third-party licenses

### FiniteStateEntropy (git submodule)
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
