set(RANDOMX_SOURCE_DIR "${MONERO_DIR}/external/randomx")

add_subdirectory("${RANDOMX_SOURCE_DIR}" "randomx" EXCLUDE_FROM_ALL)
add_library(Monero::randomx ALIAS randomx)

set_target_properties(randomx PROPERTIES
    INTERFACE_INCLUDE_DIRECTORIES "${RANDOMX_SOURCE_DIR}/src"
)
