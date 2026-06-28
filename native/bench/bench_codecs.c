#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "huf.h"
#include "fse.h"

static const char* SRC = "The quick brown fox jumps over the lazy dog";
static const int ITER = 100000;

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

int main() {
    size_t srcSize = strlen(SRC);
    size_t hbound = HUF_compressBound(srcSize);
    size_t fbound = FSE_compressBound(srcSize);

    unsigned char* hbuf = malloc(hbound);
    unsigned char* fbuf = malloc(fbound);
    unsigned char* out  = malloc(srcSize);

    double t0, t1;

    t0 = now_sec();
    for (int i = 0; i < ITER; ++i) {
        size_t c = HUF_compress(hbuf, hbound, SRC, srcSize);
        (void)c;
    }
    t1 = now_sec();
    printf("Huff0 compress: %.3f MB/s\n",
           (double)srcSize * ITER / (1024.0*1024.0) / (t1 - t0));

    t0 = now_sec();
    for (int i = 0; i < ITER; ++i) {
        size_t c = FSE_compress(fbuf, fbound, SRC, srcSize);
        (void)c;
    }
    t1 = now_sec();
    printf("FSE compress:   %.3f MB/s\n",
           (double)srcSize * ITER / (1024.0*1024.0) / (t1 - t0));

    free(hbuf); free(fbuf); free(out);
    return 0;
}
