using BenchmarkDotNet.Attributes;
using BenchmarkDotNet.Running;
using System.Text;

namespace Huff0.Net.Benchmarks
{
    [MemoryDiagnoser]
    public class CodecBenchmarks
    {
        // Huff0 hard limit: HUF_BLOCKSIZE_MAX = 128 KB; cap params well below that.
        [Params(512, 1024, 4096, 16384, 65536)]
        public int Size { get; set; }

        [Params("LOW_ENTROPY", "MEDIUM_ENTROPY")]
        public string DataType { get; set; } = "LOW_ENTROPY";

        private byte[] data = new byte[0];

        // ~1 KB mixed text — same corpus as the Java JMH benchmarks
        private static readonly byte[] LoremBytes = System.Text.Encoding.UTF8.GetBytes(
            "Data compression reduces the size of data by encoding information more efficiently. " +
            "Lossless compression algorithms like Huffman coding, arithmetic coding, and Asymmetric " +
            "Numeral Systems (ANS) exploit statistical redundancy in input data. Huffman coding assigns " +
            "shorter codes to more frequent symbols: a symbol with probability 0.5 gets a 1-bit code. " +
            "Huff0 compress: 1200-2500 MB/s on x86-64; HUF_BLOCKSIZE_MAX = 128 KB. ");

        [GlobalSetup]
        public void Setup()
        {
            data = new byte[Size];
            if (DataType == "LOW_ENTROPY")
            {
                for (int i = 0; i < Size; i++) data[i] = (byte)(i % 32);
            }
            else
            {
                for (int i = 0; i < Size; i++) data[i] = LoremBytes[i % LoremBytes.Length];
            }
        }

        [Benchmark]
        public byte[] Huff0Compress() => Huff0Interop.Compress(data);

        public static void Main(string[] args)
        {
            BenchmarkSwitcher.FromAssembly(typeof(CodecBenchmarks).Assembly).Run(args);
        }
    }
}
