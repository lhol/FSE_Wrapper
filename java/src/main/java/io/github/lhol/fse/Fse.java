package io.github.lhol.fse;

/**
 * FSE (Finite State Entropy) compression/decompression via JNI.
 *
 * <p>FSE is a table-based implementation of Asymmetric Numeral Systems (tANS) that achieves
 * near-theoretical entropy limits. It is used internally by Zstandard (zstd) and excels at
 * highly repetitive or structured data with long symbol runs.
 *
 * <p>The native library ({@code libfse.so} / {@code fse.dll} / {@code libfse.dylib})
 * is loaded automatically from the classpath on first use via {@link io.github.lhol.NativeLoader}.
 * Requires Java 11+.
 *
 * <h2>Minimum input size</h2>
 * <p>FSE requires at least {@value #MIN_COMPRESS_SIZE} bytes of input to attempt compression.
 * Inputs smaller than this are rejected with {@link IllegalArgumentException}.
 * FSE also rejects data that is not compressible (e.g. RLE / single-symbol sequences are
 * handled as a special case that returns 0). For reliable compression, use inputs of at least
 * 64 bytes with varied byte distributions.
 *
 * <p>Thread safety: all methods are stateless and may be called concurrently.
 */
public final class Fse {

    /**
     * Minimum number of bytes that {@link #compress(byte[])} will accept.
     * This reflects the hard limit in the underlying {@code FSE_compress()} C function
     * ({@code srcSize <= 2 → return 0}).
     */
    public static final int MIN_COMPRESS_SIZE = 3;

    static {
        io.github.lhol.NativeLoader.load("fse");
    }

    private Fse() {}

    private static native byte[] compressNative(byte[] input);
    private static native byte[] decompressNative(byte[] input, int expectedSize);

    /**
     * Compresses {@code input} using the FSE entropy coder.
     *
     * @param input the raw bytes to compress; must not be {@code null};
     *              must have at least {@value #MIN_COMPRESS_SIZE} bytes
     * @return the compressed bytes (always shorter than {@code input} for compressible data)
     * @throws NullPointerException     if {@code input} is {@code null}
     * @throws IllegalArgumentException if {@code input.length < }{@value #MIN_COMPRESS_SIZE}
     * @throws IllegalStateException    if the data is not compressible by FSE
     *                                  (e.g. single-symbol / RLE / already-compressed data)
     */
    public static byte[] compress(byte[] input) {
        if (input == null) {
            throw new NullPointerException("input");
        }
        if (input.length < MIN_COMPRESS_SIZE) {
            throw new IllegalArgumentException(
                "input too small for FSE compression: " + input.length +
                " bytes (minimum: " + MIN_COMPRESS_SIZE + " bytes)");
        }
        byte[] result = compressNative(input);
        if (result.length == 0) {
            throw new IllegalStateException(
                "FSE compression produced no output: input (" + input.length +
                " bytes) may not be compressible (single-symbol, RLE, or random data)");
        }
        return result;
    }

    /**
     * Decompresses {@code compressed} bytes previously produced by {@link #compress(byte[])}.
     *
     * @param compressed the FSE-compressed bytes; must not be {@code null}
     * @param expectedSize the exact length of the original uncompressed data
     * @return the restored original bytes
     * @throws NullPointerException     if {@code compressed} is {@code null}
     * @throws IllegalArgumentException if {@code expectedSize} is not positive
     * @throws IllegalStateException    if decompression fails (corrupt input or wrong size hint)
     */
    public static byte[] decompress(byte[] compressed, int expectedSize) {
        if (compressed == null) {
            throw new NullPointerException("compressed");
        }
        if (expectedSize <= 0) {
            throw new IllegalArgumentException("expectedSize must be > 0");
        }
        byte[] result = decompressNative(compressed, expectedSize);
        if (result.length == 0) {
            throw new IllegalStateException(
                "FSE decompression failed: corrupt input or wrong expectedSize (" +
                expectedSize + ")");
        }
        return result;
    }
}

