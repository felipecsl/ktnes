package com.felipecsl.knes

actual fun currentTimeMs(): Long {
  return System.currentTimeMillis()
}

actual class AudioSink {
  actual fun write(value: Float) {
  }

  actual fun drain(): FloatArray {
    return FloatArray(0)
  }
}