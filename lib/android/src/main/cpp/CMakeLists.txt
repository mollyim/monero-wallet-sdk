cmake_minimum_required(VERSION 3.22)

# Set the name of the final library
project(monero_jni NONE)

enable_language(C)
enable_language(CXX)
enable_language(ASM)

# Number of cores
cmake_host_system_information(RESULT NPROC QUERY NUMBER_OF_PHYSICAL_CORES)
set(CORES "${NPROC}"
    CACHE STRING "Number of available processor cores.")
mark_as_advanced(CORES)

message(STATUS "CMake version ${CMAKE_VERSION} with ${CORES} processor cores available")

# Location where external projects will be downloaded
set(DOWNLOAD_CACHE ""
    CACHE PATH "Location where external projects will be downloaded.")

# ABI-specific flags
if(ANDROID_ABI STREQUAL "x86_64")
  # Equivalent to CMAKE_INTERPROCEDURAL_OPTIMIZATION
  set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -maes -flto")
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -maes -flto")
else()
# TODO: message(FATAL_ERROR "Unknown ABI:" ${ANDROID_ABI})
  set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -flto")
  set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -flto")
endif()

# Prevent locale issues
set(ENV{LC_ALL} C)

# Common definitions across builds
include(cmake/toolchain.cmake)

# Project dependencies
add_subdirectory(boringssl)
add_subdirectory(boost)
add_subdirectory(libsodium)
add_subdirectory(unbound)
add_subdirectory(monero)

# Hide all symbols not marked with JNIEXPORT
set(CMAKE_C_VISIBILITY_PRESET hidden)
set(CMAKE_CXX_VISIBILITY_PRESET hidden)
set(CMAKE_VISIBILITY_INLINES_HIDDEN true)

# Project library
add_library(${PROJECT_NAME} SHARED)

target_sources(
    ${PROJECT_NAME}
    PUBLIC
      http_client.cc
      jni_cache.cc
      jni_loader.cc
      jvm.cc
      logging.cc
      wallet.cc
)

target_link_libraries(
    ${PROJECT_NAME}
    PRIVATE
      Monero::wallet_api
      log
)

# Hide symbols from statically-linked dependencies
set_target_properties(${PROJECT_NAME} PROPERTIES LINK_FLAGS "-Wl,--exclude-libs,ALL")
