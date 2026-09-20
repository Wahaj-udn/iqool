cmake_minimum_required(VERSION 3.22.1)

project("llama-android")

set(LLAMA_DIR "D:/Wahaj/AI/llama.cpp")

# Basic llama.cpp configuration for Android
set(LLAMA_BUILD_EXAMPLES OFF)
set(LLAMA_BUILD_TESTS OFF)
set(LLAMA_BUILD_SERVER OFF)

# Include the main llama.cpp project
add_subdirectory(${LLAMA_DIR} ${CMAKE_CURRENT_BINARY_DIR}/llama.cpp)

# Create our JNI library
add_library(llama-android SHARED
        llama-jni.cpp
)

# Link against llama.cpp's core library (which is called 'llama')
target_link_libraries(llama-android
        llama
        log # Android logging library
)

target_include_directories(llama-android PRIVATE
        ${LLAMA_DIR}/include
        ${LLAMA_DIR}/src
        ${LLAMA_DIR}/common
)
