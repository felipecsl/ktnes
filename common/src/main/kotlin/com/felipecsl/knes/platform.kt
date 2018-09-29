package com.felipecsl.knes

expect fun currentTimeMs(): Long

expect class AudioSink constructor() {
  fun write(value: Float)
  fun drain(): FloatArray
}