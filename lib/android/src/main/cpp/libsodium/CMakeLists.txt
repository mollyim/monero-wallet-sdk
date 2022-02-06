include(ExternalProject)

ExternalProject_Add(
    Libsodium
    URL "${VENDOR_DIR}/libsodium"
    CONFIGURE_COMMAND ./autogen.sh && env
      "AR=${NDK_AR}"
      "CC=${NDK_CC}"
      "AS=${NDK_AS}"
      "CXX=${NDK_CXX}"
      "LD=${NDK_LD}"
      "RANLIB=${NDK_RANLIB}"
      "STRIP=${NDK_STRIP}"
      "CFLAGS=${NDK_C_FLAGS}"
      ./configure
      "--prefix=<INSTALL_DIR>"
      --host=${TARGET_HOST}
      --enable-minimal
      --enable-static
      --disable-shared
    BUILD_IN_SOURCE 1
    BUILD_COMMAND "${NDK_MAKE}" install "-j${CORES}"
    BUILD_BYPRODUCTS
      "<INSTALL_DIR>/lib/libsodium.a"
)

ExternalProject_Get_Property(Libsodium INSTALL_DIR)

set(LIBSODIUM_INCLUDE_DIR "${INSTALL_DIR}/include" PARENT_SCOPE)

add_library(libsodium STATIC IMPORTED GLOBAL)
set_property(TARGET libsodium PROPERTY IMPORTED_LOCATION "${INSTALL_DIR}/lib/libsodium.a")
add_dependencies(libsodium Libsodium)

add_library(Libsodium::libsodium ALIAS libsodium)
