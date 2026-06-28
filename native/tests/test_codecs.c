#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include "huf.h"
#include "fse.h"

/* 512 bytes of repeating 0-31 pattern — always compressible by Huff0 and FSE */
static void fill_test_data(unsigned char* buf, size_t size) {
    for (size_t i = 0; i < size; i++)
        buf[i] = (unsigned char)(i % 32);
}

#define DATA_SIZE 512

void test_huff0(void) {
    unsigned char src[DATA_SIZE];
    fill_test_data(src, DATA_SIZE);

    size_t bound = HUF_compressBound(DATA_SIZE);
    unsigned char* compressed   = (unsigned char*)malloc(bound);
    unsigned char* decompressed = (unsigned char*)malloc(DATA_SIZE);
    assert(compressed && decompressed);

    size_t cSize = HUF_compress(compressed, bound, src, DATA_SIZE);
    assert(!HUF_isError(cSize));
    assert(cSize > 0); /* data must have compressed */

    size_t dSize = HUF_decompress(decompressed, DATA_SIZE, compressed, cSize);
    assert(!HUF_isError(dSize));
    assert(dSize == DATA_SIZE);
    assert(memcmp(src, decompressed, DATA_SIZE) == 0);

    free(compressed);
    free(decompressed);
}

void test_fse(void) {
    unsigned char src[DATA_SIZE];
    fill_test_data(src, DATA_SIZE);

    size_t bound = FSE_compressBound(DATA_SIZE);
    unsigned char* compressed   = (unsigned char*)malloc(bound);
    unsigned char* decompressed = (unsigned char*)malloc(DATA_SIZE);
    assert(compressed && decompressed);

    size_t cSize = FSE_compress(compressed, bound, src, DATA_SIZE);
    assert(!FSE_isError(cSize));
    assert(cSize > 1); /* 1 == FSE literal-mode (all-same-byte); >1 means compressed */

    size_t dSize = FSE_decompress(decompressed, DATA_SIZE, compressed, cSize);
    assert(!FSE_isError(dSize));
    assert(dSize == DATA_SIZE);
    assert(memcmp(src, decompressed, DATA_SIZE) == 0);

    free(compressed);
    free(decompressed);
}

int main(void) {
    test_huff0();
    test_fse();
    printf("Native tests passed\n");
    return 0;
}
