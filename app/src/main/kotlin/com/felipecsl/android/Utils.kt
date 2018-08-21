package com.felipecsl.android

fun Int.toHexString(): String {
  return java.lang.String.format("%02X", this)
}

fun Byte.toHexString(): String {
  return toInt().toHexString()
}