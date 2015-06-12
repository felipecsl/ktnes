package android.emu6502

class Memory(private val display: Display) {
  private val mem = IntArray(65536)

  fun get(addr: Int): Int {
    return mem[addr]
  }

  fun set(addr: Int, value: Int) {
    mem[addr] = value
  }

  fun storeByte(addr: Int, value: Int) {
    set(addr, value.and(0xff))
    if ((addr >= 0x200) && (addr <= 0x5ff)) {
      display.updatePixel(addr)
    }
  }
}
