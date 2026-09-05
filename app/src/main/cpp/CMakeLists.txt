cmake_minimum_required(VERSION 3.22.1)
project("binerlauncher-native")

add_library(binerlauncher-native SHARED native_bridge.cpp)
find_library(log-lib log)
target_link_libraries(binerlauncher-native ${log-lib})
