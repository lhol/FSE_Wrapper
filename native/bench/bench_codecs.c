#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "huf.h"
#include "fse.h"

static const int ITER = 50;   /* iterations per size; enough for stable timing */

/* Medium-entropy text repeated to fill buffers (>3.8 KB to avoid trivial periodicity) */
static const char* LOREM =
    "Data compression reduces the size of data by encoding information more efficiently. "
    "Lossless compression algorithms like Huffman coding, arithmetic coding, and Asymmetric "
    "Numeral Systems (ANS) exploit statistical redundancy in input data. Huffman coding assigns "
    "shorter codes to more frequent symbols: a symbol with probability 0.5 gets a 1-bit code, "
    "while a symbol with probability 0.0625 gets a 4-bit code. The theoretical limit is given "
    "by Shannon entropy: H(X) = -sum p(x) * log2(p(x)). For a uniform distribution over 256 "
    "symbols, H = 8 bits/symbol. For a biased source with 90% zeros and 10% ones, H ~= 0.469 "
    "bits/symbol. Modern compressors approach this limit closely. "
    "Finite State Entropy (FSE) is a tabled ANS implementation by Yann Collet (2013). "
    "It encodes a symbol stream using a finite state machine with 2^tableLog states, typically "
    "2048 (tableLog=11). Huff0 compress: 1200-2500 MB/s on x86-64; FSE compress: 800-1800 MB/s. "
    "Zstandard (zstd) uses both Huff0 (for literals) and FSE (for sequences and lengths). "
    "Common benchmark sizes: 512 B, 1 KB, 4 KB, 16 KB, 64 KB (Huff0 max: 128 KB). ";

#ifdef _WIN32
#  include <windows.h>
static double now_sec(void) {
    LARGE_INTEGER freq, cnt;
    QueryPerformanceFrequency(&freq);
    QueryPerformanceCounter(&cnt);
    return (double)cnt.QuadPart / (double)freq.QuadPart;
}
#else
#  include <time.h>
static double now_sec(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return ts.tv_sec + ts.tv_nsec / 1e9;
}
#endif

static void fill_low_entropy(unsigned char* buf, size_t n) {
    for (size_t i = 0; i < n; i++) buf[i] = (unsigned char)(i % 32);
}

static void fill_medium_entropy(unsigned char* buf, size_t n) {
    size_t lorLen = strlen(LOREM);
    for (size_t i = 0; i < n; i++) buf[i] = (unsigned char)LOREM[i % lorLen];
}

static const char* size_label(size_t n) {
    static char buf[32];
    if (n >= 1024*1024)      snprintf(buf, sizeof(buf), "%zuMB",  n/(1024*1024));
    else if (n >= 1024)      snprintf(buf, sizeof(buf), "%zuKB",  n/1024);
    else                     snprintf(buf, sizeof(buf), "%zuB",   n);
    return buf;
}

static void bench_codec(const char* name,
                        const char* datatype,
                        size_t srcSize,
                        unsigned char* src,
                        unsigned char* dst, size_t dstCap,
                        size_t (*compress_fn)(void*, size_t, const void*, size_t)) {
    /* Probe: check if codec supports this input size before timing.
       HUF_compress returns 0 for srcSize > 128 KB; measuring that gives
       astronomical "throughput" from near-zero elapsed time. */
    size_t probe = compress_fn(dst, dstCap, src, srcSize);
    if (probe == 0) {
        printf("%s compress [%s, %s]: N/A (incompressible or unsupported size)\n",
               name, size_label(srcSize), datatype);
        return;
    }

    /* warmup */
    for (int i = 0; i < 3; i++) compress_fn(dst, dstCap, src, srcSize);

    double t0 = now_sec();
    for (int i = 0; i < ITER; i++) compress_fn(dst, dstCap, src, srcSize);
    double t1 = now_sec();

    double mbps = (double)srcSize * ITER / (1024.0 * 1024.0) / (t1 - t0);
    printf("%s compress [%s, %s]: %.2f MB/s\n", name, size_label(srcSize), datatype, mbps);
}

static size_t huff0_wrap(void* dst, size_t dstCap, const void* src, size_t srcSize) {
    /* HUF_compress returns an error code (not 0) for srcSize > HUF_BLOCKSIZE_MAX.
       Convert any error or unsupported size to 0 so bench_codec prints N/A. */
    if (srcSize > HUF_BLOCKSIZE_MAX) return 0;
    size_t r = HUF_compress(dst, dstCap, src, srcSize);
    return HUF_isError(r) ? 0 : r;
}
static size_t fse_wrap(void* dst, size_t dstCap, const void* src, size_t srcSize) {
    size_t r = FSE_compress(dst, dstCap, src, srcSize);
    return FSE_isError(r) ? 0 : r;
}

int main(void) {
    static const size_t SIZES[] = {
        512, 1024, 4096, 16384, 65536, 262144, 1048576, 4194304, 16777216
    };
    static const int N_SIZES = (int)(sizeof(SIZES)/sizeof(SIZES[0]));

    size_t maxSize = SIZES[N_SIZES - 1];
    unsigned char* src = malloc(maxSize);
    unsigned char* dst = malloc(maxSize + 4096);  /* bound headroom */
    if (!src || !dst) { fprintf(stderr, "OOM\n"); return 1; }

    for (int s = 0; s < N_SIZES; s++) {
        size_t sz = SIZES[s];

        fill_low_entropy(src, sz);
        bench_codec("Huff0", "LOW_ENTROPY",    sz, src, dst, sz+4096, huff0_wrap);
        bench_codec("FSE",   "LOW_ENTROPY",    sz, src, dst, sz+4096, fse_wrap);

        fill_medium_entropy(src, sz);
        bench_codec("Huff0", "MEDIUM_ENTROPY", sz, src, dst, sz+4096, huff0_wrap);
        bench_codec("FSE",   "MEDIUM_ENTROPY", sz, src, dst, sz+4096, fse_wrap);
    }

    free(src); free(dst);
    return 0;
}

