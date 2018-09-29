package com.felipecsl.knes

expect fun currentTimeMs(): Long

expect class AudioSink {
  fun write(value: Float)
  fun drain(): FloatArray
}