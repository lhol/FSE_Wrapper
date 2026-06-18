package org.karenta.fse;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CodecTests {

    private static final byte[] DATA = 
        "The quick brown fox jumps over the lazy dog".getBytes();

    @Test
    public void testFseRoundtrip() {
        byte[] compressed = Fse.compress(DATA);
        byte[] restored = Fse.decompress(compressed, DATA.length);
        assertArrayEquals(DATA, restored);
    }
}
