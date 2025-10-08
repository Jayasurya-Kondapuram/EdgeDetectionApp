cmake_minimum_required(VERSION 3.18.1)

# Project name
project("edgedetection")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Create library
project("edgedetection")

add_library(edgedetection SHARED native-lib.cpp)

find_library(log-lib log)

target_link_libraries(edgedetection ${log-lib})

# Include directories
include_directories(src/main/cpp/include)