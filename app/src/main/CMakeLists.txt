cmake_minimum_required(VERSION 3.18.1)

# Project name
project("edgedetection")

# Add OpenCV (we'll download this later)
# For now, we'll create a basic native library

# Create library
add_library(
    edgedetection
    SHARED
    native-lib.cpp
)

# Find required packages
find_library(
    log-lib
    log
)

# Link libraries
target_link_libraries(
    edgedetection
    android
    ${log-lib}
)

# Include directories
include_directories(src/main/cpp/include)