package org.karenta.fse;

import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
public class CodecBench {

    private byte[] data;

    @Setup
    public void setup() {
        data = "The quick brown fox jumps over the lazy dog".getBytes();
    }

    @Benchmark
    public byte[] fseCompress() {
        return Fse.compress(data);
    }
}
