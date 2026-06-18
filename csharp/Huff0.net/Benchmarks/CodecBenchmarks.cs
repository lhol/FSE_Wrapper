using BenchmarkDotNet.Attributes;
using BenchmarkDotNet.Running;
using System.Text;

namespace Huff0.Net.Benchmarks
{
    public class CodecBenchmarks
    {
        private byte[] data;

        [GlobalSetup]
        public void Setup()
        {
            data = Encoding.UTF8.GetBytes("The quick brown fox jumps over the lazy dog");
        }

        [Benchmark]
        public byte[] Huff0Compress() => Huff0Interop.Compress(data);

        public static void Main(string[] args)
        {
            BenchmarkRunner.Run<CodecBenchmarks>();
        }
    }
}
