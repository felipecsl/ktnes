package com.felipecsl.knes

import android.os.Build
import androidx.annotation.RequiresApi

actual fun currentTimeMs(): Long {
  return System.currentTimeMillis()
}

actual class AudioSink {
  private val bufferSize = 4096
  private var buffer = FloatArray(bufferSize)
  private var pos = 0

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  actual fun write(value: Float) {
    synchronized(lock) {
      buffer[pos++] = value
    }
  }

  actual fun drain(): FloatArray {
    synchronized(lock) {
      val out = FloatArray(pos)
      for (i in 0 until pos) {
        out[i] = buffer[i]
        buffer[i] = 0F
      }
      for (i in pos until bufferSize) {
        buffer[i] = 0F
      }
      pos = 0
      return out
    }
  }

  companion object {
    private val lock = Object()
  }
}