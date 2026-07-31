package org.karenta.huff0;

import org.openjdk.jmh.annotations.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class CodeBench {

    @Param({"512", "1024", "4096", "16384", "65536", "262144", "1048576", "4194304", "16777216"})
    public int size;

    @Param({"LOW_ENTROPY", "MEDIUM_ENTROPY"})
    public String dataType;

    private byte[] data;

    private static final String LOREM =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor " +
        "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud " +
        "exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure " +
        "dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
        "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt " +
        "mollit anim id est laborum. ";

    @Setup
    public void setup() {
        data = new byte[size];
        if ("LOW_ENTROPY".equals(dataType)) {
            for (int i = 0; i < size; i++) data[i] = (byte)(i % 32);
        } else {
            byte[] src = LOREM.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < size; i++) data[i] = src[i % src.length];
        }
    }

    @Benchmark
    public byte[] huff0Compress() {
        return Huff0.compress(data);
    }
}

