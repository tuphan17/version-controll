cmake_minimum_required(VERSION 3.16)
project(tabular_vc C)

set(CMAKE_C_STANDARD 99)
add_executable(tvcs
  src/tvcs.c
  src/fsutil.c
  src/hash.c
  src/repo.c
)

if(MSVC)
  target_compile_definitions(tvcs PRIVATE _CRT_SECURE_NO_WARNINGS)
else()
  target_compile_options(tvcs PRIVATE -Wall -Wextra -pedantic)
endif()

target_include_directories(tvcs PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/include")
