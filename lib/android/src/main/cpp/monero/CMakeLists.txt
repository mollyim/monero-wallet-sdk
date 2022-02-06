set(MONERO_DIR "${VENDOR_DIR}/monero")

set(ARCH_WIDTH "64")

# Require C11/C++14 and disable extensions for all monero targets
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_C_EXTENSIONS OFF)
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Just an empty macro
macro(monero_enable_coverage)
endmacro()

# From monero's root CMakeLists.txt
macro(monero_find_all_headers headers_found module_root_dir)
  file(GLOB ${headers_found}
      "${module_root_dir}/*.h*"    # h* will include hpps as well.
      "${module_root_dir}/**/*.h*" # Any number of subdirs will be included.
      "${module_root_dir}/*.inl"   # .inl is typically template code and is being treated as headers (it's being included).
      "${module_root_dir}/**/*.inl"
      )
endmacro()

add_subdirectory(easylogging)
add_subdirectory(randomx)
add_subdirectory(lmdb)
add_subdirectory(wallet_api)
