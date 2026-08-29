package io.github.lhol.huff0;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * Huff0 entropy compression/decompression via the Java 22+ Panama Foreign Function API.
 *
 * <p>This class provides the same compress/decompress operations as {@link Huff0}
 * (the JNI variant) but uses {@code java.lang.foreign} instead of JNI for lower call overhead
 * and without requiring hand-written C glue code.
 *
 * <h2>Input size limits</h2>
 * <p>Same constraints as {@link Huff0}: input must be between {@value #MIN_COMPRESS_SIZE}
 * and {@value #MAX_COMPRESS_SIZE} bytes ({@code HUF_BLOCKSIZE_MAX = 128 KB}).
 * For larger data, split into blocks of at most {@value #MAX_COMPRESS_SIZE} bytes.
 *
 * <p><b>Requirements:</b>
 * <ul>
 *   <li>Java 22 or later (Foreign Function &amp; Memory API finalized in Java 22, JEP 454)</li>
 *   <li>JVM flag {@code --enable-native-access=ALL-UNNAMED} (or module declaration) at runtime</li>
 * </ul>
 *
 * <p>Thread safety: all methods are stateless and may be called concurrently.
 */
public final class Huff0Panama {

    /** Minimum bytes accepted by {@link #compress}. Same as {@code Huff0.MIN_COMPRESS_SIZE}. */
    public static final int MIN_COMPRESS_SIZE = 12;
    /** Maximum bytes accepted by {@link #compress} ({@code HUF_BLOCKSIZE_MAX = 128 KB}). */
    public static final int MAX_COMPRESS_SIZE = 128 * 1024;

    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;
    private static final MethodHandle MH_BOUND, MH_COMPRESS, MH_DECOMPRESS;

    static {
        io.github.lhol.NativeLoader.load("huff0");
        LOOKUP = SymbolLookup.loaderLookup();
        MH_BOUND = LINKER.downcallHandle(
                LOOKUP.find("huff0_compress_bound").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
        MH_COMPRESS = LINKER.downcallHandle(
                LOOKUP.find("huff0_compress").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        MH_DECOMPRESS = LINKER.downcallHandle(
                LOOKUP.find("huff0_decompress").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
    }

    private Huff0Panama() {}

    /**
     * Compresses {@code input} using Huff0 via the Panama FFI.
     *
     * @param input the raw bytes to compress; must be between {@value #MIN_COMPRESS_SIZE}
     *              and {@value #MAX_COMPRESS_SIZE} bytes
     * @return the compressed bytes
     * @throws NullPointerException     if {@code input} is null
     * @throws IllegalArgumentException if input length is outside [{@value #MIN_COMPRESS_SIZE},
     *                                  {@value #MAX_COMPRESS_SIZE}]
     * @throws IllegalStateException    if the data is not compressible by Huff0
     * @throws Throwable if the native FFI call encounters an unexpected error
     */
    public static byte[] compress(byte[] input) throws Throwable {
        if (input == null) throw new NullPointerException("input");
        if (input.length < MIN_COMPRESS_SIZE)
            throw new IllegalArgumentException(
                "input too small for Huff0 compression: " + input.length +
                " bytes (minimum: " + MIN_COMPRESS_SIZE + " bytes)");
        if (input.length > MAX_COMPRESS_SIZE)
            throw new IllegalArgumentException(
                "input too large for Huff0 compression: " + input.length +
                " bytes (maximum: " + MAX_COMPRESS_SIZE + " bytes = 128 KB). " +
                "Split into blocks of at most " + MAX_COMPRESS_SIZE + " bytes.");

        long bound = (long) MH_BOUND.invoke((long) input.length);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment src = arena.allocate(input.length);
            src.copyFrom(MemorySegment.ofArray(input));
            MemorySegment dst = arena.allocate(bound);
            long cSize = (long) MH_COMPRESS.invoke(src, (long) input.length, dst, bound);
            if (cSize == 0)
                throw new IllegalStateException(
                    "Huff0 compression produced no output: input (" + input.length +
                    " bytes) may not be compressible (random, encrypted, or uniform data)");
            return dst.asSlice(0, cSize).toArray(ValueLayout.JAVA_BYTE);
        }
    }

    /**
     * Decompresses {@code compressed} bytes previously produced by {@link #compress(byte[])}.
     *
     * @param compressed the Huff0-compressed bytes
     * @param expectedSize the exact length of the original uncompressed data
     * @return the restored original bytes
     * @throws NullPointerException     if {@code compressed} is null
     * @throws IllegalArgumentException if {@code expectedSize} is not positive
     * @throws IllegalStateException    if decompression fails (corrupt input or wrong size)
     * @throws Throwable if the native FFI call encounters an unexpected error
     */
    public static byte[] decompress(byte[] compressed, int expectedSize) throws Throwable {
        if (compressed == null) throw new NullPointerException("compressed");
        if (expectedSize <= 0)
            throw new IllegalArgumentException("expectedSize must be > 0");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment src = arena.allocate(compressed.length);
            src.copyFrom(MemorySegment.ofArray(compressed));
            MemorySegment dst = arena.allocate(expectedSize);
            long dSize = (long) MH_DECOMPRESS.invoke(src, (long) compressed.length, dst, (long) expectedSize);
            if (dSize != expectedSize)
                throw new IllegalStateException(
                    "Huff0 decompression failed: corrupt input or wrong expectedSize (" +
                    expectedSize + ")");
            return dst.asSlice(0, expectedSize).toArray(ValueLayout.JAVA_BYTE);
        }
    }
}


