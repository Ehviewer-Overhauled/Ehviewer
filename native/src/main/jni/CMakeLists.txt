project(ehviewer)
cmake_minimum_required(VERSION 3.4.1)

# Build liblzma as shared lib
set(BUILD_SHARED_LIBS ON)
add_subdirectory(liblzma)

# Build libzstd
add_subdirectory(libzstd/build/cmake)

# Configure libarchive for android platform
include_directories(libarchive/contrib/android/include)

# Configure lzma support for libarchive
include_directories(liblzma/src/liblzma/api)
SET(HAVE_LIBLZMA 1)
SET(HAVE_LZMA_H 1)
SET(HAVE_LZMA_STREAM_ENCODER_MT 1)
SET(HAVE_LZMADEC_H 1)
SET(HAVE_LIBLZMADEC 1)

# Configure zstd support for libarchive
include_directories(libzstd/lib)
SET(HAVE_LIBZSTD 1)
SET(HAVE_LIBZSTD_COMPRESSOR 1)
SET(HAVE_ZSTD_H 1)

# Build libarchive
add_subdirectory(libarchive)

# Build hippo's image lib
add_subdirectory(image)

# Build libfuse
add_subdirectory(libfuse)

# Build and link our app's native lib
add_library(${PROJECT_NAME} SHARED main.c archive.c)
target_include_directories(${PROJECT_NAME} PUBLIC image/image libarchive/libarchive)
target_link_libraries(${PROJECT_NAME} image archive fuse)
