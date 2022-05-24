cmake_minimum_required(VERSION 3.4.1)
project(fuse C)

add_definitions(-D_FILE_OFFSET_BITS=64)
add_definitions(-DFUSERMOUNT_DIR=\"/system/bin\")
add_definitions(-DFUSE_USE_VERSION=35)

SET(FUSE_SOURCES
        libfuse/lib/fuse.c
        libfuse/lib/fuse_loop.c
        libfuse/lib/fuse_loop_mt.c
        libfuse/lib/fuse_lowlevel.c
        libfuse/lib/fuse_opt.c
        libfuse/lib/fuse_signals.c
        libfuse/lib/buffer.c
        libfuse/lib/cuse_lowlevel.c
        libfuse/lib/helper.c
        libfuse/lib/modules/subdir.c
        libfuse/lib/modules/iconv.c
        libfuse/lib/mount_util.c
        libfuse/lib/mount.c
        libfuse/lib/fuse_log.c)

set(FUSE_INCLUDES
        ${CMAKE_CURRENT_SOURCE_DIR}
        libfuse/lib
        libfuse/include)
set(CMAKE_SHARED_LINKER_FLAGS  "${CMAKE_SHARED_LINKER_FLAGS} -Wl,--version-script=${CMAKE_SOURCE_DIR}/libfuse/libfuse/lib/fuse_versionscript")
add_library(${PROJECT_NAME} SHARED ${FUSE_SOURCES})
target_include_directories(${PROJECT_NAME} PRIVATE ${FUSE_INCLUDES})
