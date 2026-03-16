cmake_minimum_required(VERSION 3.22)
project(pocketclaw_native)

set(CMAKE_CXX_STANDARD 17)

# Local paths (pre-downloaded, no network needed during build)
set(LLAMA_SRC_DIR "${CMAKE_SOURCE_DIR}/../../../../../libs/llama.cpp")
set(WHISPER_SRC_DIR "${CMAKE_SOURCE_DIR}/../../../../../libs/whisper.cpp")

# ─── llama.cpp (includes ggml) ───
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_CURL OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON ON CACHE BOOL "" FORCE)
add_subdirectory(${LLAMA_SRC_DIR} ${CMAKE_BINARY_DIR}/llama)

# ─── Llama JNI bridge ───
add_library(pocketclaw_llama SHARED llama_jni.cpp)
target_link_libraries(pocketclaw_llama PRIVATE llama common log)
target_include_directories(pocketclaw_llama PRIVATE
    ${LLAMA_SRC_DIR}/include
    ${LLAMA_SRC_DIR}/common
)

# ─── Whisper (compiled against llama.cpp's ggml to avoid version conflicts) ───
add_library(pocketclaw_whisper SHARED
    whisper_jni.cpp
    ${WHISPER_SRC_DIR}/src/whisper.cpp
)
target_include_directories(pocketclaw_whisper PRIVATE
    ${WHISPER_SRC_DIR}/include
    ${WHISPER_SRC_DIR}/src
    ${WHISPER_SRC_DIR}/ggml/include
)
target_link_libraries(pocketclaw_whisper PRIVATE ggml ggml-base ggml-cpu log)
target_compile_definitions(pocketclaw_whisper PRIVATE GGML_USE_CPU)
