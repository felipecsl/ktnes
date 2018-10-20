package com.felipecsl.knes

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.gettimeofday
import platform.posix.timeval

actual fun currentTimeMs(): Long {
  memScoped {
    val now = alloc<timeval>()
    gettimeofday(now.ptr, null)
    return (now.tv_sec.toLong() * 1000) + (now.tv_usec.toLong() / 1000)
  }
}