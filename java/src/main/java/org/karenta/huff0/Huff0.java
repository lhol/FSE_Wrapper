package org.karenta.huff0;

public final class Huff0 {

    static {
        // Library name without prefix/suffix:
        //   Linux: libhuff0_jni.so
        //   macOS: libhuff0_jni.dylib
        //   Windows: huff0_jni.dll
        System.loadLibrary("huff0");
    }

    private Huff0() {}

    // Native methods
    private static native byte[] compressNative(byte[] input);
    private static native byte[] decompressNative(byte[] input, int expectedDecompressedSize);

    public static byte[] compress(byte[] input) {
        if (input == null) {
            throw new NullPointerException("input");
        }
        return compressNative(input);
    }

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
