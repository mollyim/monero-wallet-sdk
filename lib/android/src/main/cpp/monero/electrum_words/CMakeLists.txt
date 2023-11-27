set(ELECTRUM_WORDS_SOURCES
    src/mnemonics/electrum-words.cpp
)

set(ELECTRUM_WORDS_OVERRIDES
    mlog_override.cc
)

set(ELECTRUM_WORDS_INCLUDES
    contrib/epee/include
    src
    src/mnemonics
)

list(TRANSFORM ELECTRUM_WORDS_SOURCES  PREPEND "${MONERO_DIR}/")
list(TRANSFORM ELECTRUM_WORDS_INCLUDES PREPEND "${MONERO_DIR}/")

set(EASYLOGGING_SOURCE_DIR "${MONERO_DIR}/external/easylogging++")

add_library(
    electrum_words STATIC ${ELECTRUM_WORDS_SOURCES} ${ELECTRUM_WORDS_OVERRIDES}
)

target_include_directories(
    electrum_words
    PUBLIC
      "${ELECTRUM_WORDS_INCLUDES}"
)

# Include external project header directories here.  Workaround for:
# https://gitlab.kitware.com/cmake/cmake/issues/15052
target_include_directories(
    electrum_words
    SYSTEM PUBLIC
      "${BOOST_INCLUDE_DIR}"
      "${LIBSODIUM_INCLUDE_DIR}"
)

target_link_libraries(
    electrum_words
    PUBLIC
    PRIVATE
      Monero::easylogging
      Libsodium::libsodium
)

# Mnemonics depends on boost::crc that is a header-only library
add_dependencies(electrum_words Boost)

add_library(Monero::electrum_words ALIAS electrum_words)
