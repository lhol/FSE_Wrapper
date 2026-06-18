#include <stdlib.h>
#include <string.h>
#include "jni.h"
#include "fse.h"

#ifdef _WIN32
  #define FSE_API __declspec(dllexport)
#else
  #define FSE_API __attribute__((visibility("default")))
#endif

FSE_API size_t fse_compress_bound(size_t srcSize) {
    return FSE_compressBound(srcSize);
}

FSE_API size_t fse_compress(
    const void* src, size_t srcSize,
    void* dst, size_t dstCapacity)
{
    size_t cSize = FSE_compress(dst, dstCapacity, src, srcSize);
    return FSE_isError(cSize) ? 0 : cSize;
}

FSE_API size_t fse_decompress(
    const void* src, size_t srcSize,
    void* dst, size_t dstCapacity)
{
    size_t dSize = FSE_decompress(dst, dstCapacity, src, srcSize);
    return FSE_isError(dSize) ? 0 : dSize;
}

JNIEXPORT jbyteArray JNICALL
Java_com_karenta_fse_Fse_compressNative(
    JNIEnv* env, jclass clazz, jbyteArray input)
{
    (void)clazz;
    jsize srcSize = (*env)->GetArrayLength(env, input);
    if (srcSize == 0) return (*env)->NewByteArray(env, 0);

    jbyte* src = (*env)->GetByteArrayElements(env, input, NULL);
    size_t bound = fse_compress_bound(srcSize);

    jbyteArray outArr = (*env)->NewByteArray(env, bound);
    jbyte* out = (*env)->GetByteArrayElements(env, outArr, NULL);

    size_t cSize = fse_compress(src, srcSize, out, bound);
    (*env)->ReleaseByteArrayElements(env, input, src, JNI_ABORT);

    if (cSize == 0) {
        (*env)->ReleaseByteArrayElements(env, outArr, out, JNI_ABORT);
        return (*env)->NewByteArray(env, 0);
    }

    (*env)->ReleaseByteArrayElements(env, outArr, out, 0);

    if (cSize < bound) {
        jbyteArray resized = (*env)->NewByteArray(env, cSize);
        (*env)->SetByteArrayRegion(env, resized, 0, cSize, out);
        return resized;
    }
    return outArr;
}

JNIEXPORT jbyteArray JNICALL
Java_com_karenta_fse_Fse_decompressNative(
    JNIEnv* env, jclass clazz, jbyteArray input, jint expectedSize)
{
    (void)clazz;
    jsize srcSize = (*env)->GetArrayLength(env, input);
    if (srcSize == 0 || expectedSize <= 0)
        return (*env)->NewByteArray(env, 0);

    jbyte* src = (*env)->GetByteArrayElements(env, input, NULL);
    jbyteArray outArr = (*env)->NewByteArray(env, expectedSize);
    jbyte* out = (*env)->GetByteArrayElements(env, outArr, NULL);

    size_t dSize = fse_decompress(src, srcSize, out, expectedSize);
    (*env)->ReleaseByteArrayElements(env, input, src, JNI_ABORT);

    if (dSize == 0 || dSize != (size_t)expectedSize) {
        (*env)->ReleaseByteArrayElements(env, outArr, out, JNI_ABORT);
        return (*env)->NewByteArray(env, 0);
    }

    (*env)->ReleaseByteArrayElements(env, outArr, out, 0);
    return outArr;
}
