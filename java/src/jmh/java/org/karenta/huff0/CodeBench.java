package org.karenta.huff0;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class CodeBench {

    private byte[] data;

    @Setup
    public void setup() {
        data = "The quick brown fox jumps over the lazy dog".getBytes();
    }

    @Benchmark
    public byte[] huff0Compress() {
        return Huff0.compress(data);
    }

}
