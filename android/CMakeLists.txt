cmake_minimum_required(VERSION 3.4.1)

add_library( # Specifies the name of the library.
        ktnes-audio
        SHARED
        src/main/cpp/native-lib.cpp
        src/main/cpp/AudioEngine.cpp
        )

target_link_libraries(ktnes-audio log oboe)

# Set the path to the Oboe directory.
set(OBOE_DIR ../oboe)

# Add the Oboe library as a subdirectory in your project.
add_subdirectory(${OBOE_DIR} ./oboe)

# Specify the path to the Oboe header files.
include_directories(${OBOE_DIR}/include)