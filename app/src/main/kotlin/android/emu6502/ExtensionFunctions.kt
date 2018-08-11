package android.emu6502

infix fun Byte.shr(other: Int): Byte {
  return this.toInt().shr(other).toByte()
}

infix fun Byte.shl(other: Int): Byte {
  return this.toInt().shr(other).toByte()
}