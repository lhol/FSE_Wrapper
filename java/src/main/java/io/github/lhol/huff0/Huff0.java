package io.github.lhol.huff0;

/**
 * Huff0 entropy compression/decompression via JNI.
 *
 * <p>Huff0 is a fast Huffman entropy coder used internally by Zstandard (zstd).
 * It achieves good compression ratios on data with non-uniform byte distributions
 * and is optimised for single-pass throughput.
 *
 * <p>The native library ({@code libhuff0.so} / {@code huff0.dll} / {@code libhuff0.dylib})
 * is loaded automatically from the classpath on first use. Requires Java 11+.
 *
 * <p>Thread safety: all methods are stateless and may be called concurrently.
 */
public final class Huff0 {

    static {
        io.github.lhol.NativeLoader.load("huff0");
    }

    private Huff0() {}

    private static native byte[] compressNative(byte[] input);
    private static native byte[] decompressNative(byte[] input, int expectedDecompressedSize);

    /**
     * Compresses {@code input} using the Huff0 entropy coder.
     *
     * @param input the raw bytes to compress; must not be {@code null}
     * @return the compressed bytes
     * @throws NullPointerException if {@code input} is {@code null}
     * @throws RuntimeException if compression fails (e.g. input too large or incompressible)
     */
    public static byte[] compress(byte[] input) {
        if (input == null) {
            throw new NullPointerException("input");
        }
        return compressNative(input);
    }

    /**
     * Decompresses {@code compressed} bytes previously produced by {@link #compress(byte[])}.
     *
     * @param compressed the Huff0-compressed bytes; must not be {@code null}
     * @param expectedDecompressedSize the exact length of the original uncompressed data
     * @return the restored original bytes
     * @throws NullPointerException if {@code compressed} is {@code null}
     * @throws IllegalArgumentException if {@code expectedDecompressedSize} is not positive
     * @throws RuntimeException if decompression fails (corrupt input or wrong size hint)
     */
    public static byte[] decompress(byte[] compressed, int expectedDecompressedSize) {
        if (compressed == null) {
            throw new NullPointerException("compressed");
        }
        if (expectedDecompressedSize <= 0) {
            throw new IllegalArgumentException("expectedDecompressedSize must be > 0");
        }
        return decompressNative(compressed, expectedDecompressedSize);
    }
}
