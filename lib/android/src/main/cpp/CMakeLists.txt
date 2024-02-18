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

### Project libraries

set(COMMON_SOURCES
    common/jvm.cc
    common/java_native.cc
)

set(WALLET_SOURCES
    wallet/http_client.cc
    wallet/jni_cache.cc
    wallet/jni_loader.cc
    wallet/logging.cc
    wallet/wallet.cc
)

add_library(monero_wallet SHARED ${COMMON_SOURCES} ${WALLET_SOURCES})

target_link_libraries(
    monero_wallet
    PRIVATE
      Monero::wallet2
      log
)

set(MNEMONICS_SOURCES
    mnemonics/jni_cache.cc
    mnemonics/jni_loader.cc
    mnemonics/mnemonics.cc
)

add_library(monero_mnemonics SHARED ${COMMON_SOURCES} ${MNEMONICS_SOURCES})

target_link_libraries(
    monero_mnemonics
    PUBLIC
      Monero::easylogging
    PRIVATE
      Monero::electrum_words
      OpenSSL::SSL
      log
)

target_include_directories(monero_wallet PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}")
target_include_directories(monero_mnemonics PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}")

# Hide symbols from statically-linked dependencies
set_target_properties(monero_wallet PROPERTIES LINK_FLAGS "-Wl,--exclude-libs,ALL")
set_target_properties(monero_mnemonics PROPERTIES LINK_FLAGS "-Wl,--exclude-libs,ALL")
