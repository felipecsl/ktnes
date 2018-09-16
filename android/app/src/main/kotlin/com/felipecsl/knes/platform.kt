package com.felipecsl.knes

import android.media.AudioTrack
import android.os.Build
import androidx.annotation.RequiresApi

actual fun currentTimeMs(): Long {
  return System.currentTimeMillis()
}

actual class AudioSink(private val audioTrack: AudioTrack, private val bufferSize: Int) {
  private val buffer = FloatArray(bufferSize)
  private var position = 0

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  actual fun write(value: Float) {
    buffer[position++] = value
    if (position == bufferSize) {
      audioTrack.write(buffer, 0, bufferSize, AudioTrack.WRITE_NON_BLOCKING)
      position = 0
    }
  }

  fun play() {
    audioTrack.play()
  }
}