package com.felipecsl.android

@Suppress("NOTHING_TO_INLINE")
inline infix fun Byte.and(other: Int): Int = (this.toInt() and other)

fun Int.toHexString(): String {
  return java.lang.String.format("%02X", this)
}

fun Long.toHexString(): String {
  return java.lang.String.format("0x%16X", this)
}

fun Byte.toHexString(): String {
  return toInt().toHexString()
}