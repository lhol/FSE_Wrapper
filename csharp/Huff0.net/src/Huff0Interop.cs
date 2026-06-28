using System;
using System.Runtime.InteropServices;

namespace Huff0.Net
{
    public static class Huff0Interop
    {
        // .NET resolves the name per platform automatically:
        //   Windows → huff0.dll  |  Linux → libhuff0.so  |  macOS → libhuff0.dylib
        private const string LIB = "huff0";

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

        public static byte[] Compress(byte[] input)
        {
            UIntPtr bound = huff0_compress_bound((UIntPtr)input.Length);
            byte[] dst = new byte[(ulong)bound];
            UIntPtr cSize = huff0_compress(input, (UIntPtr)input.Length, dst, bound);
            if ((ulong)cSize == 0) throw new Exception("Huff0 compression failed");
            byte[] result = new byte[(ulong)cSize];
            Array.Copy(dst, result, (long)cSize);
            return result;
        }

        public static byte[] Decompress(byte[] compressed, int expectedSize)
        {
            byte[] dst = new byte[expectedSize];
            UIntPtr dSize = huff0_decompress(
                compressed, (UIntPtr)compressed.Length,
                dst, (UIntPtr)expectedSize);
            if ((ulong)dSize != (ulong)expectedSize) throw new Exception("Huff0 decompression failed");
            return dst;
        }
    }
}
