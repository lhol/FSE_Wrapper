using System;
using System.Runtime.InteropServices;

namespace Fse.Net
{
    public static class FseInterop
    {
        // .NET resolves the name per platform automatically:
        //   Windows → fse.dll  |  Linux → libfse.so  |  macOS → libfse.dylib
        private const string LIB = "fse";

        [DllImport(LIB, CallingConvention = CallingConvention.Cdecl)]
        private static extern UIntPtr fse_compress_bound(UIntPtr srcSize);

        [DllImport(LIB, CallingConvention = CallingConvention.Cdecl)]
        private static extern UIntPtr fse_compress(
            byte[] src, UIntPtr srcSize,
            byte[] dst, UIntPtr dstCapacity);

        [DllImport(LIB, CallingConvention = CallingConvention.Cdecl)]
        private static extern UIntPtr fse_decompress(
            byte[] src, UIntPtr srcSize,
            byte[] dst, UIntPtr dstCapacity);

        public static byte[] Compress(byte[] input)
        {
            UIntPtr bound = fse_compress_bound((UIntPtr)input.Length);
            byte[] dst = new byte[(ulong)bound];
            UIntPtr cSize = fse_compress(input, (UIntPtr)input.Length, dst, bound);
            if ((ulong)cSize == 0) throw new Exception("FSE compression failed");
            byte[] result = new byte[(ulong)cSize];
            Array.Copy(dst, result, (long)cSize);
            return result;
        }

        public static byte[] Decompress(byte[] compressed, int expectedSize)
        {
            byte[] dst = new byte[expectedSize];
            UIntPtr dSize = fse_decompress(
                compressed, (UIntPtr)compressed.Length,
                dst, (UIntPtr)expectedSize);
            if ((ulong)dSize != (ulong)expectedSize) throw new Exception("FSE decompression failed");
            return dst;
        }
    }
}
