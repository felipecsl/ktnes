package android.emu6502.nes

import android.emu6502.Display
import android.emu6502.mirrorAddress

class PPU(
    var cycle: Int = 0,            // 0-340
    var scanLine: Int = 0,            // 0-261, 0-239=visible, 240=post, 241-260=vblank, 261=pre
    private var frame: Int = 0,            // frame counter

    // storage variables
    private var paletteData: IntArray = IntArray(32),
    private var nameTableData: IntArray = IntArray(2048),
    private var oamData: IntArray = IntArray(256),
    var front: Array<IntArray>   = Array(Display.NUM_X) { IntArray(Display.NUM_Y) },
    var back: Array<IntArray>    = Array(Display.NUM_X) { IntArray(Display.NUM_Y) },

    // PPU registers
    var v: Int = 0,            // current vram address (15 bit)
    var t: Int = 0,            // temporary vram address (15 bit)
    var x: Int = 0,           // fine x scroll (3 bit)
    var w: Int = 0,           // write toggle (1 bit)
    var f: Int = 0,           // even/odd frame flag (1 bit)
    var register: Int = 0,

    var nmiOccurred: Boolean = false,
    var nmiOutput: Boolean = false,
    var nmiPrevious: Boolean = false,
    var nmiDelay: Int = 0,

    // background temporary variables
    var nameTableByte: Int = 0,
    var attributeTableByte: Int = 0,
    var lowTileByte: Int = 0,
    var highTileByte: Int = 0,
    var tileData: Int = 0,

    // sprite temporary variables
    private var spriteCount: Int = 0,
    var spritePatterns: IntArray = IntArray(8),
    var spritePositions: IntArray = IntArray(8),
    var spritePriorities: IntArray = IntArray(8),
    var spriteIndexes: IntArray = IntArray(8),

    // $2000 PPUCTRL
    var flagNameTable: Int = 0, // 0: $2000; 1: $2400; 2: $2800; 3: $2C00
    var flagIncrement: Int = 0, // 0: add 1; 1: add 32
    var flagSpriteTable: Int = 0, // 0: $0000; 1: $1000; ignored in 8x16 mode
    var flagBackgroundTable: Int = 0, // 0: $0000; 1: $1000
    var flagSpriteSize: Int = 0, // 0: 8x8; 1: 8x16
    var flagMasterSlave: Int = 0, // 0: read EXT; 1: write EXT

    // $2001 PPUMASK
    var flagGrayscale: Int = 0, // 0: color; 1: grayscale
    var flagShowLeftBackground: Int = 0, // 0: hide; 1: show
    var flagShowLeftSprites: Int = 0, // 0: hide; 1: show
    var flagShowBackground: Int = 0, // 0: hide; 1: show
    var flagShowSprites: Int = 0, // 0: hide; 1: show
    var flagRedTint: Int = 0, // 0: normal; 1: emphasized
    var flagGreenTint: Int = 0, // 0: normal; 1: emphasized
    var flagBlueTint: Int = 0, // 0: normal; 1: emphasized

    // $2002 PPUSTATUS
    var flagSpriteZeroHit: Int = 0,
    var flagSpriteOverflow: Int = 0,

    // $2003 OAMADDR
    var oamAddress: Int = 0,

    // $2007 PPUDATA
    var bufferedData: Int = 0 // for buffered reads
) {
  init {
    reset()
  }

  lateinit var console: Console

  fun readRegister(address: Int): Int {
    return when (address) {
      0x2002 -> readStatus()
      0x2004 -> readOAMData()
      0x2007 -> readData()
      else -> 0
    }
  }

  private fun readStatus(): Int {
    var result = register and 0x1F
    result = result or flagSpriteOverflow shl 5
    result = result or flagSpriteZeroHit shl 6
    if (nmiOccurred) {
      result = result or (1 shl 7)
    }
    nmiOccurred = false
    nmiChange()
    // w:                   = 0
    w = 0
    return result
  }

  fun writeRegister(address: Int, value: Int) {
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

  private fun writeDMA(value: Int) {
    val cpu = console.cpu
    var address = value shl 8
    0.until(256).forEach {
      oamData[oamAddress] = cpu.read(address)
      oamAddress++
      address++
    }
    cpu.stall += 513
    if (cpu.cycles % 2 == 1) {
      cpu.stall++
    }
  }

  fun step() {
    tick()
    val renderingEnabled = flagShowBackground != 0 || flagShowSprites != 0
    val preLine = scanLine == 261
    val visibleLine = scanLine < 240
    // postLine = scanLine == 240
    val renderLine = preLine || visibleLine
    val preFetchCycle = cycle in 321..336
    val visibleCycle = cycle in 1..256
    val fetchCycle = preFetchCycle || visibleCycle
    if (renderingEnabled) {
      if (visibleLine && visibleCycle) {
        renderPixel()
      }
      if (renderLine && fetchCycle) {
        tileData = tileData shl 4
        when (cycle % 8) {
          1 -> fetchNameTableByte()
          3 -> fetchAttributeTableByte()
          5 -> fetchLowTileByte()
          7 -> fetchHighTileByte()
          0 -> storeTileData()
        }
      }
      if (preLine && cycle >= 280 && cycle <= 304) {
        copyY()
      }
      if (renderLine) {
        if (fetchCycle && cycle % 8 == 0) {
          incrementX()
        }
        if (cycle == 256) {
          incrementY()
        }
        if (cycle == 257) {
          copyX()
        }
      }
    }

    // sprite logic
    if (renderingEnabled) {
      if (cycle == 257) {
        if (visibleLine) {
          evaluateSprites()
        } else {
          spriteCount = 0
        }
      }
    }

    // vblank logic
    if (scanLine == 241 && cycle == 1) {
      setVerticalBlank()
    }
    if (preLine && cycle == 1) {
      clearVerticalBlank()
      flagSpriteZeroHit = 0
      flagSpriteOverflow = 0
    }
  }

  private fun clearVerticalBlank() {
    nmiOccurred = false
    nmiChange()
  }

  private fun setVerticalBlank() {
    val front = this.front
    val back = this.back
    this.front = back
    this.back = front
    nmiOccurred = true
    nmiChange()
  }

  private fun evaluateSprites() {
    val h = if (flagSpriteSize == 0) 8 else 16
    var count = 0
    0.until(64).forEach { i ->
      val y = oamData[i * 4 + 0]
      val a = oamData[i * 4 + 2]
      val x = oamData[i * 4 + 3]
      val row = scanLine - y
      if (row < 0 || row >= h) {
        // continue
      } else {
        if (count < 8) {
          spritePatterns[count] = fetchSpritePattern(i, row)
          spritePositions[count] = x
          spritePriorities[count] = (a shr 5) and 1
          spriteIndexes[count] = i
        }
        count++
      }
    }
    if (count > 8) {
      count = 8
      flagSpriteOverflow = 1
    }
    spriteCount = count
  }

  private fun fetchSpritePattern(i: Int, _row: Int): Int {
    var tile = oamData[i * 4 + 1]
    val attributes: Int = oamData[i * 4 + 2]
    val address: Int
    var row = _row
    if (flagSpriteSize == 0) {
      if (attributes and 0x80 == 0x80) {
        row = 7 - row
      }
      val table = flagSpriteTable
      address = 0x1000 * table + tile * 16 + row
    } else {
      if (attributes and 0x80 == 0x80) {
        row = 15 - row
      }
      val table = tile and 1
      tile = tile and 0xFE
      if (row > 7) {
        tile++
        row -= 8
      }
      address = 0x1000 * table + tile * 16 + row
    }
    val a = (attributes and 3) shl 2
    var lowTileByte = read(address)
    var highTileByte = read(address + 8)
    var data = 0
    0.until(8).forEach {
      val p1: Int
      val p2: Int
      if (attributes and 0x40 == 0x40) {
        p1 = (lowTileByte and 1) shl 0
        p2 = (highTileByte and 1) shl 1
        lowTileByte = lowTileByte shr 1
        highTileByte = highTileByte shr 1
      } else {
        p1 = (lowTileByte and 0x80) shr 7
        p2 = (highTileByte and 0x80) shr 6
        lowTileByte = lowTileByte shl 1
        highTileByte = highTileByte shl 1
      }
      data = data shl 4
      data = data or (a or p1 or p2)
    }
    return data
  }

  private fun storeTileData() {
    var data = 0
    0.until(8).forEach {
      val a = attributeTableByte
      val p1 = (lowTileByte and 0x80) shr 7
      val p2 = (highTileByte and 0x80) shr 6
      lowTileByte = lowTileByte shl 1
      highTileByte = highTileByte shl 1
      data = data shl 4
      data = data or (a or p1 or p2)
    }
    tileData = tileData or data
  }

  private fun fetchHighTileByte() {
    val fineY = (v shr 12) and 7
    val table = flagBackgroundTable
    val tile = nameTableByte
    val address = 0x1000 * table + tile * 16 + fineY
    highTileByte = read(address + 8)
  }

  private fun fetchLowTileByte() {
    val fineY = (v shr 12) and 7
    val table = flagBackgroundTable
    val tile = nameTableByte
    val address = 0x1000 * table + tile * 16 + fineY
    lowTileByte = read(address)
  }

  private fun read(_address: Int): Int {
    val address = _address % 0x4000
    return when {
      address < 0x2000 -> console.mapper.read(address)
      address < 0x3F00 -> {
        val mode = console.cartridge.mirror
        nameTableData[mirrorAddress(mode, address) % 2048]
      }
      address < 0x4000 -> readPalette(address % 32)
      else -> throw RuntimeException("unhandled PPU memory read at address: $address")
    }
  }

  private fun readPalette(address: Int): Int {
    val addr = if (address >= 16 && address % 4 == 0) address - 16 else address
    return paletteData[addr]
  }

  private fun writePalette(address: Int, value: Int) {
    val addr = if (address >= 16 && address % 4 == 0) address - 16 else address
    paletteData[addr] = value
  }

  private fun write(addr: Int, value: Int) {
    val address = addr % 0x4000
    when {
      address < 0x2000 -> console.mapper.write(address, value)
      address < 0x3F00 -> {
        val mode = console.cartridge.mirror
        nameTableData[mirrorAddress(mode, address) % 2048] = value
      }
      address < 0x4000 -> writePalette(address % 32, value)
      else -> throw RuntimeException("unhandled ppu memory write at address: $address")
    }
  }

  private fun fetchAttributeTableByte() {
    val v = v
    val address = 0x23C0 or (v and 0x0C00) or ((v shr 4) and 0x38) or ((v shr 2) and 0x07)
    val shift = ((v shr 4) and 4) or (v and 2)
    attributeTableByte = ((read(address) shr shift) and 3) shl 2
  }

  private fun copyY() {
    // vert(v) = vert(t)
    // v: .IHGF.ED CBA..... = t: .IHGF.ED CBA.....
    v = (v and 0x841F) or (t and 0x7BE0)
  }

  private fun copyX() {
    // hori(v) = hori(t)
    // v: .....F.. ...EDCBA = t: .....F.. ...EDCBA
    v = (v and 0xFBE0) or (t and 0x041F)
  }

  private fun incrementY() {
    // increment vert(v)
    // if fine Y < 7
    if (v and 0x7000 != 0x7000) {
      // increment fine Y
      v += 0x1000
    } else {
      // fine Y = 0
      v = v and 0x8FFF
      // let y = coarse Y
      var y = (v and 0x03E0) shr 5
      when (y) {
        29 -> {
          // coarse Y = 0
          y = 0
          // switch vertical nametable
          v = v xor 0x0800
        }
        31 -> // coarse Y = 0, nametable not switched
          y = 0
        else -> // increment coarse Y
          y++
      }
      // put coarse Y back into v
      v = (v and 0xFC1F) or (y shl 5)
    }
  }

  private fun incrementX() {
    // increment hori(v)
    // if coarse X == 31
    if (v and 0x001F == 31) {
      // coarse X = 0
      v = v and 0xFFE0
      // switch horizontal nametable
      v = v xor 0x0400
    } else {
      // increment coarse X
      v++
    }
  }

  private fun renderPixel() {
    val x = cycle - 1
    val y = scanLine
    var background = backgroundPixel()
    var (i, sprite) = spritePixel()
    if (x < 8 && flagShowLeftBackground == 0) {
      background = 0
    }
    if (x < 8 && flagShowLeftSprites == 0) {
      sprite = 0
    }
    val b = background % 4 != 0
    val s = sprite % 4 != 0
    val color = if (!b && !s) {
      0
    } else if (!b && s) {
      sprite or 0x10
    } else if (b && !s) {
      background
    } else {
      if (spriteIndexes[i] == 0 && x < 255) {
        flagSpriteZeroHit = 1
      }
      if (spritePriorities[i] == 0) (sprite or 0x10) else background
    }
    back[x][y] = Display.PALETTE[readPalette(color) % 64]
  }

  private fun backgroundPixel(): Int {
    if (flagShowBackground == 0) {
      return 0
    }
    val data = fetchTileData() shr ((7 - x) * 4)
    return (data and 0x0F)
  }

  private fun fetchTileData(): Int {
    return tileData shr 32
  }

  private fun spritePixel(): Pair<Int, Int> {
    if (flagShowSprites == 0) {
      return 0 to 0
    }
    for (i: Int in 0.until(spriteCount)) {
      var offset = (cycle - 1) - spritePositions[i]
      if (offset < 0 || offset > 7) {
        continue
      }
      offset = 7 - offset
      val color = (spritePatterns[i] shr offset * 4) and 0x0F
      if (color % 4 == 0) {
        continue
      }
      return i to color
    }
    return 0 to 0
  }

  private fun tick() {
    if (nmiDelay > 0) {
      nmiDelay--
      if (nmiDelay == 0 && nmiOutput && nmiOccurred) {
        console.cpu.triggerNMI()
      }
    }

    if (flagShowBackground != 0 || flagShowSprites != 0) {
      if (f == 1 && scanLine == 261 && cycle == 339) {
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

  // $2007: PPUDATA (read)
  private fun readData(): Int {
    var value = read(v)
    // emulate buffered reads
    if (v % 0x4000 < 0x3F00) {
      val buffered = bufferedData
      bufferedData = value
      value = buffered
    } else {
      bufferedData = read(v - 0x1000)
    }
    // increment address
    v += if (flagIncrement == 0) 1 else 32
    return value
  }

  // $2007: PPUDATA (write)
  private fun writeData(value: Int) {
    write(v, value)
    v += if (flagIncrement == 0) 1 else 32
  }

  // $2006: PPUADDR
  private fun writeAddress(value: Int) {
    if (w == 0) {
      // t: ..FEDCBA ........ = d: ..FEDCBA
      // t: .X...... ........ = 0
      // w:                   = 1
      t = (t and 0x80FF) or ((value and 0x3F) shl 8)
      w = 1
    } else {
      // t: ........ HGFEDCBA = d: HGFEDCBA
      // v                    = t
      // w:                   = 0
      t = (t and 0xFF00) or value
      v = t
      w = 0
    }
  }

  // $2005: PPUSCROLL
  private fun writeScroll(value: Int) {
    if (w == 0) {
      // t: ........ ...HGFED = d: HGFED...
      // x:               CBA = d: .....CBA
      // w:                   = 1
      t = (t and 0xFFE0) or (value shr 3)
      x = value and 0x07
      w = 1
    } else {
      // t: .CBA..HG FED..... = d: HGFEDCBA
      // w:                   = 0
      t = (t and 0x8FFF) or ((value and 0x07) shl 12)
      t = (t and 0xFC1F) or ((value and 0xF8) shl 2)
      w = 0
    }
  }

  private fun readOAMData(): Int {
    return oamData[oamAddress]
  }

  // $2004: OAMDATA (write)
  private fun writeOAMData(value: Int) {
    oamData[oamAddress] = value
    oamAddress++
  }

  // $2003: OAMADDR
  private fun writeOAMAddress(value: Int) {
    oamAddress = value
  }

  private fun writeMask(value: Int) {
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
  private fun writeControl(value: Int) {
    flagNameTable = (value shr 0) and 3
    flagIncrement = (value shr 2) and 1
    flagSpriteTable = (value shr 3) and 1
    flagBackgroundTable = (value shr 4) and 1
    flagSpriteSize = (value shr 5) and 1
    flagMasterSlave = (value shr 6) and 1
    nmiOutput = (value shr 7) and 1 == 1
    nmiChange()
    // t: ....BA.. ........ = d: ......BA
    t = (t and 0xF3FF) or ((value and 0x03) shl 10)
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

  private fun reset() {
    cycle = 340
    scanLine = 240
    frame = 0
    writeControl(0)
    writeMask(0)
    writeOAMAddress(0)
  }
}