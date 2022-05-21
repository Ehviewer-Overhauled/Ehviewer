project(ehviewer)
cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fvisibility=hidden -fvisibility-inlines-hidden -ffunction-sections -fdata-sections")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fvisibility=hidden -fvisibility-inlines-hidden -ffunction-sections -fdata-sections -fno-unwind-tables -fno-asynchronous-unwind-tables -fno-rtti -fno-stack-protector")

add_subdirectory(image)
add_subdirectory(integrity)

add_library(${PROJECT_NAME} SHARED main.c)

target_compile_options(${PROJECT_NAME} PUBLIC -fvisibility=hidden -fvisibility-inlines-hidden -fno-exceptions)

target_include_directories(${PROJECT_NAME} PUBLIC image/image integrity)

target_link_libraries(${PROJECT_NAME} image genuine)