package com.felipecsl.knes

import kotlinx.cinterop.*
import platform.posix.*

actual fun currentTimeMs(): Long {
  memScoped {
    val now = alloc<timeval>()
    gettimeofday(now.ptr, null)
    return (now.tv_sec.toLong() * 1000) + (now.tv_usec.toLong() / 1000)
  }
}

actual class Bitmap actual constructor(width: Int, height: Int) {
  actual fun setPixel(x: Int, y: Int, color: Int) {
    // TODO
  }
}