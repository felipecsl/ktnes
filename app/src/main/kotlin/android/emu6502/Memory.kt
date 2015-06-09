package android.emu6502

class Memory {
  private val mem = ByteArray(65536)

  public fun get(addr: Int): Byte {
    return mem[addr]
  }

  public fun set(addr: Int, value: Int) {
    mem[addr] = value.toByte()
  }
}
