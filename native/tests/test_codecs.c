#include <stdio.h>
#include <string.h>
#include <assert.h>

#include "huf.h"
#include "fse.h"

static const char* TEST_STRING = "The quick brown fox jumps over the lazy dog";

void test_huff0() {
    size_t srcSize = strlen(TEST_STRING);
    size_t bound = HUF_compressBound(srcSize);

    unsigned char* compressed = malloc(bound);
    unsigned char* decompressed = malloc(srcSize);

    size_t cSize = HUF_compress(compressed, bound, TEST_STRING, srcSize);
    assert(!HUF_isError(cSize));

    size_t dSize = HUF_decompress(decompressed, srcSize, compressed, cSize);
    assert(!HUF_isError(dSize));
    assert(dSize == srcSize);
    assert(memcmp(TEST_STRING, decompressed, srcSize) == 0);

    free(compressed);
    free(decompressed);
}

void test_fse() {
    size_t srcSize = strlen(TEST_STRING);
    size_t bound = FSE_compressBound(srcSize);

    unsigned char* compressed = malloc(bound);
    unsigned char* decompressed = malloc(srcSize);

    size_t cSize = FSE_compress(compressed, bound, TEST_STRING, srcSize);
    assert(!FSE_isError(cSize));

    size_t dSize = FSE_decompress(decompressed, srcSize, compressed, cSize);
    assert(!FSE_isError(dSize));
    assert(dSize == srcSize);
    assert(memcmp(TEST_STRING, decompressed, srcSize) == 0);

    free(compressed);
    free(decompressed);
}

int main() {
    test_huff0();
    test_fse();
    printf("Native tests passed\n");
    return 0;
}
