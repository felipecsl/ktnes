package com.felipecsl.knes

fun Int.ensureByte(): Int {
  if (this < 0 || this > 255) {
    throw RuntimeException("Value is not a byte: $this")
  }
  return this
}

fun Int.ensurePositive(): Int {
  if (this < 0) {
    throw RuntimeException("Value is not positive: $this")
  }
  return this
}
