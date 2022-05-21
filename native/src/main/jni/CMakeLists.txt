project(ehviewer)
cmake_minimum_required(VERSION 3.4.1)

add_subdirectory(image)

add_library(${PROJECT_NAME} SHARED main.c)

target_include_directories(${PROJECT_NAME} PUBLIC image/image)

target_link_libraries(${PROJECT_NAME} image)