project(ehviewer)
cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Ofast -fvisibility=hidden -fvisibility-inlines-hidden -ffunction-sections -fdata-sections -funroll-loops -flto=thin \
           -mllvm -polly \
           -mllvm -polly-run-dce \
           -mllvm -polly-run-inliner \
           -mllvm -polly-isl-arg=--no-schedule-serialize-sccs \
           -mllvm -polly-ast-use-context \
           -mllvm -polly-detect-keep-going \
           -mllvm -polly-position=before-vectorizer \
           -mllvm -polly-vectorizer=stripmine \
           -mllvm -polly-detect-profitability-min-per-loop-insts=40 \
           -mllvm -polly-invariant-load-hoisting")

# Build GNUTLS libnettle
add_subdirectory(nettle)

# Configure libnettle support for libarchive
include_directories(nettle)
include_directories(.)
set(HAVE_LIBNETTLE 1)
set(HAVE_NETTLE_AES_H 1)
set(HAVE_NETTLE_HMAC_H 1)
set(HAVE_NETTLE_MD5_H 1)
set(HAVE_NETTLE_PBKDF2_H 1)
set(HAVE_NETTLE_RIPEMD160_H 1)
set(HAVE_NETTLE_SHA_H 1)

# Build liblzma
add_subdirectory(liblzma)

# Configure lzma support for libarchive
include_directories(liblzma/src/liblzma/api)
SET(HAVE_LIBLZMA 1)
SET(HAVE_LZMA_H 1)
SET(HAVE_LZMA_STREAM_ENCODER_MT 1)
SET(HAVE_LZMADEC_H 1)
SET(HAVE_LIBLZMADEC 1)

# Configure libarchive link's static lib
SET(LIBARCHIVE_CUSTOM_LIBS "liblzma" "nettle")

# Build libarchive
add_subdirectory(libarchive)

# Build and link our app's native lib
add_library(${PROJECT_NAME} SHARED archive.c fdutils.c)
target_include_directories(${PROJECT_NAME} PUBLIC libarchive/libarchive)
target_link_libraries(${PROJECT_NAME} archive_static log -Wl,--gc-sections)
