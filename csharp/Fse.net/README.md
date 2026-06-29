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

