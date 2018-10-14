package com.felipecsl.knes

import com.felipecsl.knes.MMC3.Companion.MirrorHorizontal
import com.felipecsl.knes.MMC3.Companion.MirrorSingle0
import com.felipecsl.knes.MMC3.Companion.MirrorSingle1
import com.felipecsl.knes.MMC3.Companion.MirrorVertical

// http://wiki.nesdev.com/w/index.php/MMC3
internal class MMC1(
    private val cartridge: Cartridge,
    private val stepCallback: MapperStepCallback? = null
) : Mapper {
  private var shiftRegister: Int = 0x10 // Byte
  private var control: Int = 0 // Byte
  private var prgMode: Int = 0 // Byte
  private var chrMode: Int = 0 // Byte
  private var prgBank: Int = 0 // Byte
  private var chrBank0: Int = 0 // Byte
  private var chrBank1: Int = 0 // Byte
  private var prgOffsets = IntArray(2)
  private var chrOffsets = IntArray(2)
  private val chr = cartridge.chr
  private val prg = cartridge.prg
  private val sram = cartridge.sram
  override lateinit var cpu: CPU

  init {
    prgOffsets[1] = prgBankOffset(-1)
  }

  override fun read(address: Int): Int {
    return when {
      address < 0x2000 -> {
        val bank = address / 0x1000
        val offset = address % 0x1000
        chr[chrOffsets[bank] + offset]
      }
      address >= 0x8000 -> {
        val addr = address - 0x8000
        val bank = addr / 0x4000
        val offset = addr % 0x4000
        val index = prgOffsets[bank] + offset
        val ret = prg[index]
        ret
      }
      address >= 0x6000 -> {
        return sram[address - 0x6000]
      }
      else -> throw RuntimeException("unhandled mapper1 read at address: $address")
    }
  }

  override fun write(address: Int, value: Int /* Byte */) {
    when {
      address < 0x2000 -> {
        val bank = address / 0x1000
        val offset = address % 0x1000
        chr[chrOffsets[bank] + offset] = value
      }
      address >= 0x8000 -> {
        // load register
        if (value and 0x80 == 0x80) {
          shiftRegister = 0x10
          writeControl(control or 0x0C)
        } else {
          val complete = shiftRegister and 1 == 1
          shiftRegister = shiftRegister shr 1
          shiftRegister = shiftRegister or ((value and 1) shl 4)
          if (complete) {
            // write register
            when {
              address <= 0x9FFF -> writeControl(shiftRegister)
              address <= 0xBFFF -> {
                // CHR bank 0 (internal, $A000-$BFFF)
                chrBank0 = shiftRegister
                updateOffsets()
              }
              address <= 0xDFFF -> {
                // CHR bank 1 (internal, $C000-$DFFF)
                chrBank1 = shiftRegister
                updateOffsets()
              }
              address <= 0xFFFF -> {
                // PRG bank (internal, $E000-$FFFF)
                prgBank = shiftRegister and 0x0F
                updateOffsets()
              }
            }
            shiftRegister = 0x10
          }
        }
      }
      address >= 0x6000 -> {
        sram[address - 0x6000] = value
      }
      else -> throw RuntimeException("unhandled mapper1 write at address: $address")
    }
  }

  override fun restoreState(serializedState: String) {
    val state = StatePersistence.restoreState(serializedState)
    shiftRegister = state.next()
    control = state.next()
    prgMode = state.next()
    chrMode = state.next()
    prgBank = state.next()
    chrBank0 = state.next()
    chrBank1 = state.next()
    prgOffsets = state.next()
    chrOffsets = state.next()
  }

  override fun dumpState(): String {
    return StatePersistence.dumpState(
        shiftRegister,
        control,
        prgMode,
        chrMode,
        prgBank,
        chrBank0,
        chrBank1,
        prgOffsets,
        chrOffsets
    ).also { println("MMC1 state saved") }
  }

  private fun writeControl(value: Int) {
    control = value
    chrMode = (value shr 4) and 1 and 0xFF
    prgMode = (value shr 2) and 3 and 0xFF
    when (value and 3) {
      0 -> cartridge.mirror = MirrorSingle0
      1 -> cartridge.mirror = MirrorSingle1
      2 -> cartridge.mirror = MirrorVertical
      3 -> cartridge.mirror = MirrorHorizontal
    }
    updateOffsets()
  }

  // PRG ROM bank mode (0, 1: switch 32 KB at $8000, ignoring low bit of bank number;
  //                    2: fix first bank at $8000 and switch 16 KB bank at $C000;
  //                    3: fix last bank at $C000 and switch 16 KB bank at $8000)
  // CHR ROM bank mode (0: switch 8 KB at a time; 1: switch two separate 4 KB banks)
  private fun updateOffsets() {
    when (prgMode) {
      0, 1 -> {
        prgOffsets[0] = prgBankOffset(prgBank and 0xFE)
        prgOffsets[1] = prgBankOffset(prgBank or 0x01)
      }
      2 -> {
        prgOffsets[0] = 0
        prgOffsets[1] = prgBankOffset(prgBank)
      }
      3 -> {
        prgOffsets[0] = prgBankOffset(prgBank)
        prgOffsets[1] = prgBankOffset(-1)
      }
    }
    when (chrMode) {
      0 -> {
        chrOffsets[0] = chrBankOffset(chrBank0 and 0xFE)
        chrOffsets[1] = chrBankOffset(chrBank0 or 0x01)
      }
      1 -> {
        chrOffsets[0] = chrBankOffset(chrBank0)
        chrOffsets[1] = chrBankOffset(chrBank1)
      }
    }
  }

  private fun prgBankOffset(index: Int): Int {
    var idx = index
    if (idx >= 0x80) {
      idx -= 0x100
    }
    idx %= (prg.size / 0x4000)
    var offset = idx * 0x4000
    if (offset < 0) {
      offset += prg.size
    }
    return offset
  }

  private fun chrBankOffset(index: Int): Int {
    var idx = index
    if (idx >= 0x80) {
      idx -= 0x100
    }
    idx %= (chr.size / 0x1000)
    var offset = idx * 0x1000
    if (offset < 0) {
      offset += chr.size
    }
    return offset
  }

  override fun step() {
  }
}