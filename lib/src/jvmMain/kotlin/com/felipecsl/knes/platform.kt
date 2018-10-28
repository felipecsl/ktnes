package com.felipecsl.knes

actual fun currentTimeMs(): Double {
  return System.currentTimeMillis().toDouble()
}