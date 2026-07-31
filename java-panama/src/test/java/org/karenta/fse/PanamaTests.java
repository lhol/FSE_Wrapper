package org.karenta.fse;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Java 22+ Panama FFI binding (FsePanama).
 * Requires --enable-native-access=ALL-UNNAMED JVM flag (set in Surefire config).
 */
public class PanamaTests {

    private static final byte[] DATA;
    static {
        DATA = new byte[4096];
        for (int i = 0; i < DATA.length; i++) DATA[i] = (byte)(i % 32);
    }

    @Test
    public void testFsePanamaRoundtrip() throws Throwable {
        byte[] compressed = FsePanama.compress(DATA);
        assertTrue(compressed.length > 0, "compress must produce output");
        assertTrue(compressed.length < DATA.length, "compressible data must shrink");
        byte[] restored = FsePanama.decompress(compressed, DATA.length);
        assertArrayEquals(DATA, restored, "decompress must restore original");
    }

    @Test
    public void testFsePanamaSmallInput() throws Throwable {
        byte[] small = new byte[512];
        for (int i = 0; i < small.length; i++) small[i] = (byte)(i % 16);
        byte[] compressed = FsePanama.compress(small);
        assertTrue(compressed.length > 0);
        byte[] restored = FsePanama.decompress(compressed, small.length);
        assertArrayEquals(small, restored);
    }

    @Test
    public void testFsePanamaLargeInput() throws Throwable {
        byte[] large = new byte[65536];
        for (int i = 0; i < large.length; i++) large[i] = (byte)(i % 64);
        byte[] compressed = FsePanama.compress(large);
        assertTrue(compressed.length > 0);
        byte[] restored = FsePanama.decompress(compressed, large.length);
        assertArrayEquals(large, restored);
    }
}
