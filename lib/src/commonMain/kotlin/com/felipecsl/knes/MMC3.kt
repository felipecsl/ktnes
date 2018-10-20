@file:Suppress("NOTHING_TO_INLINE")

package com.felipecsl.knes

// http://wiki.nesdev.com/w/index.php/MMC3
internal class MMC3(
    private val cartridge: Cartridge,
    private val stepCallback: MapperStepCallback? = null
) : Mapper {
  override lateinit var cpu: CPU

  private var register: Int = 0
  private var registers = IntArray(8)
  private var prgMode: Int = 0
  private var chrMode: Int = 0
  private var prgOffsets = IntArray(4)
  private var chrOffsets = IntArray(8)
  private var reload: Int = 0
  private var counter: Int = 0
  private var irqEnable: Boolean = false
  private var chr = cartridge.chr
  private var prg = cartridge.prg
  private var sram = cartridge.sram

  init {
    prgOffsets[0] = prgBankOffset(0)
    prgOffsets[1] = prgBankOffset(1)
    prgOffsets[2] = prgBankOffset(-2)
    prgOffsets[3] = prgBankOffset(-1)
  }

  override fun read(address: Int): Int {
    return when {
      address < 0x2000 -> {
        val bank = address / 0x0400
        val offset = address % 0x0400
        chr[chrOffsets[bank] + offset]
      }
      address >= 0x8000 -> {
        val addr = address - 0x8000
        val bank = addr / 0x2000
        val offset = addr % 0x2000
        prg[prgOffsets[bank] + offset]
      }
      address >= 0x6000 -> {
        return sram[address - 0x6000]
      }
      else -> throw RuntimeException("unhandled mapper4 read at address: $address")
    }
  }

  override fun restoreState(serializedState: String) {
    val state = StatePersistence.restoreState(serializedState)
    register = state.next()
    registers = state.next()
    prgMode = state.next()
    chrMode = state.next()
    prgOffsets = state.next()
    chrOffsets = state.next()
    reload = state.next()
    counter = state.next()
    irqEnable = state.next()
    prg = state.next()
    chr = state.next()
    sram = state.next()
    println("MMC3 state restored")
  }

  override fun dumpState(): String {
    return StatePersistence.dumpState(
        register, registers, prgMode, chrMode, prgOffsets, chrOffsets, reload, counter, irqEnable,
        prg, chr, sram
    ).also { println("MMC3 state saved") }
  }

  override fun write(address: Int, value: Int) {
    when {
      address < 0x2000 -> {
        val bank = address / 0x0400
        val offset = address % 0x0400
        chr[chrOffsets[bank] + offset] = value
      }
      address >= 0x8000 -> {
        // write register
        if (address <= 0x9FFF && address % 2 == 0) {
          // write bank select
          prgMode = (value ushr 6) and 1
          chrMode = (value ushr 7) and 1
          register = value and 7
          updateOffsets()
        } else if (address <= 0x9FFF && address % 2 == 1) {
          registers[register] = value
          updateOffsets()
        } else if (address <= 0xBFFF && address % 2 == 0) {
          when (value and 1) {
            0 -> cartridge.mirror = MirrorVertical
            1 -> cartridge.mirror = MirrorHorizontal
          }
        } else if (address <= 0xBFFF && address % 2 == 1) {
        } else if (address <= 0xDFFF && address % 2 == 0) {
          reload = value
        } else if (address <= 0xDFFF && address % 2 == 1) {
          counter = 0
        } else if (address <= 0xFFFF && address % 2 == 0) {
          irqEnable = false
        } else if (address <= 0xFFFF && address % 2 == 1) {
          irqEnable = true
        }
      }
      address >= 0x6000 -> {
        sram[address - 0x6000] = value
      }
      else -> throw RuntimeException("unhandled mapper4 write at address $address")
    }
  }

  private inline fun updateOffsets() {
    when (prgMode) {
      0 -> {
        prgOffsets[0] = prgBankOffset(registers[6])
        prgOffsets[1] = prgBankOffset(registers[7])
        prgOffsets[2] = prgBankOffset(-2)
        prgOffsets[3] = prgBankOffset(-1)
      }
      1 -> {
        prgOffsets[0] = prgBankOffset(-2)
        prgOffsets[1] = prgBankOffset(registers[7])
        prgOffsets[2] = prgBankOffset(registers[6])
        prgOffsets[3] = prgBankOffset(-1)
      }
    }
    when (chrMode) {
      0 -> {
        chrOffsets[0] = chrBankOffset(registers[0] and 0xFE)
        chrOffsets[1] = chrBankOffset(registers[0] or 0x01)
        chrOffsets[2] = chrBankOffset(registers[1] and 0xFE)
        chrOffsets[3] = chrBankOffset(registers[1] or 0x01)
        chrOffsets[4] = chrBankOffset(registers[2])
        chrOffsets[5] = chrBankOffset(registers[3])
        chrOffsets[6] = chrBankOffset(registers[4])
        chrOffsets[7] = chrBankOffset(registers[5])
      }
      1 -> {
        chrOffsets[0] = chrBankOffset(registers[2])
        chrOffsets[1] = chrBankOffset(registers[3])
        chrOffsets[2] = chrBankOffset(registers[4])
        chrOffsets[3] = chrBankOffset(registers[5])
        chrOffsets[4] = chrBankOffset(registers[0] and 0xFE)
        chrOffsets[5] = chrBankOffset(registers[0] or 0x01)
        chrOffsets[6] = chrBankOffset(registers[1] and 0xFE)
        chrOffsets[7] = chrBankOffset(registers[1] or 0x01)
      }
    }
  }

  private inline fun chrBankOffset(index: Int): Int {
    var idx = index
    if (idx >= 0x80) {
      idx -= 0x100
    }
    val size = chr.size
    idx %= size / 0x0400
    var offset = idx * 0x0400
    if (offset < 0) {
      offset += size
    }
    return offset
  }

  private inline fun prgBankOffset(index: Int): Int {
    var idx = index
    if (idx >= 0x80) {
      idx -= 0x100
    }
    val size = prg.size
    idx %= size / 0x2000
    var offset = idx * 0x2000
    if (offset < 0) {
      offset += size
    }
    return offset
  }

  override fun step() {
//    stepCallback?.onStep(register, registers, prgMode, chrMode, prgOffsets, chrOffsets, reload,
//        counter, irqEnable)
    if (counter == 0) {
      counter = reload
    } else {
      counter = (counter - 1) and 0xFF
      if (counter == 0 && irqEnable) {
        // trigger IRQ causes an IRQ interrupt to occur on the next cycle
        if (cpu.I == 0) {
          cpu.interrupt = Interrupt.IRQ
        }
      }
    }
  }

  companion object {
    val MirrorHorizontal = 0
    val MirrorVertical = 1
    val MirrorSingle0 = 2
    val MirrorSingle1 = 3
    val MirrorFour = 4
  }
}