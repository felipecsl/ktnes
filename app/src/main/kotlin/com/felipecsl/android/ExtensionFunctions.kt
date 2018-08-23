package com.felipecsl.android

val mirrorLookup = arrayOf(
    arrayOf(0, 0, 1, 1),
    arrayOf(0, 1, 0, 1),
    arrayOf(0, 0, 0, 0),
    arrayOf(1, 1, 1, 1),
    arrayOf(0, 1, 2, 3)
)

fun mirrorAddress(mode: Int, addr: Int): Int {
  val address = (addr - 0x2000) % 0x1000
  val table = address / 0x0400
  val offset = address % 0x0400
  return 0x2000 + mirrorLookup[mode][table] * 0x0400 + offset
}

@Suppress("NOTHING_TO_INLINE")
inline infix fun Byte.and(other: Int): Int = (this.toInt() and other)