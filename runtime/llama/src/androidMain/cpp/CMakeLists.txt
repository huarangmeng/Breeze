cmake_minimum_required(VERSION 3.22.1)

project("breeze_llama_android" LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(BREEZE_LLAMA_CPP_SOURCE_DIR "" CACHE PATH "Path to the vendored llama.cpp source directory")

if(NOT EXISTS "${BREEZE_LLAMA_CPP_SOURCE_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "BREEZE_LLAMA_CPP_SOURCE_DIR must point to the vendored llama.cpp root")
endif()

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(GGML_BACKEND_DL OFF CACHE BOOL "" FORCE)
set(GGML_METAL OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_OPENCL OFF CACHE BOOL "" FORCE)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)
set(GGML_HIP OFF CACHE BOOL "" FORCE)
set(GGML_SYCL OFF CACHE BOOL "" FORCE)

add_subdirectory("${BREEZE_LLAMA_CPP_SOURCE_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/llama.cpp")

add_library(breeze_llama_jni SHARED
    ../../jvmMain/cpp/breeze_llama_jni.cpp
)

target_include_directories(breeze_llama_jni PRIVATE
    "${BREEZE_LLAMA_CPP_SOURCE_DIR}"
    "${BREEZE_LLAMA_CPP_SOURCE_DIR}/include"
    "${BREEZE_LLAMA_CPP_SOURCE_DIR}/ggml/include"
)

target_link_libraries(breeze_llama_jni PRIVATE
    llama
    log
    android
)
