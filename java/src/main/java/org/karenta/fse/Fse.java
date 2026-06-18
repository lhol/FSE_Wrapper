package org.karenta.fse;

public final class Fse {

    static {
        System.loadLibrary("fse");
    }

    private Fse() {}

    private static native byte[] compressNative(byte[] input);
    private static native byte[] decompressNative(byte[] input, int expectedSize);

    public static byte[] compress(byte[] input) {
        return compressNative(input);
    }

    public static byte[] decompress(byte[] compressed, int expectedSize) {
        return decompressNative(compressed, expectedSize);
    }
}
