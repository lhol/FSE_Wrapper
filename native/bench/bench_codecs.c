#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "huf.h"
#include "fse.h"

static const int ITER = 50;   /* iterations per size; enough for stable timing */

/* Medium-entropy text repeated to fill buffers */
static const char* LOREM =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor "
    "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud "
    "exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure "
    "dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. "
    "Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt "
    "mollit anim id est laborum. ";

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
    /* warmup */
    for (int i = 0; i < 3; i++) compress_fn(dst, dstCap, src, srcSize);

    double t0 = now_sec();
    for (int i = 0; i < ITER; i++) compress_fn(dst, dstCap, src, srcSize);
    double t1 = now_sec();

    double mbps = (double)srcSize * ITER / (1024.0 * 1024.0) / (t1 - t0);
    printf("%s compress [%s, %s]: %.2f MB/s\n", name, size_label(srcSize), datatype, mbps);
}

static size_t huff0_wrap(void* dst, size_t dstCap, const void* src, size_t srcSize) {
    return HUF_compress(dst, dstCap, src, srcSize);
}
static size_t fse_wrap(void* dst, size_t dstCap, const void* src, size_t srcSize) {
    return FSE_compress(dst, dstCap, src, srcSize);
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

