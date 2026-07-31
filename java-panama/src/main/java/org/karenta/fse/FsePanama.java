package org.karenta.fse;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public final class FsePanama {
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup LOOKUP;
    private static final MethodHandle MH_BOUND, MH_COMPRESS, MH_DECOMPRESS;

    static {
        System.loadLibrary("fse");
        LOOKUP = SymbolLookup.loaderLookup();
        MH_BOUND = LINKER.downcallHandle(
                LOOKUP.find("fse_compress_bound").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG));
        MH_COMPRESS = LINKER.downcallHandle(
                LOOKUP.find("fse_compress").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
        MH_DECOMPRESS = LINKER.downcallHandle(
                LOOKUP.find("fse_decompress").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG,
                        ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
    }

    private FsePanama() {}

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

    public static byte[] decompress(byte[] compressed, int expectedSize) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment src = arena.allocate(compressed.length);
            src.copyFrom(MemorySegment.ofArray(compressed));
            MemorySegment dst = arena.allocate(expectedSize);
            long dSize = (long) MH_DECOMPRESS.invoke(src, (long) compressed.length, dst, (long) expectedSize);
            if (dSize != expectedSize) throw new IllegalStateException("FSE decompression failed, got " + dSize);
            return dst.asSlice(0, expectedSize).toArray(ValueLayout.JAVA_BYTE);
        }
    }
}
