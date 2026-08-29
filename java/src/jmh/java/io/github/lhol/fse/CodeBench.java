package io.github.lhol.fse;

import org.openjdk.jmh.annotations.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class CodeBench {

    @Param({"512", "1024", "4096", "16384", "65536", "262144", "1048576", "4194304", "16777216"})
    public int size;

    @Param({"LOW_ENTROPY", "MEDIUM_ENTROPY"})
    public String dataType;

    private byte[] data;

    // ~3.8 KB of mixed Wikipedia-style text with numbers, punctuation, and varied entropy.
    // Long enough that large benchmark sizes do not repeat a trivially short pattern.
    private static final String MEDIUM_TEXT =
        "Data compression reduces the size of data by encoding information more efficiently. " +
        "Lossless compression algorithms like Huffman coding, arithmetic coding, and Asymmetric " +
        "Numeral Systems (ANS) exploit statistical redundancy in input data. Huffman coding assigns " +
        "shorter codes to more frequent symbols: a symbol with probability 0.5 gets a 1-bit code, " +
        "while a symbol with probability 0.0625 gets a 4-bit code. The theoretical limit is given " +
        "by Shannon entropy: H(X) = -sum p(x) * log2(p(x)). For a uniform distribution over 256 " +
        "symbols, H = 8 bits/symbol. For a biased source with 90% zeros and 10% ones, H ~= 0.469 " +
        "bits/symbol. Modern compressors approach this limit closely.\n" +
        "Finite State Entropy (FSE) is a tabled ANS implementation by Yann Collet (2013). " +
        "It encodes a symbol stream using a finite state machine with 2^tableLog states, typically " +
        "2048 (tableLog=11). Encoding symbol s from state x: next = (x/freq[s])<<1 + cumFreq[s] + " +
        "(x % freq[s]). Decoding: symbol = sym[x], next_x = (freq[sym] << (x>>tableLog)) + " +
        "(x & ((1<<tableLog)-1)). FSE achieves within 0.02 bits/symbol of Shannon entropy.\n" +
        "Huffman coding was invented by David A. Huffman in 1952 at MIT. The algorithm builds " +
        "a binary prefix tree bottom-up: starting with symbol nodes sorted by frequency, it " +
        "repeatedly combines the two least-frequent nodes into a parent, assigning 0 to the left " +
        "and 1 to the right branch. The resulting code is optimal among prefix-free codes. " +
        "However, Huffman codes require integer bit lengths, so probabilities like 1/3 cannot " +
        "be represented exactly. Arithmetic coding and ANS methods achieve sub-bit precision.\n" +
        "Real-world data has varying entropy. English text: ~1.0-1.5 bits true entropy per " +
        "character (Shannon, 1951 guessing experiment), but ~4.5 bits first-order symbol entropy. " +
        "Binary executables: 5-7 bits/byte. Random or encrypted data: ~8 bits/byte (incompressible). " +
        "Common benchmark sizes: 512 B, 1 KB, 4 KB, 16 KB, 64 KB, 256 KB, 1 MB, 4 MB, 16 MB. " +
        "At 512 B, setup overhead dominates. At 16 MB (16,777,216 bytes), memory bandwidth is " +
        "the bottleneck. Peak throughput (L2/L3 cache range): 64 KB to 1 MB.\n" +
        "Huff0 compress: 1200-2500 MB/s on x86-64 @ 3 GHz. FSE compress: 800-1800 MB/s. " +
        "Decompression is typically 20-50% faster than compression. JNI overhead per call: " +
        "200-500 ns. Panama FFI reduces this to ~80-120 ns. For 512-byte buffers at 2000 MB/s " +
        "effective throughput, time per call = 512 / (2000*1024*1024) = ~244 ns; JNI overhead " +
        "is therefore ~82-205% of the useful work at 512 B but negligible at 64 KB+.\n" +
        "Zstandard (zstd) uses both Huff0 (for literals) and FSE (for sequences/lengths). " +
        "zstd level 1 achieves ~500 MB/s compression and ~1700 MB/s decompression on the " +
        "Silesia corpus (211 MB, ratio 2.88x). LZ4 achieves ~750 MB/s compression and ~4500 " +
        "MB/s decompression at ratio 2.10x. Brotli level 4 achieves ~150 MB/s at ratio 3.05x. " +
        "The tradeoffs between speed, ratio, and memory use determine which algorithm is best " +
        "for a given workload. High-throughput streaming pipelines prefer LZ4 or Snappy; " +
        "cold storage and network transfer prefer zstd or Brotli. FSE and Huff0 are rarely " +
        "used standalone but are critical building blocks inside zstd v1.5.5 (released 2022-07-13, " +
        "commit a7519a8b9b3f, 6734 stars on GitHub as of 2024-03-01). Version 0.1.1 of this " +
        "library wraps the FiniteStateEntropy reference implementation (tag v0.2.0, 2018-08-01). ";

    @Setup
    public void setup() {
        data = new byte[size];
        if ("LOW_ENTROPY".equals(dataType)) {
            for (int i = 0; i < size; i++) data[i] = (byte)(i % 32);
        } else {
            byte[] src = MEDIUM_TEXT.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < size; i++) data[i] = src[i % src.length];
        }
    }

    @Benchmark
    public byte[] fseCompressJNI() {
        return Fse.compress(data);
    }
}

