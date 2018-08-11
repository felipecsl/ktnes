package android.emu6502.nes

import android.emu6502.shl
import android.emu6502.shr
import android.media.Image
import android.system.Os.read
import kotlin.experimental.and
import kotlin.experimental.xor

class PPU(
    val console:                Console,
    // @formatter:off

    var cycle:                  Int = 0,            // 0-340
    var scanLine:               Int = 0,            // 0-261, 0-239=visible, 240=post, 241-260=vblank, 261=pre
    var frame:                  Int = 0,            // frame counter

    // storage variables
    var paletteData:            ByteArray = ByteArray(32),
    var nameTableData:          ByteArray = ByteArray(2048),
    var oamData:                ByteArray = ByteArray(256),
    var front:                  Image,
    var back:                   Image,

    // PPU registers
    var v:                      Int = 0,            // current vram address (15 bit)
    var t:                      Int = 0,            // temporary vram address (15 bit)
    var x:                      Byte = 0,           // fine x scroll (3 bit)
    var w:                      Byte = 0,           // write toggle (1 bit)
    var f:                      Byte = 0,           // even/odd frame flag (1 bit)
    var register:               Byte = 0,

    var nmiOccurred:            Boolean = false,
    var nmiOutput:              Boolean = false,
    var nmiPrevious:            Boolean = false,
    var nmiDelay:               Byte = 0,

    // background temporary variables
    var nameTableByte:          Byte = 0,
    var attributeTableByte:     Byte = 0,
    var lowTileByte:            Byte = 0,
    var highTileByte:           Byte = 0,
    var tileData:               Int = 0,

    // $2000 PPUCTRL
    var flagNameTable:          Byte = 0, // 0: $2000; 1: $2400; 2: $2800; 3: $2C00
    var flagIncrement:          Byte = 0, // 0: add 1; 1: add 32
    var flagSpriteTable:        Byte = 0, // 0: $0000; 1: $1000; ignored in 8x16 mode
    var flagBackgroundTable:    Byte = 0, // 0: $0000; 1: $1000
    var flagSpriteSize:         Byte = 0, // 0: 8x8; 1: 8x16
    var flagMasterSlave:        Byte = 0, // 0: read EXT; 1: write EXT

    // $2001 PPUMASK
    var flagGrayscale:          Byte = 0, // 0: color; 1: grayscale
    var flagShowLeftBackground: Byte = 0, // 0: hide; 1: show
    var flagShowLeftSprites:    Byte = 0, // 0: hide; 1: show
    var flagShowBackground:     Byte = 0, // 0: hide; 1: show
    var flagShowSprites:        Byte = 0, // 0: hide; 1: show
    var flagRedTint:            Byte = 0, // 0: normal; 1: emphasized
    var flagGreenTint:          Byte = 0, // 0: normal; 1: emphasized
    var flagBlueTint:           Byte = 0, // 0: normal; 1: emphasized

    // $2002 PPUSTATUS
    val flagSpriteZeroHit:      Boolean = false,
    val flagSpriteOverflow:     Boolean = false,

    // $2003 OAMADDR
    var oamAddress:             Byte = 0,

    // $2007 PPUDATA
    val bufferedData:           Byte = 0 // for buffered reads

    // @formatter:on
) {
  fun writeRegister(address: Int, value: Byte) {
    register = value
    when (address) {
      0x2000 -> writeControl(value)
      0x2001 -> writeMask(value)
      0x2003 -> writeOAMAddress(value)
      0x2004 -> writeOAMData(value)
      0x2005 -> writeScroll(value)
      0x2006 -> writeAddress(value)
      0x2007 -> writeData(value)
      0x4014 -> writeDMA(value)
    }
  }

  private fun writeDMA(value: Byte) {
    TODO()
  }

  private fun step() {
    tick()
    val renderingEnabled = flagShowBackground != 0.toByte() || flagShowSprites != 0.toByte()
    val preLine = scanLine == 261
    val visibleLine = scanLine < 240
    // postLine = scanLine == 240
    val renderLine = preLine || visibleLine
    val preFetchCycle = cycle in 321..336
    val visibleCycle = cycle in 1..256
    val fetchCycle = preFetchCycle || visibleCycle
  }

  private fun tick() {
    if (nmiDelay > 0) {
      nmiDelay--
      if (nmiDelay == 0.toByte() && nmiOutput && nmiOccurred) {
        console.cpu.triggerNMI()
      }
    }

    if (flagShowBackground != 0.toByte() || flagShowSprites != 0.toByte()) {
      if (f == 1.toByte() && scanLine == 261 && cycle == 339) {
        cycle = 0
        scanLine = 0
        frame++
        f = f xor 1
        return
      }
    }
    cycle++
    if (cycle > 340) {
      cycle = 0
      scanLine++
      if (scanLine > 261) {
        scanLine = 0
        frame++
        f = f xor 1
      }
    }
  }

  private fun writeData(value: Byte) {
    TODO("not implemented")
  }

  // $2006: PPUADDR
  private fun writeAddress(value: Byte) {
    if (w == 0.toByte()) {
      // t: ..FEDCBA ........ = d: ..FEDCBA
      // t: .X...... ........ = 0
      // w:                   = 1
      t = (t and 0x80FF) or ((value and 0x3F) shl 8).toInt()
      w = 1
    } else {
      // t: ........ HGFEDCBA = d: HGFEDCBA
      // v                    = t
      // w:                   = 0
      t = (t and 0xFF00) or value.toInt()
      v = t
      w = 0
    }
  }

  // $2005: PPUSCROLL
  private fun writeScroll(value: Byte) {
    TODO("not implemented")
  }

  // $2004: OAMDATA (write)
  private fun writeOAMData(value: Byte) {
    oamData[oamAddress.toInt()] = value
    oamAddress++
  }

  // $2003: OAMADDR
  private fun writeOAMAddress(value: Byte) {
    oamAddress = value
  }

  private fun writeMask(value: Byte) {
    flagGrayscale = (value shr 0) and 1
    flagShowLeftBackground = (value shr 1) and 1
    flagShowLeftSprites = (value shr 2) and 1
    flagShowBackground = (value shr 3) and 1
    flagShowSprites = (value shr 4) and 1
    flagRedTint = (value shr 5) and 1
    flagGreenTint = (value shr 6) and 1
    flagBlueTint = (value shr 7) and 1
  }

  // $2000: PPUCTRL
  private fun writeControl(value: Byte) {
    flagNameTable = (value shr 0) and 3
    flagIncrement = (value shr 2) and 1
    flagSpriteTable = (value shr 3) and 1
    flagBackgroundTable = (value shr 4) and 1
    flagSpriteSize = (value shr 5) and 1
    flagMasterSlave = (value shr 6) and 1
    nmiOutput = (value shr 7) and 1 == 1.toByte()
    nmiChange()
    // t: ....BA.. ........ = d: ......BA
    t = (t and 0xF3FF) or ((value and 0x03) shl 10).toInt()
  }

  private fun nmiChange() {
    val nmi = nmiOutput && nmiOccurred
    if (nmi && !nmiPrevious) {
      // TODO: this fixes some games but the delay shouldn't have to be so
      // long, so the timings are off somewhere
      nmiDelay = 15
    }
    nmiPrevious = nmi
  }

  private fun fetchNameTableByte() {
    val address = 0x2000 or (v and 0x0FFF)
    nameTableByte = read(address)
  }
}