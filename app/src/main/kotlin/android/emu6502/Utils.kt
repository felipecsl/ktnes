package android.emu6502

fun Int.toHexString(): String {
  return java.lang.String.format("%02X", this);
}

fun Int.isSet(): Boolean {
  return this != 0
}