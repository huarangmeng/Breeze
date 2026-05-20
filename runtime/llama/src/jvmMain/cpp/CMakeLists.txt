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
set(BREEZE_LLAMA_GPU_BACKEND "auto" CACHE STRING "GPU backend for Breeze's Desktop llama runtime: auto, cpu, metal, vulkan, cuda, hip, sycl, or opencl.")
set_property(CACHE BREEZE_LLAMA_GPU_BACKEND PROPERTY STRINGS auto cpu metal vulkan cuda hip sycl opencl)

string(TOLOWER "${BREEZE_LLAMA_GPU_BACKEND}" BREEZE_LLAMA_GPU_BACKEND_NORMALIZED)
if(BREEZE_LLAMA_GPU_BACKEND_NORMALIZED STREQUAL "auto")
    if(APPLE)
        set(BREEZE_LLAMA_GPU_BACKEND_NORMALIZED "metal")
    elseif(WIN32 OR UNIX)
        set(BREEZE_LLAMA_GPU_BACKEND_NORMALIZED "vulkan")
    else()
        set(BREEZE_LLAMA_GPU_BACKEND_NORMALIZED "cpu")
    endif()
endif()

if(BREEZE_LLAMA_GPU_BACKEND_NORMALIZED STREQUAL "cpu")
    message(STATUS "Breeze llama GPU backend: cpu")
elseif(BREEZE_LLAMA_GPU_BACKEND_NORMALIZED STREQUAL "metal")
    set(GGML_METAL ON CACHE BOOL "" FORCE)
    message(STATUS "Breeze llama GPU backend: metal")
elseif(BREEZE_LLAMA_GPU_BACKEND_NORMALIZED STREQUAL "vulkan")
    set(GGML_VULKAN ON CACHE BOOL "" FORCE)
    message(STATUS "Breeze llama GPU backend: vulkan")
elseif(BREEZE_LLAMA_GPU_BACKEND_NORMALIZED STREQUAL "cuda")
    set(GGML_CUDA ON CACHE BOOL "" FORCE)
    message(STATUS "Breeze llama GPU backend: cuda")
elseif(BREEZE_LLAMA_GPU_BACKEND_NORMALIZED STREQUAL "hip")
    set(GGML_HIP ON CACHE BOOL "" FORCE)
    message(STATUS "Breeze llama GPU backend: hip")
elseif(BREEZE_LLAMA_GPU_BACKEND_NORMALIZED STREQUAL "sycl")
    set(GGML_SYCL ON CACHE BOOL "" FORCE)
    message(STATUS "Breeze llama GPU backend: sycl")
elseif(BREEZE_LLAMA_GPU_BACKEND_NORMALIZED STREQUAL "opencl")
    set(GGML_OPENCL ON CACHE BOOL "" FORCE)
    message(STATUS "Breeze llama GPU backend: opencl")
else()
    message(FATAL_ERROR "Unsupported BREEZE_LLAMA_GPU_BACKEND: ${BREEZE_LLAMA_GPU_BACKEND}")
endif()

if(NOT BREEZE_LLAMA_CPP_SOURCE_DIR)
    message(FATAL_ERROR "BREEZE_LLAMA_CPP_SOURCE_DIR must point to the pinned llama.cpp submodule checkout.")
endif()

if(NOT EXISTS "${BREEZE_LLAMA_CPP_SOURCE_DIR}/CMakeLists.txt")
    message(FATAL_ERROR "Pinned llama.cpp submodule is missing: ${BREEZE_LLAMA_CPP_SOURCE_DIR}")
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
