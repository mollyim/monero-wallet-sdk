set(WALLET2_SOURCES
    contrib/epee/src/abstract_http_client.cpp
    contrib/epee/src/byte_slice.cpp
    contrib/epee/src/byte_stream.cpp
    contrib/epee/src/file_io_utils.cpp
    contrib/epee/src/hex.cpp
    contrib/epee/src/http_auth.cpp
    contrib/epee/src/int-util.cpp
    contrib/epee/src/memwipe.c
    contrib/epee/src/mlocker.cpp
    contrib/epee/src/net_helper.cpp
    contrib/epee/src/net_parse_helpers.cpp
    contrib/epee/src/net_ssl.cpp
    contrib/epee/src/net_utils_base.cpp
    contrib/epee/src/parserse_base_utils.cpp
    contrib/epee/src/portable_storage.cpp
    contrib/epee/src/string_tools.cpp
    contrib/epee/src/wipeable_string.cpp
    src/blockchain_db/blockchain_db.cpp
    src/blockchain_db/lmdb/db_lmdb.cpp
    src/checkpoints/checkpoints.cpp
    src/common/aligned.c
    src/common/base58.cpp
    src/common/combinator.cpp
    src/common/dns_utils.cpp
    src/common/error.cpp
    src/common/notify.cpp
    src/common/password.cpp
    src/common/pruning.cpp
    src/common/spawn.cpp
    src/common/threadpool.cpp
    src/common/util.cpp
    src/crypto/aesb.c
    src/crypto/blake256.c
    src/crypto/chacha.c
    src/crypto/crypto-ops-data.c
    src/crypto/crypto-ops.c
    src/crypto/crypto.cpp
    src/crypto/CryptonightR_JIT.c
    src/crypto/CryptonightR_template.h
    src/crypto/groestl.c
    src/crypto/hash-extra-blake.c
    src/crypto/hash-extra-groestl.c
    src/crypto/hash-extra-jh.c
    src/crypto/hash-extra-skein.c
    src/crypto/hash.c
    src/crypto/jh.c
    src/crypto/keccak.c
    src/crypto/oaes_lib.c
    src/crypto/random.c
    src/crypto/rx-slow-hash.c
    src/crypto/skein.c
    src/crypto/slow-hash.c
    src/crypto/tree-hash.c
    src/cryptonote_basic/account.cpp
    src/cryptonote_basic/cryptonote_basic_impl.cpp
    src/cryptonote_basic/cryptonote_format_utils.cpp
    src/cryptonote_basic/cryptonote_format_utils_basic.cpp
    src/cryptonote_basic/difficulty.cpp
    src/cryptonote_basic/hardfork.cpp
    src/cryptonote_basic/miner.cpp
    src/cryptonote_core/blockchain.cpp
    src/cryptonote_core/cryptonote_tx_utils.cpp
    src/cryptonote_core/tx_pool.cpp
    src/cryptonote_core/tx_sanity_check.cpp
    src/device/device.cpp
    src/device/device_default.cpp
    src/device_trezor/device_trezor.cpp
    src/hardforks/hardforks.cpp
    src/mnemonics/electrum-words.cpp
    src/multisig/multisig.cpp
    src/multisig/multisig_clsag_context.cpp
    src/multisig/multisig_tx_builder_ringct.cpp
    src/net/error.cpp
    src/net/http.cpp
    src/net/i2p_address.cpp
    src/net/parse.cpp
    src/net/socks.cpp
    src/net/socks_connect.cpp
    src/net/tor_address.cpp
    src/ringct/bulletproofs.cc
    src/ringct/bulletproofs_plus.cc
    src/ringct/multiexp.cc
    src/ringct/rctCryptoOps.c
    src/ringct/rctOps.cpp
    src/ringct/rctSigs.cpp
    src/ringct/rctTypes.cpp
    src/rpc/rpc_payment_signature.cpp
    src/wallet/message_store.cpp
    src/wallet/message_transporter.cpp
    src/wallet/node_rpc_proxy.cpp
    src/wallet/ringdb.cpp
    src/wallet/wallet2.cpp
    src/wallet/wallet_rpc_payments.cpp
)

if(ANDROID_ABI STREQUAL "x86_64")
  list(APPEND WALLET2_SOURCES src/crypto/CryptonightR_template.S)
endif()

set(WALLET2_OVERRIDES
    mlog_override.cc
    i18n_override.cc
    perf_timer_override.cc
)

set(WALLET2_PRECOMPILED_HEADERS
    boringssl_compat.h
)

set(WALLET2_INCLUDES
    contrib/epee/include
    external
    external/rapidjson/include
    src
    src/common
    src/crypto
    src/cryptonote_basic
    src/wallet
    src/wallet/api
)

list(TRANSFORM WALLET2_SOURCES  PREPEND "${MONERO_DIR}/")
list(TRANSFORM WALLET2_INCLUDES PREPEND "${MONERO_DIR}/")

set(GENERATED_HEADERS_DIR "${CMAKE_CURRENT_BINARY_DIR}/generated_include")

# Use internal crypto library ("cn") for wallet
configure_file("${MONERO_DIR}/src/crypto/wallet/empty.h.in" "${GENERATED_HEADERS_DIR}/crypto/wallet/ops.h")

# Do not allocate the scratchpad on the stack
add_definitions(-DFORCE_USE_HEAP=1)

add_library(
    wallet2 STATIC ${WALLET2_SOURCES} ${WALLET2_OVERRIDES}
)

target_precompile_headers(
    wallet2
    PRIVATE
      "${WALLET2_PRECOMPILED_HEADERS}"
)

target_include_directories(
    wallet2
    PUBLIC
      "${WALLET2_INCLUDES}"
      "${GENERATED_HEADERS_DIR}"
)

# Include external project header directories here.  Workaround for:
# https://gitlab.kitware.com/cmake/cmake/issues/15052
target_include_directories(
    wallet2
    SYSTEM PUBLIC
      "${BOOST_INCLUDE_DIR}"
      "${LIBSODIUM_INCLUDE_DIR}"
      "${OPENSSL_INCLUDE_DIR}"
      "${UNBOUND_INCLUDE_DIR}"
)

target_link_libraries(
    wallet2
    PUBLIC
      Monero::easylogging
    PRIVATE
      Monero::lmdb
      Monero::randomx
      OpenSSL::SSL
      Boost::filesystem
      Boost::iostreams
      Boost::serialization
      Boost::thread
      Libsodium::libsodium
      Unbound::unbound
)

add_library(Monero::wallet2 ALIAS wallet2)
