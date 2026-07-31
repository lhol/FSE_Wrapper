package io.github.lhol.huff0;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

/**
 * Huff0 entropy compression/decompression via the Java 22+ Panama Foreign Function API.
 *
 * <p>This class provides the same compress/decompress operations as {@code org.karenta.huff0.Huff0}
 * (the JNI variant) but uses {@code java.lang.foreign} instead of JNI for lower call overhead
 * and without requiring hand-written C glue code.
 *
 * <p><b>Requirements:</b>
 * <ul>
 *   <li>Java 22 or later (Foreign Function &amp; Memory API finalized in Java 22, JEP 454)</li>
 *   <li>JVM flag {@code --enable-native-access=ALL-UNNAMED} (or module declaration) at runtime</li>
 *   <li>The native {@code huff0} shared library accessible via {@code java.library.path}</li>
 * </ul>
 *
 * <p>Thread safety: all methods are stateless and may be called concurrently.
 */
public final class Huff0Panama {
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;
    private static final MethodHandle MH_BOUND, MH_COMPRESS, MH_DECOMPRESS;

    static {
        System.loadLibrary("huff0");
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
     * @param input the raw bytes to compress
     * @return the compressed bytes
     * @throws Throwable if compression fails or the native call encounters an error
     */
    public static byte[] compress(byte[] input) throws Throwable {
        long bound = (long) MH_BOUND.invoke((long) input.length);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment src = arena.allocate(input.length);
            src.copyFrom(MemorySegment.ofArray(input));
            MemorySegment dst = arena.allocate(bound);
            long cSize = (long) MH_COMPRESS.invoke(src, (long) input.length, dst, bound);
            return dst.asSlice(0, cSize).toArray(ValueLayout.JAVA_BYTE);
        }
    }

    /**
     * Decompresses {@code compressed} bytes previously produced by {@link #compress(byte[])}.
     *
     * @param compressed the Huff0-compressed bytes
     * @param expectedSize the exact length of the original uncompressed data
     * @return the restored original bytes
     * @throws IllegalStateException if the decompressed size does not match {@code expectedSize}
     * @throws Throwable if the native call encounters an error
     */
    public static byte[] decompress(byte[] compressed, int expectedSize) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment src = arena.allocate(compressed.length);
            src.copyFrom(MemorySegment.ofArray(compressed));
            MemorySegment dst = arena.allocate(expectedSize);
            long dSize = (long) MH_DECOMPRESS.invoke(src, (long) compressed.length, dst, (long) expectedSize);
            if (dSize != expectedSize) throw new IllegalStateException("Huff0 decompression failed, got " + dSize);
            return dst.asSlice(0, expectedSize).toArray(ValueLayout.JAVA_BYTE);
        }
    }
}

