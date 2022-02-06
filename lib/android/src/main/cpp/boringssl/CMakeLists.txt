set(BORINGSSL_SOURCE_DIR "${VENDOR_DIR}/boringssl")

add_subdirectory("${BORINGSSL_SOURCE_DIR}" "boringssl" EXCLUDE_FROM_ALL)

add_library(OpenSSL::SSL    ALIAS ssl)
add_library(OpenSSL::Crypto ALIAS crypto)

set(OPENSSL_SOURCE_DIR  "${BORINGSSL_SOURCE_DIR}"         PARENT_SCOPE)
set(OPENSSL_INCLUDE_DIR "${BORINGSSL_SOURCE_DIR}/include" PARENT_SCOPE)
