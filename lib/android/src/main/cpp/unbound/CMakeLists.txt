include(ExternalProject)

ExternalProject_Add(
    Unbound
    URL "${VENDOR_DIR}/unbound"
    CONFIGURE_COMMAND env
      "AR=${NDK_AR}"
      "CC=${NDK_CC}"
      "AS=${NDK_AS}"
      "CXX=${NDK_CXX}"
      "LD=${NDK_LD}"
      "RANLIB=${NDK_RANLIB}"
      "STRIP=${NDK_STRIP}"
      "CFLAGS=${NDK_C_FLAGS}"
      "LDFLAGS=-L$<TARGET_FILE_DIR:OpenSSL::Crypto> \
               -L$<TARGET_FILE_DIR:OpenSSL::SSL>"  # to pass autoconf SSL tests
      ./configure
      "--prefix=<INSTALL_DIR>"
      --host=${TARGET_HOST}
      --with-pic
      --with-libunbound-only
      "--with-ssl=${OPENSSL_SOURCE_DIR}"
      --enable-static
      --enable-pie
      --disable-shared
      --disable-gost
    BUILD_IN_SOURCE 1
    BUILD_COMMAND "${NDK_MAKE}" install "-j${CORES}"
    BUILD_BYPRODUCTS
      "<INSTALL_DIR>/lib/libunbound.a"
)

add_dependencies(Unbound OpenSSL::Crypto OpenSSL::SSL)

ExternalProject_Get_Property(Unbound INSTALL_DIR)

set(UNBOUND_INCLUDE_DIR "${INSTALL_DIR}/include" PARENT_SCOPE)

add_library(unbound STATIC IMPORTED GLOBAL)
set_property(TARGET unbound PROPERTY IMPORTED_LOCATION "${INSTALL_DIR}/lib/libunbound.a")
add_dependencies(unbound Unbound)

add_library(Unbound::unbound ALIAS unbound)
