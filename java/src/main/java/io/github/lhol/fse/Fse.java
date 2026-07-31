package io.github.lhol.fse;

/**
 * FSE (Finite State Entropy) compression/decompression via JNI.
 *
 * <p>FSE is a table-based implementation of Asymmetric Numeral Systems (tANS) that achieves
 * near-theoretical entropy limits. It is used internally by Zstandard (zstd) and excels at
 * highly repetitive or structured data with long symbol runs.
 *
 * <p>The native library ({@code libfse.so} / {@code fse.dll} / {@code libfse.dylib})
 * is loaded automatically from the classpath on first use. Requires Java 11+.
 *
 * <p>Thread safety: all methods are stateless and may be called concurrently.
 */
public final class Fse {

    static {
        System.loadLibrary("fse");
    }

    private Fse() {}

    private static native byte[] compressNative(byte[] input);
    private static native byte[] decompressNative(byte[] input, int expectedSize);

    /**
     * Compresses {@code input} using the FSE entropy coder.
     *
     * @param input the raw bytes to compress; must not be {@code null}
     * @return the compressed bytes
     * @throws NullPointerException if {@code input} is {@code null}
     * @throws RuntimeException if compression fails
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
     * @param compressed the FSE-compressed bytes; must not be {@code null}
     * @param expectedSize the exact length of the original uncompressed data
     * @return the restored original bytes
     * @throws NullPointerException if {@code compressed} is {@code null}
     * @throws RuntimeException if decompression fails (corrupt input or wrong size hint)
     */
    public static byte[] decompress(byte[] compressed, int expectedSize) {
        if (compressed == null) {
            throw new NullPointerException("compressed");
        }
        return decompressNative(compressed, expectedSize);
    }
}
