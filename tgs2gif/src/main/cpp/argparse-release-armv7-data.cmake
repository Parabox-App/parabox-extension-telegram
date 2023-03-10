########### AGGREGATED COMPONENTS AND DEPENDENCIES FOR THE MULTI CONFIG #####################
#############################################################################################

set(argparse_COMPONENT_NAMES "")
set(argparse_FIND_DEPENDENCY_NAMES "")

########### VARIABLES #######################################################################
#############################################################################################
set(argparse_PACKAGE_FOLDER_RELEASE "C:/Users/ojhdt/.conan2/p/argpada1dc208edf26/p")
set(argparse_BUILD_MODULES_PATHS_RELEASE )


set(argparse_INCLUDE_DIRS_RELEASE "${argparse_PACKAGE_FOLDER_RELEASE}/include")
set(argparse_RES_DIRS_RELEASE )
set(argparse_DEFINITIONS_RELEASE )
set(argparse_SHARED_LINK_FLAGS_RELEASE )
set(argparse_EXE_LINK_FLAGS_RELEASE )
set(argparse_OBJECTS_RELEASE )
set(argparse_COMPILE_DEFINITIONS_RELEASE )
set(argparse_COMPILE_OPTIONS_C_RELEASE )
set(argparse_COMPILE_OPTIONS_CXX_RELEASE )
set(argparse_LIB_DIRS_RELEASE )
set(argparse_BIN_DIRS_RELEASE )
set(argparse_LIBRARY_TYPE_RELEASE UNKNOWN)
set(argparse_IS_HOST_WINDOWS_RELEASE 0)
set(argparse_LIBS_RELEASE )
set(argparse_SYSTEM_LIBS_RELEASE )
set(argparse_FRAMEWORK_DIRS_RELEASE )
set(argparse_FRAMEWORKS_RELEASE )
set(argparse_BUILD_DIRS_RELEASE )
set(argparse_NO_SONAME_MODE_RELEASE FALSE)


# COMPOUND VARIABLES
set(argparse_COMPILE_OPTIONS_RELEASE
    "$<$<COMPILE_LANGUAGE:CXX>:${argparse_COMPILE_OPTIONS_CXX_RELEASE}>"
    "$<$<COMPILE_LANGUAGE:C>:${argparse_COMPILE_OPTIONS_C_RELEASE}>")
set(argparse_LINKER_FLAGS_RELEASE
    "$<$<STREQUAL:$<TARGET_PROPERTY:TYPE>,SHARED_LIBRARY>:${argparse_SHARED_LINK_FLAGS_RELEASE}>"
    "$<$<STREQUAL:$<TARGET_PROPERTY:TYPE>,MODULE_LIBRARY>:${argparse_SHARED_LINK_FLAGS_RELEASE}>"
    "$<$<STREQUAL:$<TARGET_PROPERTY:TYPE>,EXECUTABLE>:${argparse_EXE_LINK_FLAGS_RELEASE}>")


set(argparse_COMPONENTS_RELEASE )