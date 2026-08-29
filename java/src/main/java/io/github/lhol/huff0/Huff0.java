package io.github.lhol.huff0;

/**
 * Huff0 entropy compression/decompression via JNI.
 *
 * <p>Huff0 is a fast Huffman entropy coder used internally by Zstandard (zstd).
 * It achieves good compression ratios on data with non-uniform byte distributions
 * and is optimised for single-pass throughput.
 *
 * <p>The native library ({@code libhuff0.so} / {@code huff0.dll} / {@code libhuff0.dylib})
 * is loaded automatically from the classpath on first use via {@link io.github.lhol.NativeLoader}.
 * Requires Java 11+.
 *
 * <h2>Minimum input size</h2>
 * <p>Huff0 requires at least {@value #MIN_COMPRESS_SIZE} bytes of input to attempt compression.
 * Inputs smaller than this are rejected with {@link IllegalArgumentException}.
 * Inputs that are too random or already-compressed may also be rejected even above this threshold
 * — Huff0 only stores data when it can actually reduce size.
 * For reliable compression, use inputs of at least 128 bytes with non-uniform byte distributions.
 *
 * <p>Thread safety: all methods are stateless and may be called concurrently.
 */
public final class Huff0 {

    /**
     * Minimum number of bytes that {@link #compress(byte[])} will accept.
     * This reflects the hard limit in the underlying {@code HUF_compress()} C function
     * ({@code srcSize < 12 → return 0}).
     */
    public static final int MIN_COMPRESS_SIZE = 12;

    static {
        io.github.lhol.NativeLoader.load("huff0");
    }

    private Huff0() {}

    private static native byte[] compressNative(byte[] input);
    private static native byte[] decompressNative(byte[] input, int expectedDecompressedSize);

    /**
     * Compresses {@code input} using the Huff0 entropy coder.
     *
     * @param input the raw bytes to compress; must not be {@code null};
     *              must have at least {@value #MIN_COMPRESS_SIZE} bytes
     * @return the compressed bytes (always shorter than {@code input} for compressible data)
     * @throws NullPointerException     if {@code input} is {@code null}
     * @throws IllegalArgumentException if {@code input.length < }{@value #MIN_COMPRESS_SIZE}
     * @throws IllegalStateException    if the data is not compressible by Huff0
     *                                  (e.g. random, encrypted, or already-compressed data)
     */
    public static byte[] compress(byte[] input) {
        if (input == null) {
            throw new NullPointerException("input");
        }
        if (input.length < MIN_COMPRESS_SIZE) {
            throw new IllegalArgumentException(
                "input too small for Huff0 compression: " + input.length +
                " bytes (minimum: " + MIN_COMPRESS_SIZE + " bytes)");
        }
        byte[] result = compressNative(input);
        if (result.length == 0) {
            throw new IllegalStateException(
                "Huff0 compression produced no output: input (" + input.length +
                " bytes) may not be compressible (random, encrypted, or uniform data)");
        }
        return result;
    }

    /**
     * Decompresses {@code compressed} bytes previously produced by {@link #compress(byte[])}.
     *
     * @param compressed the Huff0-compressed bytes; must not be {@code null}
     * @param expectedDecompressedSize the exact length of the original uncompressed data
     * @return the restored original bytes
     * @throws NullPointerException     if {@code compressed} is {@code null}
     * @throws IllegalArgumentException if {@code expectedDecompressedSize} is not positive
     * @throws IllegalStateException    if decompression fails (corrupt input or wrong size hint)
     */
    public static byte[] decompress(byte[] compressed, int expectedDecompressedSize) {
        if (compressed == null) {
            throw new NullPointerException("compressed");
        }
        if (expectedDecompressedSize <= 0) {
            throw new IllegalArgumentException("expectedDecompressedSize must be > 0");
        }
        byte[] result = decompressNative(compressed, expectedDecompressedSize);
        if (result.length == 0) {
            throw new IllegalStateException(
                "Huff0 decompression failed: corrupt input or wrong expectedDecompressedSize (" +
                expectedDecompressedSize + ")");
        }
        return result;
    }
}

