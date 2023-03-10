# Avoid multiple calls to find_package to append duplicated properties to the targets
include_guard()########### VARIABLES #######################################################################
#############################################################################################
set(argparse_FRAMEWORKS_FOUND_DEBUG "") # Will be filled later
conan_find_apple_frameworks(argparse_FRAMEWORKS_FOUND_DEBUG "${argparse_FRAMEWORKS_DEBUG}" "${argparse_FRAMEWORK_DIRS_DEBUG}")

set(argparse_LIBRARIES_TARGETS "") # Will be filled later


######## Create an interface target to contain all the dependencies (frameworks, system and conan deps)
if(NOT TARGET argparse_DEPS_TARGET)
    add_library(argparse_DEPS_TARGET INTERFACE IMPORTED)
endif()

set_property(TARGET argparse_DEPS_TARGET
             PROPERTY INTERFACE_LINK_LIBRARIES
             $<$<CONFIG:Debug>:${argparse_FRAMEWORKS_FOUND_DEBUG}>
             $<$<CONFIG:Debug>:${argparse_SYSTEM_LIBS_DEBUG}>
             $<$<CONFIG:Debug>:>
             APPEND)

####### Find the libraries declared in cpp_info.libs, create an IMPORTED target for each one and link the
####### argparse_DEPS_TARGET to all of them
conan_package_library_targets("${argparse_LIBS_DEBUG}"    # libraries
                              "${argparse_LIB_DIRS_DEBUG}" # package_libdir
                              "${argparse_BIN_DIRS_DEBUG}" # package_bindir
                              "${argparse_LIBRARY_TYPE_DEBUG}"
                              "${argparse_IS_HOST_WINDOWS_DEBUG}"
                              argparse_DEPS_TARGET
                              argparse_LIBRARIES_TARGETS  # out_libraries_targets
                              "_DEBUG"
                              "argparse"    # package_name
                              "${argparse_NO_SONAME_MODE_DEBUG}")  # soname

# FIXME: What is the result of this for multi-config? All configs adding themselves to path?
set(CMAKE_MODULE_PATH ${argparse_BUILD_DIRS_DEBUG} ${CMAKE_MODULE_PATH})

########## GLOBAL TARGET PROPERTIES Debug ########################################
    set_property(TARGET argparse::argparse
                 PROPERTY INTERFACE_LINK_LIBRARIES
                 $<$<CONFIG:Debug>:${argparse_OBJECTS_DEBUG}>
                 $<$<CONFIG:Debug>:${argparse_LIBRARIES_TARGETS}>
                 APPEND)

    if("${argparse_LIBS_DEBUG}" STREQUAL "")
        # If the package is not declaring any "cpp_info.libs" the package deps, system libs,
        # frameworks etc are not linked to the imported targets and we need to do it to the
        # global target
        set_property(TARGET argparse::argparse
                     PROPERTY INTERFACE_LINK_LIBRARIES
                     argparse_DEPS_TARGET
                     APPEND)
    endif()

    set_property(TARGET argparse::argparse
                 PROPERTY INTERFACE_LINK_OPTIONS
                 $<$<CONFIG:Debug>:${argparse_LINKER_FLAGS_DEBUG}> APPEND)
    set_property(TARGET argparse::argparse
                 PROPERTY INTERFACE_INCLUDE_DIRECTORIES
                 $<$<CONFIG:Debug>:${argparse_INCLUDE_DIRS_DEBUG}> APPEND)
    # Necessary to find LINK shared libraries in Linux
    set_property(TARGET argparse::argparse
                 PROPERTY INTERFACE_LINK_DIRECTORIES
                 $<$<CONFIG:Debug>:${argparse_LIB_DIRS_DEBUG}> APPEND)
    set_property(TARGET argparse::argparse
                 PROPERTY INTERFACE_COMPILE_DEFINITIONS
                 $<$<CONFIG:Debug>:${argparse_COMPILE_DEFINITIONS_DEBUG}> APPEND)
    set_property(TARGET argparse::argparse
                 PROPERTY INTERFACE_COMPILE_OPTIONS
                 $<$<CONFIG:Debug>:${argparse_COMPILE_OPTIONS_DEBUG}> APPEND)

########## For the modules (FindXXX)
set(argparse_LIBRARIES_DEBUG argparse::argparse)
