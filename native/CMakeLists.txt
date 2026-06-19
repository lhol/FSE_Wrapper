cmake_minimum_required(VERSION 3.10)
project(codecs C)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if (WIN32)
    message(STATUS "Building on Windows. Ensure you have a JDK installed and JAVA_HOME environment variable set.")
endif()
if (WIN32 AND NOT DEFINED ENV{JAVA_HOME})
    message(FATAL_ERROR "JAVA_HOME is not set. Please set JAVA_HOME to your JDK.")
endif()

# Set JNI include directories
if (WIN32)
    # Strip quotes from JAVA_HOME (it may be quoted on Windows)
    set(JAVA_HOME_PATH $ENV{JAVA_HOME})
    string(REGEX REPLACE "^\"+|\"+$" "" JAVA_HOME_PATH "${JAVA_HOME_PATH}")
        set(JAVA_AWT_LIBRARY NotNeeded)
        set(JAVA_AWT_INCLUDE_PATH NotNeeded)
        set(JAVA_JVM_LIBRARY NotNeeded)
    set(JNI_INCLUDE_PATH ${JAVA_HOME_PATH}/include)
    set(JNI_INCLUDE_PATH_WINDOWS ${JAVA_HOME_PATH}/include/win32)
    include_directories(${JNI_INCLUDE_PATH} ${JNI_INCLUDE_PATH_WINDOWS})
else()
    find_package(JNI REQUIRED)
    include_directories(${JNI_INCLUDE_DIRS})
endif()

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/FiniteStateEntropy/lib)

# Huff0 sources (Cyan4973)
set(HUFF0_SOURCES
    ${CMAKE_CURRENT_SOURCE_DIR}/FiniteStateEntropy/lib/huf_compress.c
    ${CMAKE_CURRENT_SOURCE_DIR}/FiniteStateEntropy/lib/huf_decompress.c
)

# FSE sources (Cyan4973)
set(FSE_SOURCES
    ${CMAKE_CURRENT_SOURCE_DIR}/FiniteStateEntropy/lib/fse_compress.c
    ${CMAKE_CURRENT_SOURCE_DIR}/FiniteStateEntropy/lib/fse_decompress.c
    ${CMAKE_CURRENT_SOURCE_DIR}/FiniteStateEntropy/lib/entropy_common.c
    ${CMAKE_CURRENT_SOURCE_DIR}/FiniteStateEntropy/lib/hist.c
)

add_library(huff0 SHARED ${CMAKE_CURRENT_SOURCE_DIR}/huff0_jni.c ${HUFF0_SOURCES} ${FSE_SOURCES})
add_library(fse   SHARED ${CMAKE_CURRENT_SOURCE_DIR}/fse_jni.c   ${FSE_SOURCES})

target_include_directories(huff0 PRIVATE ${JNI_INCLUDE_DIRS} ${CMAKE_CURRENT_SOURCE_DIR}/FiniteStateEntropy/lib)
target_include_directories(fse PRIVATE ${JNI_INCLUDE_DIRS} ${CMAKE_CURRENT_SOURCE_DIR}/FiniteStateEntropy/lib)

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
