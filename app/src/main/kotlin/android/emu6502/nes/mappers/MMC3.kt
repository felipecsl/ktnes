package android.emu6502.nes.mappers

import android.emu6502.CPU
import android.emu6502.nes.Cartridge
import android.emu6502.nes.PPU
import android.emu6502.toHexString

// http://wiki.nesdev.com/w/index.php/MMC3
class MMC3(
    private val cartridge: Cartridge,
    private val ppu: PPU,
    private val cpu: CPU
) : Mapper {
  private var register: Int = 0
  private var registers = IntArray(8)
  private var prgMode: Int = 0
  private var chrMode: Int = 0
  private var prgOffsets = IntArray(4)
  private var chrOffsets = IntArray(8)
  private var reload: Int = 0
  private var counter: Int = 0
  private var irqEnable: Boolean = false

  override fun read(address: Int): Int {
    return when {
      address < 0x2000 -> {
        val bank = address / 0x0400
        val offset = address % 0x0400
        cartridge.chr[chrOffsets[bank] + offset]
      }
      address >= 0x8000 -> {
        val addr = address - 0x8000
        val bank = addr / 0x2000
        val offset = addr % 0x2000
        cartridge.pgr[prgOffsets[bank] + offset]
      }
      address >= 0x6000 -> {
        return cartridge.sram[address - 0x6000]
      }
      else -> throw RuntimeException("unhandled mapper4 read at address: ${address.toHexString()}")
    }
  }

  override fun write(address: Int, value: Int) {
    when {
      address < 0x2000 -> {
        val bank = address / 0x0400
        val offset = address % 0x0400
        cartridge.chr[chrOffsets[bank] + offset] = value
      }
      address >= 0x8000 -> {
        writeRegister(address, value)
      }
      address >= 0x6000 -> {
        cartridge.sram[address - 0x6000] = value
      }
      else -> throw RuntimeException("unhandled mapper4 write at address ${address.toHexString()}")
    }
  }

  private fun writeRegister(address: Int, value: Int) {
    if (address <= 0x9FFF && address % 2 == 0)
      writeBankSelect(value)
    else if (address <= 0x9FFF && address % 2 == 1)
      writeBankData(value)
    else if (address <= 0xBFFF && address % 2 == 0)
      writeMirror(value)
    else if (address <= 0xBFFF && address % 2 == 1)
      writeProtect()
    else if (address <= 0xDFFF && address % 2 == 0)
      writeIRQLatch(value)
    else if (address <= 0xDFFF && address % 2 == 1)
      writeIRQReload()
    else if (address <= 0xFFFF && address % 2 == 0)
      writeIRQDisable()
    else if (address <= 0xFFFF && address % 2 == 1)
      writeIRQEnable()
  }

  private fun writeBankSelect(value: Int) {
    prgMode = (value shr 6) and 1
    chrMode = (value shr 7) and 1
    register = value and 7
    updateOffsets()
  }

  private fun updateOffsets() {
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

  private fun chrBankOffset(index: Int): Int {
    var idx = index
    if (idx >= 0x80) {
      idx -= 0x100
    }
    idx %= cartridge.chr.size / 0x0400
    var offset = idx * 0x0400
    if (offset < 0) {
      offset += cartridge.chr.size
    }
    return offset
  }

  private fun prgBankOffset(index: Int): Int {
    var idx = index
    if (idx >= 0x80) {
      idx -= 0x100
    }
    idx %= cartridge.chr.size / 0x2000
    var offset = idx * 0x2000
    if (offset < 0) {
      offset += cartridge.chr.size
    }
    return offset
  }

  private fun writeBankData(value: Int) {
    registers[register] = value
    updateOffsets()
  }

  private fun writeMirror(value: Int) {
    when (value and 1) {
      0 -> cartridge.mirror = MirrorVertical
      1 -> cartridge.mirror = MirrorHorizontal
    }
  }

  private fun writeProtect() {
  }

  private fun writeIRQLatch(value: Int) {
    reload = value
  }

  private fun writeIRQReload() {
    counter = 0
  }

  private fun writeIRQDisable() {
    irqEnable = false
  }

  private fun writeIRQEnable() {
    irqEnable = true
  }

  override fun step() {
    if (ppu.cycle != 280) { // TODO: this *should* be 260
      return
    }
    if (ppu.scanLine in 240..260) {
      return
    }
    if (!ppu.flagShowBackground && !ppu.flagShowSprites) {
      return
    }
    handleScanLine()
  }

  private fun handleScanLine() {
    if (counter == 0) {
      counter = reload
    } else {
      counter--
      if (counter == 0 && irqEnable) {
        cpu.triggerIRQ()
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