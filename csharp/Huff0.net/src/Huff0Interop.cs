using System;
using System.Runtime.InteropServices;

namespace Huff0.Net
{
    public static class Huff0Interop
    {
        // .NET resolves the name per platform automatically:
        //   Windows → huff0.dll  |  Linux → libhuff0.so  |  macOS → libhuff0.dylib
        private const string LIB = "huff0";

        /// <summary>
        /// Minimum number of bytes that <see cref="Compress"/> will accept.
        /// Reflects the hard limit in HUF_compress() (srcSize &lt; 12 → returns 0).
        /// </summary>
        public const int MinCompressSize = 12;

        [DllImport(LIB, CallingConvention = CallingConvention.Cdecl)]
        private static extern UIntPtr huff0_compress_bound(UIntPtr srcSize);

        [DllImport(LIB, CallingConvention = CallingConvention.Cdecl)]
        private static extern UIntPtr huff0_compress(
            byte[] src, UIntPtr srcSize,
            byte[] dst, UIntPtr dstCapacity);

        [DllImport(LIB, CallingConvention = CallingConvention.Cdecl)]
        private static extern UIntPtr huff0_decompress(
            byte[] src, UIntPtr srcSize,
            byte[] dst, UIntPtr dstCapacity);

        /// <summary>
        /// Compresses <paramref name="input"/> using the Huff0 entropy coder.
        /// </summary>
        /// <param name="input">Raw bytes to compress. Must be at least <see cref="MinCompressSize"/> bytes.</param>
        /// <returns>Compressed bytes, always shorter than <paramref name="input"/> for compressible data.</returns>
        /// <exception cref="ArgumentNullException">If <paramref name="input"/> is null.</exception>
        /// <exception cref="ArgumentException">If input is shorter than <see cref="MinCompressSize"/> bytes.</exception>
        /// <exception cref="InvalidOperationException">If the data is not compressible by Huff0
        /// (e.g. random, encrypted, or already-compressed data).</exception>
        public static byte[] Compress(byte[] input)
        {
            if (input == null) throw new ArgumentNullException(nameof(input));
            if (input.Length < MinCompressSize)
                throw new ArgumentException(
                    $"input too small for Huff0 compression: {input.Length} bytes " +
                    $"(minimum: {MinCompressSize} bytes)", nameof(input));

            UIntPtr bound = huff0_compress_bound((UIntPtr)input.Length);
            byte[] dst = new byte[(ulong)bound];
            UIntPtr cSize = huff0_compress(input, (UIntPtr)input.Length, dst, bound);
            if ((ulong)cSize == 0)
                throw new InvalidOperationException(
                    $"Huff0 compression produced no output: input ({input.Length} bytes) " +
                    "may not be compressible (random, encrypted, or uniform data)");

            byte[] result = new byte[(ulong)cSize];
            Array.Copy(dst, result, (long)cSize);
            return result;
        }

        /// <summary>
        /// Decompresses <paramref name="compressed"/> bytes previously produced by <see cref="Compress"/>.
        /// </summary>
        /// <param name="compressed">Huff0-compressed bytes.</param>
        /// <param name="expectedSize">Exact length of the original uncompressed data.</param>
        /// <returns>The restored original bytes.</returns>
        /// <exception cref="ArgumentNullException">If <paramref name="compressed"/> is null.</exception>
        /// <exception cref="ArgumentOutOfRangeException">If <paramref name="expectedSize"/> is not positive.</exception>
        /// <exception cref="InvalidOperationException">If decompression fails (corrupt input or wrong size).</exception>
        public static byte[] Decompress(byte[] compressed, int expectedSize)
        {
            if (compressed == null) throw new ArgumentNullException(nameof(compressed));
            if (expectedSize <= 0)
                throw new ArgumentOutOfRangeException(nameof(expectedSize), "expectedSize must be > 0");

            byte[] dst = new byte[expectedSize];
            UIntPtr dSize = huff0_decompress(
                compressed, (UIntPtr)compressed.Length,
                dst, (UIntPtr)expectedSize);
            if ((ulong)dSize != (ulong)expectedSize)
                throw new InvalidOperationException(
                    $"Huff0 decompression failed: corrupt input or wrong expectedSize ({expectedSize})");
            return dst;
        }
    }
}
