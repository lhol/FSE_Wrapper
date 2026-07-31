using BenchmarkDotNet.Attributes;
using BenchmarkDotNet.Running;
using System.Text;

namespace Fse.Net.Benchmarks
{
    [MemoryDiagnoser]
    public class CodecBenchmarks
    {
        [Params(512, 1024, 4096, 16384, 65536, 262144, 1048576, 4194304, 16777216)]
        public int Size { get; set; }

        [Params("LOW_ENTROPY", "MEDIUM_ENTROPY")]
        public string DataType { get; set; } = "LOW_ENTROPY";

        private byte[] data = new byte[0];

        private static readonly byte[] LoremBytes = Encoding.UTF8.GetBytes(
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor " +
            "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud " +
            "exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure " +
            "dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. " +
            "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt " +
            "mollit anim id est laborum. ");

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
        public byte[] FseCompress() => FseInterop.Compress(data);

        public static void Main(string[] args)
        {
            BenchmarkSwitcher.FromAssembly(typeof(CodecBenchmarks).Assembly).Run(args);
        }
    }
}
