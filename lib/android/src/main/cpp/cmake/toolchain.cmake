# Definitions for Android NDK r23 single clang-toolchain

set(TOOLCHAIN "${CMAKE_ANDROID_NDK_TOOLCHAIN_UNIFIED}")
set(PREBUILTS "${CMAKE_ANDROID_NDK}/prebuilt/${CMAKE_ANDROID_NDK_TOOLCHAIN_HOST_TAG}")
set(SYSROOT   "${CMAKE_SYSROOT}")

set(TOOLCHAIN_BIN "${TOOLCHAIN}/bin")
set(PREBUILTS_BIN "${PREBUILTS}/bin")

set(TARGET_HOST ${CMAKE_ANDROID_ARCH_TRIPLE})
set(TARGET_API  ${CMAKE_SYSTEM_VERSION})

set(CLANG_CC  ${TARGET_HOST}${TARGET_API}-clang)
set(CLANG_AS  ${TARGET_HOST}${TARGET_API}-clang)
set(CLANG_CXX ${TARGET_HOST}${TARGET_API}-clang++)

# https://developer.android.com/ndk/guides/other_build_systems#overview
if(ANDROID_ABI STREQUAL "armeabi-v7a")
  string(REPLACE "arm" "armv7a" CLANG_CC  "${CLANG_CC}")
  string(REPLACE "arm" "armv7a" CLANG_AS  "${CLANG_AS}")
  string(REPLACE "arm" "armv7a" CLANG_CXX "${CLANG_CXX}")
endif()

find_program(NDK_AR     llvm-ar      PATHS "${TOOLCHAIN_BIN}" NO_DEFAULT_PATH REQUIRED)
find_program(NDK_CC     ${CLANG_CC}  PATHS "${TOOLCHAIN_BIN}" NO_DEFAULT_PATH REQUIRED)
find_program(NDK_AS     ${CLANG_AS}  PATHS "${TOOLCHAIN_BIN}" NO_DEFAULT_PATH REQUIRED)
find_program(NDK_CXX    ${CLANG_CXX} PATHS "${TOOLCHAIN_BIN}" NO_DEFAULT_PATH REQUIRED)
find_program(NDK_LD     ld           PATHS "${TOOLCHAIN_BIN}" NO_DEFAULT_PATH REQUIRED)
find_program(NDK_RANLIB llvm-ranlib  PATHS "${TOOLCHAIN_BIN}" NO_DEFAULT_PATH REQUIRED)
find_program(NDK_STRIP  llvm-strip   PATHS "${TOOLCHAIN_BIN}" NO_DEFAULT_PATH REQUIRED)
find_program(NDK_MAKE   make         PATHS "${PREBUILTS_BIN}" NO_DEFAULT_PATH REQUIRED)

# Common compiler flags for current build config (debug or release)
string(TOUPPER "${CMAKE_BUILD_TYPE}" CMAKE_BUILD_TYPE_UPPER)
set(NDK_C_FLAGS "${CMAKE_C_FLAGS} ${CMAKE_C_FLAGS_${CMAKE_BUILD_TYPE_UPPER}}")
