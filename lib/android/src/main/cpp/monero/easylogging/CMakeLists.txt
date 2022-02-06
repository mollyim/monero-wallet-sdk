set(EASYLOGGING_SOURCE_DIR "${MONERO_DIR}/external/easylogging++")

add_compile_definitions(AUTO_INITIALIZE_EASYLOGGINGPP)

add_subdirectory("${EASYLOGGING_SOURCE_DIR}" "easylogging++" EXCLUDE_FROM_ALL)
add_library(Monero::easylogging ALIAS easylogging)

set_target_properties(easylogging PROPERTIES
    INTERFACE_INCLUDE_DIRECTORIES "${EASYLOGGING_SOURCE_DIR}"
)
