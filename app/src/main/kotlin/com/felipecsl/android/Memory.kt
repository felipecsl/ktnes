package com.felipecsl.android

class Memory(private val display: Display) {
  private val mem = IntArray(65536)

  fun get(addr: Int): Int {
    return mem[addr]
  }

  fun getWord(addr: Int): Int {
    return get(addr) + get(addr + 1).shl(8)
  }

  fun set(addr: Int, value: Int) {
    mem[addr] = value
  }

  fun storeByte(addr: Int, value: Int) {
//    set(addr, value.and(0xff))
//    if (addr in 0x200..0x5ff) {
//      display.updatePixel(addr, mem[addr].and(0x0f))
//    }
  }
  // Store keycode in ZP $ff
  fun storeKeypress(keyCode: Int) {
    storeByte(0xff, keyCode)
  }

  fun format(start: Int, length: Int): String {
    var i = 0
    var n: Int
    val dump = StringBuilder()
    while (i < length) {
      if (i.and(15) == 0) {
        if (i > 0) {
          dump.append("\n")
        }
        n = start + i
        dump.append(n.shr(8).and(0xff).toHexString())
        dump.append(n.and(0xff).toHexString())
        dump.append(": ")
      }
      dump.append(get(start + i).toHexString())
      dump.append(" ")
      i++
    }
    return dump.toString().trim()
  }
}
