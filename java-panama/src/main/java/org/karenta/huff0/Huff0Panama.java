package org.karenta.huff0;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public final class Huff0Panama {
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;
    private static final MethodHandle MH_BOUND, MH_COMPRESS, MH_DECOMPRESS;

    static {
        System.loadLibrary("huff0");
        LOOKUP = SymbolLookup.loaderLookup();
        MH_BOUND = LINKER.downcallHandle(
                LOOKUP.find("huff0_compress_bound").get(),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
        MH_COMPRESS = LINKER.downcallHandle(
                LOOKUP.find("huff0_compress").get(),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        MH_DECOMPRESS = LINKER.downcallHandle(
                LOOKUP.find("huff0_decompress").get(),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
    }

    private Huff0Panama() {}

    public static byte[] compress(byte[] input) throws Throwable {
        long bound = (long) MH_BOUND.invoke((long) input.length);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment src = arena.allocateArray(ValueLayout.JAVA_BYTE, input);
            MemorySegment dst = arena.allocate(bound);
            long cSize = (long) MH_COMPRESS.invoke(src, (long) input.length, dst, bound);
            byte[] out = new byte[(int) cSize];
            dst.asSlice(0, cSize).copyTo(MemorySegment.ofArray(out));
            return out;
        }
    }

    public static byte[] decompress(byte[] compressed, int expectedSize) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment src = arena.allocateArray(ValueLayout.JAVA_BYTE, compressed);
            MemorySegment dst = arena.allocate(expectedSize);
            long dSize = (long) MH_DECOMPRESS.invoke(src, (long) compressed.length, dst, (long) expectedSize);
            if (dSize != expectedSize) throw new IllegalStateException("Huff0 decompression failed");
            byte[] out = new byte[expectedSize];
            dst.asSlice(0, expectedSize).copyTo(MemorySegment.ofArray(out));
            return out;
        }
    }
}
