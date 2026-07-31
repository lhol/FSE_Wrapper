package org.karenta.huff0;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Java 21 Panama FFI binding (Huff0Panama).
 * Requires --enable-native-access=ALL-UNNAMED JVM flag (set via Surefire config).
 */
public class PanamaTests {

    private static final byte[] DATA;
    static {
        DATA = new byte[4096];
        for (int i = 0; i < DATA.length; i++) DATA[i] = (byte)(i % 32);
    }

    @Test
    public void testHuff0PanamaRoundtrip() throws Throwable {
        byte[] compressed = Huff0Panama.compress(DATA);
        assertTrue(compressed.length > 0, "Panama compress must produce output");
        assertTrue(compressed.length < DATA.length, "Compressible data must shrink");
        byte[] restored = Huff0Panama.decompress(compressed, DATA.length);
        assertArrayEquals(DATA, restored, "Panama decompress must restore original");
    }

    @Test
    public void testHuff0PanamaSmallInput() throws Throwable {
        byte[] small = new byte[512];
        for (int i = 0; i < small.length; i++) small[i] = (byte)(i % 16);
        byte[] compressed = Huff0Panama.compress(small);
        assertTrue(compressed.length > 0);
        byte[] restored = Huff0Panama.decompress(compressed, small.length);
        assertArrayEquals(small, restored);
    }

    @Test
    public void testHuff0PanamaLargeInput() throws Throwable {
        byte[] large = new byte[65536];
        for (int i = 0; i < large.length; i++) large[i] = (byte)(i % 64);
        byte[] compressed = Huff0Panama.compress(large);
        assertTrue(compressed.length > 0);
        byte[] restored = Huff0Panama.decompress(compressed, large.length);
        assertArrayEquals(large, restored);
    }
}
