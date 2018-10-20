package com.felipecsl.knes

class AudioBuffer {
  private var buffer = FloatArray(BUFFER_SIZE) { 0F }
  private var pos = 0

  fun write(value: Float) {
    buffer[pos++ % BUFFER_SIZE] = value
  }

  fun drain(): FloatArray {
    pos = 0
    return buffer
  }

  companion object {
    private const val BUFFER_SIZE = 2048
  }
}