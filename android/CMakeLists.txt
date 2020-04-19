cmake_minimum_required(VERSION 3.4.1)

add_library( # Specifies the name of the library.
        ktnes-audio
        SHARED
        src/main/cpp/native-lib.cpp
        src/main/cpp/AudioEngine.cpp
        )

find_package (oboe REQUIRED CONFIG)

target_link_libraries(ktnes-audio log oboe::oboe)