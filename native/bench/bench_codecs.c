#include <stdio.h>
#include <string.h>
#include <time.h>
#include "huf.h"
#include "fse.h"

static const char* MSG = "The quick brown fox jumps over the lazy dog";
static const int ITER = 100000;

static double now_sec(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return ts.tv_sec + ts.tv_nsec / 1e9;
}

int main() {
    size_t srcSize = strlen(MSG);
    size_t hbound = HUF_compressBound(srcSize);
    size_t fbound = FSE_compressBound(srcSize);

    unsigned char* hbuf = malloc(hbound);
    unsigned char* fbuf = malloc(fbound);
    unsigned char* out  = malloc(srcSize);

    double t0, t1;

    t0 = now_sec();
    for (int i = 0; i < ITER; ++i) {
        size_t c = HUF_compress(hbuf, hbound, MSG, srcSize);
        (void)c;
    }
    t1 = now_sec();
    printf("Huff0 compress: %.3f MB/s\n",
           (double)srcSize * ITER / (1024.0*1024.0) / (t1 - t0));

    t0 = now_sec();
    for (int i = 0; i < ITER; ++i) {
        size_t c = FSE_compress(fbuf, fbound, MSG, srcSize);
        (void)c;
    }
    t1 = now_sec();
    printf("FSE compress:   %.3f MB/s\n",
           (double)srcSize * ITER / (1024.0*1024.0) / (t1 - t0));

    free(hbuf); free(fbuf); free(out);
    return 0;
}
