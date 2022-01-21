cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "-std=c++14 -DANDROID -g")
set(CMAKE_C_FLAGS "-w -std=c11 -DANDROID -D_LARGEFILE_SOURCE=1 -g")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -ffunction-sections -fdata-sections")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -ffunction-sections -fdata-sections -fno-unwind-tables -fno-asynchronous-unwind-tables -fno-rtti -fno-stack-protector")

add_subdirectory(a7zip)
add_subdirectory(image)
add_subdirectory(integrity)

set(NATIVE_LIB "ehviewer")

add_library(${NATIVE_LIB} SHARED main.c)

target_compile_options(${NATIVE_LIB} PUBLIC -fvisibility=hidden -fvisibility-inlines-hidden -ffast-math -Oz -funroll-loops -ffast-math -fno-strict-aliasing -fno-math-errno)

target_include_directories(${NATIVE_LIB} PUBLIC
        a7zip/a7zip
        image/image
        integrity)

target_link_libraries(${NATIVE_LIB} -Wl,--no-whole-archive
        a7zip
        image
        genuine)
