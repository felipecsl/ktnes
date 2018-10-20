package com.felipecsl.knes

internal class Mapper2(
    cartridge: Cartridge,
    private val stepCallback: MapperStepCallback?
) : Mapper {
  private val prgBanks: Int = cartridge.prg.size / 0x4000
  private var prgBank1: Int = 0
  private val prgBank2: Int = prgBanks - 1
  private val chr = cartridge.chr
  private val prg = cartridge.prg
  private val sram = cartridge.sram
  override lateinit var cpu: CPU

  override fun read(address: Int): Int {
    return when {
      address < 0x2000 -> return chr[address]
      address >= 0xC000 -> prg[prgBank2 * 0x4000 + (address - 0xC000)]
      address >= 0x8000 -> prg[prgBank1 * 0x4000 + (address - 0x8000)]
      address >= 0x6000 -> sram[address - 0x6000]
      else -> throw IllegalStateException("unhandled mapper2 read at address: $address")
    }
  }

  override fun write(address: Int, value: Int) {
    when {
      address < 0x2000 -> chr[address] = value
      address >= 0x8000 -> prgBank1 = value % prgBanks
      address >= 0x6000 -> sram[address - 0x6000] = value
      else -> throw IllegalStateException("unhandled mapper2 write at address: $address")
    }
  }

  override fun step() {
  }

  override fun restoreState(serializedState: String) {
    TODO("not implemented")
  }

  override fun dumpState(): String {
    TODO("not implemented")
  }
}
