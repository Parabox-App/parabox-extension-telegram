# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.22.1)

# Declares and names the project.

project("tgs2gif")

include(ExternalProject)

set(CMAKE_CXX_STANDARD 17)

if(CMAKE_SYSTEM_NAME STREQUAL "Darwin")
    # https://stackoverflow.com/a/3801032
    set(STATIC_LINKING_DEFAULT_VALUE OFF)
else()
    set(STATIC_LINKING_DEFAULT_VALUE ON)
endif()

option(TGS_TO_PNG_STATIC_LINKING "Build using static linking" ${STATIC_LINKING_DEFAULT_VALUE})

#include(conanbuildinfo.cmake)
conan_basic_setup()

if (TGS_TO_PNG_STATIC_LINKING)
    # enable static linking https://stackoverflow.com/a/24671474
    set(CMAKE_FIND_LIBRARY_SUFFIXES ".a")
    set(BUILD_SHARED_LIBS OFF)
    set(CMAKE_EXE_LINKER_FLAGS "-static")
endif()

add_executable(
        ${PROJECT_NAME}
        src/main.cpp
        src/render.cpp src/render.h
        src/zstr/fstream.h src/zstr/zstr.h
)

ExternalProject_Add(
        rlottie
        GIT_REPOSITORY "https://github.com/Samsung/rlottie.git"
        GIT_TAG "875626965959d8e269ca22175c8e1ad190696c43"
        ${ARGN}
        PREFIX "${CMAKE_BINARY_DIR}/lib"
        CMAKE_ARGS
        -Wno-dev
        "-DCMAKE_TOOLCHAIN_FILE:PATH=${CMAKE_TOOLCHAIN_FILE}"
        "-DCMAKE_USER_MAKE_RULES_OVERRIDE=${CMAKE_USER_MAKE_RULES_OVERRIDE}"
        "-DCMAKE_C_FLAGS:STRING=${CMAKE_C_FLAGS}"
        "-DCMAKE_CXX_FLAGS:STRING=${CMAKE_CXX_FLAGS}"
        "-DCMAKE_BUILD_TYPE:STRING=${CMAKE_BUILD_TYPE}"
        "-DCMAKE_INSTALL_PREFIX:PATH=<INSTALL_DIR>"
        "-DLOTTIE_MODULE=OFF"
        "-DBUILD_SHARED_LIBS=OFF"
        "-DLIB_INSTALL_DIR=${CMAKE_BINARY_DIR}/lib/rlottie"
        BUILD_BYPRODUCTS "${CMAKE_BINARY_DIR}/lib/rlottie/librlottie.a"
)
set(INCLUDES ${INCLUDES} "${CMAKE_BINARY_DIR}/lib/src/rlottie/inc")
set(LIBS ${LIBS} pthread "${CMAKE_BINARY_DIR}/lib/src/rlottie-build/librlottie.a")
add_dependencies(${PROJECT_NAME} rlottie)

include_directories(${INCLUDES})
target_link_libraries(${PROJECT_NAME} ${CONAN_LIBS} ${LIBS})
target_compile_options(${PROJECT_NAME} PUBLIC)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        tgs2gif

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        tgs2gif.cpp)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
        tgs2gif

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})