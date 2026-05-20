cmake_minimum_required(VERSION 3.22)

project(breeze_llama_jni LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_BLAS OFF CACHE BOOL "" FORCE)
set(GGML_CANN OFF CACHE BOOL "" FORCE)
set(GGML_CUDA OFF CACHE BOOL "" FORCE)
set(GGML_HIP OFF CACHE BOOL "" FORCE)
set(GGML_METAL OFF CACHE BOOL "" FORCE)
set(GGML_MUSA OFF CACHE BOOL "" FORCE)
set(GGML_RPC OFF CACHE BOOL "" FORCE)
set(GGML_SYCL OFF CACHE BOOL "" FORCE)
set(GGML_VULKAN OFF CACHE BOOL "" FORCE)
set(GGML_WEBGPU OFF CACHE BOOL "" FORCE)
set(GGML_ZDNN OFF CACHE BOOL "" FORCE)
set(GGML_VIRTGPU OFF CACHE BOOL "" FORCE)
set(GGML_OPENCL OFF CACHE BOOL "" FORCE)
set(GGML_HEXAGON OFF CACHE BOOL "" FORCE)
set(GGML_ZENDNN OFF CACHE BOOL "" FORCE)
set(GGML_OPENVINO OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(BREEZE_LLAMA_CPP_SOURCE_DIR "" CACHE PATH "Optional local llama.cpp source directory")

if(NOT BREEZE_LLAMA_CPP_SOURCE_DIR)
    message(FATAL_ERROR "BREEZE_LLAMA_CPP_SOURCE_DIR must point to the vendored llama.cpp source directory.")
endif()

if(NOT EXISTS "${BREEZE_LLAMA_CPP_SOURCE_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "Vendored llama.cpp source is missing: ${BREEZE_LLAMA_CPP_SOURCE_DIR}")
endif()

add_subdirectory(${BREEZE_LLAMA_CPP_SOURCE_DIR} ${CMAKE_BINARY_DIR}/llama.cpp)

find_package(JNI REQUIRED)

add_library(breeze_llama_jni SHARED
    breeze_llama_jni.cpp
)

target_include_directories(breeze_llama_jni
    PRIVATE
        ${JNI_INCLUDE_DIRS}
        ${BREEZE_LLAMA_CPP_SOURCE_DIR}/include
        ${BREEZE_LLAMA_CPP_SOURCE_DIR}/ggml/include
)

target_link_libraries(breeze_llama_jni
    PRIVATE
        llama
        JNI::JNI
)

if(APPLE)
    set_target_properties(breeze_llama_jni PROPERTIES
        MACOSX_RPATH ON
        INSTALL_RPATH "@loader_path"
    )
endif()
