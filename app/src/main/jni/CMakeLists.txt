cmake_minimum_required(VERSION 3.22.1)
project(termux C)

add_library(termux SHARED termux.c)

# Force 16KB page alignment for Google Play compatibility
target_link_options(termux PRIVATE "-Wl,-z,max-page-size=16384")
