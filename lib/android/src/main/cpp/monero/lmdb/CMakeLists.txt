set(LMDB_SOURCE_DIR "${MONERO_DIR}/external/db_drivers/liblmdb")

add_subdirectory("${LMDB_SOURCE_DIR}" "lmdb" EXCLUDE_FROM_ALL)
add_library(Monero::lmdb ALIAS lmdb)

set_target_properties(lmdb PROPERTIES
    INTERFACE_INCLUDE_DIRECTORIES "${LMDB_SOURCE_DIR}"
)
