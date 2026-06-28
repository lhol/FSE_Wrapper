package org.karenta.fse;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CodecTests {

    /* 512 bytes of repeating 0-31 pattern — always compressible */
    private static final byte[] DATA = new byte[512];
    static {
        for (int i = 0; i < DATA.length; i++) DATA[i] = (byte)(i % 32);
    }

    @Test
    public void testFseRoundtrip() {
        byte[] compressed = Fse.compress(DATA);
        assertTrue(compressed.length > 0, "Compression must produce output");
        byte[] restored = Fse.decompress(compressed, DATA.length);
        assertArrayEquals(DATA, restored);
    }
}
