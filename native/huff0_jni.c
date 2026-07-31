#include <stdlib.h>
#include <string.h>

#include "jni.h"
#include "huf.h"   // from FiniteStateEntropy/lib

// ---- Plain C API for C# and other callers ----
// These are thin wrappers around Huff0.

#ifdef _WIN32
  #define HUFF0_API __declspec(dllexport)
#else
  #define HUFF0_API __attribute__((visibility("default")))
#endif

// Compress: returns compressed size, or 0 on error.
// dst must be at least HUF_compressBound(srcSize).
HUFF0_API size_t huff0_compress(
    const void* src, size_t srcSize,
    void* dst, size_t dstCapacity)
{
    size_t cSize = HUF_compress(dst, dstCapacity, src, srcSize);
    if (HUF_isError(cSize)) {
        return 0;
    }
    return cSize;
}

// Decompress: returns decompressed size, or 0 on error.
HUFF0_API size_t huff0_decompress(
    const void* src, size_t srcSize,
    void* dst, size_t dstCapacity)
{
    size_t dSize = HUF_decompress(dst, dstCapacity, src, srcSize);
    if (HUF_isError(dSize)) {
        return 0;
    }
    return dSize;
}

// Helper: get upper bound for compressed size.
HUFF0_API size_t huff0_compress_bound(size_t srcSize)
{
    return HUF_compressBound(srcSize);
}

// ---- JNI wrappers for Java ----

JNIEXPORT jbyteArray JNICALL
Java_io_github_lhol_huff0_Huff0_compressNative(
    JNIEnv* env, jclass clazz, jbyteArray input)
{
    (void)clazz;

    jsize srcSize = (*env)->GetArrayLength(env, input);
    if (srcSize == 0) {
        return (*env)->NewByteArray(env, 0);
    }

    jbyte* srcBytes = (*env)->GetByteArrayElements(env, input, NULL);
    if (srcBytes == NULL) {
        return NULL; // OutOfMemoryError already thrown
    }

    size_t bound = huff0_compress_bound((size_t)srcSize);
    jbyteArray outArray = (*env)->NewByteArray(env, (jsize)bound);
    if (outArray == NULL) {
        (*env)->ReleaseByteArrayElements(env, input, srcBytes, JNI_ABORT);
        return NULL;
    }

    jbyte* outBytes = (*env)->GetByteArrayElements(env, outArray, NULL);
    if (outBytes == NULL) {
        (*env)->ReleaseByteArrayElements(env, input, srcBytes, JNI_ABORT);
        return NULL;
    }

    size_t cSize = huff0_compress(
        (const void*)srcBytes, (size_t)srcSize,
        (void*)outBytes, (size_t)bound);

    (*env)->ReleaseByteArrayElements(env, input, srcBytes, JNI_ABORT);

    if (cSize == 0) {
        (*env)->ReleaseByteArrayElements(env, outArray, outBytes, JNI_ABORT);
        return (*env)->NewByteArray(env, 0);
    }

    if (cSize < (size_t)bound) {
        /* Copy while outBytes is still valid, then discard the oversized array */
        jbyteArray resized = (*env)->NewByteArray(env, (jsize)cSize);
        if (resized == NULL) {
            (*env)->ReleaseByteArrayElements(env, outArray, outBytes, JNI_ABORT);
            return NULL;
        }
        (*env)->SetByteArrayRegion(env, resized, 0, (jsize)cSize, outBytes);
        (*env)->ReleaseByteArrayElements(env, outArray, outBytes, JNI_ABORT);
        return resized;
    }

    (*env)->ReleaseByteArrayElements(env, outArray, outBytes, 0);
    return outArray;
}

JNIEXPORT jbyteArray JNICALL
Java_io_github_lhol_huff0_Huff0_decompressNative(
    JNIEnv* env, jclass clazz, jbyteArray input, jint expectedDecompressedSize)
{
    (void)clazz;

    jsize srcSize = (*env)->GetArrayLength(env, input);
    if (srcSize == 0 || expectedDecompressedSize <= 0) {
        return (*env)->NewByteArray(env, 0);
    }

    jbyte* srcBytes = (*env)->GetByteArrayElements(env, input, NULL);
    if (srcBytes == NULL) {
        return NULL;
    }

    jbyteArray outArray = (*env)->NewByteArray(env, expectedDecompressedSize);
    if (outArray == NULL) {
        (*env)->ReleaseByteArrayElements(env, input, srcBytes, JNI_ABORT);
        return NULL;
    }

    jbyte* outBytes = (*env)->GetByteArrayElements(env, outArray, NULL);
    if (outBytes == NULL) {
        (*env)->ReleaseByteArrayElements(env, input, srcBytes, JNI_ABORT);
        return NULL;
    }

    size_t dSize = huff0_decompress(
        (const void*)srcBytes, (size_t)srcSize,
        (void*)outBytes, (size_t)expectedDecompressedSize);

    (*env)->ReleaseByteArrayElements(env, input, srcBytes, JNI_ABORT);

    if (dSize == 0 || dSize != (size_t)expectedDecompressedSize) {
        // Decompression error or size mismatch
        (*env)->ReleaseByteArrayElements(env, outArray, outBytes, JNI_ABORT);
        jbyteArray empty = (*env)->NewByteArray(env, 0);
        return empty;
    }

    (*env)->ReleaseByteArrayElements(env, outArray, outBytes, 0);
    return outArray;
}
