include(ExternalProject)

# The source code of Boost
# From https://www.boost.org/users/download/
set(BOOST_SOURCE
    "https://boostorg.jfrog.io/artifactory/main/release/1.78.0/source/boost_1_78_0.tar.bz2"
    CACHE STRING "Boost source file to use.")
set(BOOST_SHA256
    "8681f175d4bdb26c52222665793eef08490d7758529330f98d3b29dd0735bccc"
    CACHE STRING "SHA256 hash for the Boost source file.")
mark_as_advanced(BOOST_SOURCE BOOST_SHA256)

# Decide between debug or release
if(CMAKE_BUILD_TYPE STREQUAL "Debug")
  set(BOOST_VARIANT "debug")
elseif(CMAKE_BUILD_TYPE STREQUAL "RelWithDebInfo")
  set(BOOST_VARIANT "release")
else()
  message(FATAL_ERROR "Boost: build type '${CMAKE_BUILD_TYPE}' not supported.")
endif()

# B2 user-config.jam template
set(BOOST_USER_CONFIG "boost_${CMAKE_ANDROID_ARCH_ABI}.jam")
configure_file("${CMAKE_CURRENT_SOURCE_DIR}/user-config/${BOOST_USER_CONFIG}.in"
               "${BOOST_USER_CONFIG}")

ExternalProject_Add(
    Boost
    URL "${BOOST_SOURCE}"
    URL_HASH "SHA256=${BOOST_SHA256}"
    DOWNLOAD_DIR "${DOWNLOAD_CACHE}"
    UPDATE_DISCONNECTED TRUE
    CONFIGURE_COMMAND ./bootstrap.sh "--prefix=<INSTALL_DIR>"
    BUILD_IN_SOURCE 1
    BUILD_COMMAND ./b2 install
      toolset=clang-ndk
      target-os=android
      variant=${BOOST_VARIANT}
      link=static
      "-j${CORES}"
      --with-chrono
      --with-date_time
      --with-filesystem
      --with-iostreams
      --with-regex
      --with-serialization
      --with-system
      --with-thread
    INSTALL_COMMAND ""
    BUILD_BYPRODUCTS
      "<INSTALL_DIR>/lib/libboost_chrono.a"
      "<INSTALL_DIR>/lib/libboost_date_time.a"
      "<INSTALL_DIR>/lib/libboost_filesystem.a"
      "<INSTALL_DIR>/lib/libboost_iostreams.a"
      "<INSTALL_DIR>/lib/libboost_regex.a"
      "<INSTALL_DIR>/lib/libboost_serialization.a"
      "<INSTALL_DIR>/lib/libboost_system.a"
      "<INSTALL_DIR>/lib/libboost_thread.a"
)

ExternalProject_Add_Step(
    Boost userconfig
    COMMENT "Setting up user-config.jam for cross compiling"
    COMMAND "${CMAKE_COMMAND}" -E copy "${BOOST_USER_CONFIG}" "<SOURCE_DIR>/tools/build/src/user-config.jam"
    DEPENDS "${BOOST_USER_CONFIG}"
    DEPENDEES update
    DEPENDERS configure
)

ExternalProject_Get_Property(Boost INSTALL_DIR)

set(BOOST_INCLUDE_DIR "${INSTALL_DIR}/include" PARENT_SCOPE)

function(link_boost_library COMPONENT)
  add_library(${COMPONENT} STATIC IMPORTED GLOBAL)
  set_property(TARGET ${COMPONENT} PROPERTY IMPORTED_LOCATION "${INSTALL_DIR}/lib/libboost_${COMPONENT}.a")
  set_property(TARGET ${COMPONENT} PROPERTY INTERFACE_SYSTEM_INCLUDE_DIRECTORIES "${BOOST_INCLUDE_DIR}")
  foreach(link_dep ${ARGN})
    set_property(TARGET ${COMPONENT} APPEND PROPERTY INTERFACE_LINK_LIBRARIES ${link_dep})
  endforeach()
  add_dependencies(${COMPONENT} Boost)
  add_library("Boost::${COMPONENT}" ALIAS ${COMPONENT})
endfunction()

link_boost_library(chrono)
link_boost_library(date_time)
link_boost_library(filesystem system)
link_boost_library(iostreams)
link_boost_library(regex)
link_boost_library(serialization)
link_boost_library(system)
link_boost_library(thread chrono date_time regex)
