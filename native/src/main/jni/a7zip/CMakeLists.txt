cmake_minimum_required(VERSION 3.4.1)

project(a7zip C CXX)

add_subdirectory(p7zip)

set(A_SEVEN_ZIP_SOURCES
        a7zip/BlackHole.cpp
        a7zip/InArchive.cpp
        a7zip/JavaEnv.cpp
        a7zip/JavaHelper.cpp
        a7zip/JavaInArchive.cpp
        a7zip/JavaInitA7Zip.cpp
        a7zip/JavaInputStream.cpp
        a7zip/JavaSeekableInputStream.cpp
        a7zip/OpenVolumeCallback.cpp
        a7zip/OutputStream.cpp
        a7zip/SeekableInputStream.cpp
        a7zip/SevenZip.cpp
        )


add_library(a7zip STATIC ${A_SEVEN_ZIP_SOURCES})
target_link_libraries(a7zip PUBLIC p7zip log)
target_compile_options(a7zip PRIVATE -fvisibility=hidden -fvisibility-inlines-hidden -fno-exceptions)
