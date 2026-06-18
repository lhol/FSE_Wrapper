cmake_minimum_required(VERSION 3.10)
project(codecs C)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/FiniteStateEntropy/lib)

# Huff0 sources (Cyan4973)
set(HUFF0_SOURCES
    FiniteStateEntropy/lib/huf_compress.c
    FiniteStateEntropy/lib/huf_decompress.c
    FiniteStateEntropy/lib/huf_tables.c
)

# FSE sources (Cyan4973)
set(FSE_SOURCES
    FiniteStateEntropy/lib/fse_compress.c
    FiniteStateEntropy/lib/fse_decompress.c
    FiniteStateEntropy/lib/fse_tables.c
)

add_library(huff0 SHARED huff0_jni.c ${HUFF0_SOURCES})
add_library(fse   SHARED fse_jni.c   ${FSE_SOURCES})

if (WIN32)
    set_target_properties(huff0 PROPERTIES WINDOWS_EXPORT_ALL_SYMBOLS ON)
    set_target_properties(fse   PROPERTIES WINDOWS_EXPORT_ALL_SYMBOLS ON)
endif()

enable_testing()
add_executable(test_codecs tests/test_codecs.c)
target_link_libraries(test_codecs huff0 fse)
add_test(NAME native_tests COMMAND test_codecs)

add_executable(bench_codecs bench/bench_codecs.c)
target_link_libraries(bench_codecs huff0 fse)


# Library name will be:
#   Linux: libhuff0_jni.so
#   macOS: libhuff0_jni.dylib
#   Windows: huff0_jni.dll
