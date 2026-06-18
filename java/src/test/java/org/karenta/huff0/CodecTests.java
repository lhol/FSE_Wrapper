package org.karenta.huff0;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CodecTests {

    private static final byte[] DATA = 
        "The quick brown fox jumps over the lazy dog".getBytes();

    @Test
    public void testHuff0Roundtrip() {
        byte[] compressed = Huff0.compress(DATA);
        byte[] restored = Huff0.decompress(compressed, DATA.length);
        assertArrayEquals(DATA, restored);
    }
}
