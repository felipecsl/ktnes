package com.felipecsl.knes

import android.os.Build
import androidx.annotation.RequiresApi
import java.util.*

actual fun currentTimeMs(): Long {
  return System.currentTimeMillis()
}

actual class AudioSink {
  private val bufferSize = 4096
  private var buffer = FloatArray(bufferSize)
  private var pos = 0

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  actual fun write(value: Float) {
    buffer[pos++ % bufferSize] = value
  }

  actual fun drain(): FloatArray {
    val out = Arrays.copyOf(buffer, pos)
    pos = 0
    return out
  }
}