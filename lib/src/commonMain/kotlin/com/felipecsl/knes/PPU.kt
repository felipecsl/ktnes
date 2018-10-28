package com.felipecsl.knes

internal class PPU(
    cartridge: Cartridge,
    private val stepCallback: PPUStepCallback? = null,
    private var cycle: Int = 0,            // 0-340
    private var scanLine: Int = 0,         // 0-261, 0-239=visible, 240=post, 241-260=vblank, 261=pre
    private var frame: Int = 0,            // frame counter

    // storage variables
    private var paletteData: IntArray = IntArray(32),
    private var nameTableData: IntArray = IntArray(2048), // ByteArray
    private var oamData: IntArray = IntArray(256),
    internal var front: IntArray = IntArray(IMG_WIDTH * IMG_HEIGHT),
    private var back: IntArray = IntArray(IMG_WIDTH * IMG_HEIGHT),

    // PPU registers
    private var v: Int = 0,            // current vram address (15 bit)
    private var t: Int = 0,            // temporary vram address (15 bit)
    private var x: Int = 0,           // fine x scroll (3 bit)
    private var w: Int = 0,           // write toggle (1 bit)
    private var f: Int = 0,           // even/odd frame flag (1 bit)
    private var register: Int = 0,

    private var nmiOccurred: Boolean = false,
    private var nmiOutput: Boolean = false,
    private var nmiPrevious: Boolean = false,
    private var nmiDelay: Int = 0,

    // background temporary variables
    private var nameTableByte: Int = 0,       // Byte
    private var attributeTableByte: Int = 0,  // Byte
    private var lowTileByte: Int = 0,         // Byte
    private var highTileByte: Int = 0,        // Byte
    private var tileData: Long = 0L,          // uint64

    // sprite temporary variables
    private var spriteCount: Int = 0,
    private var spritePatterns: IntArray = IntArray(8),
    private var spritePositions: IntArray = IntArray(8),  // ByteArray
    private var spritePriorities: IntArray = IntArray(8), // ByteArray
    private var spriteIndexes: IntArray = IntArray(8),    // ByteArray

    // $2000 PPUCTRL
    private var flagNameTable: Int = 0, // 0: $2000; 1: $2400; 2: $2800; 3: $2C00
    private var flagIncrement: Int = 0, // 0: add 1; 1: add 32
    private var flagSpriteTable: Int = 0, // 0: $0000; 1: $1000; ignored in 8x16 mode
    private var flagBackgroundTable: Int = 0, // 0: $0000; 1: $1000
    private var flagSpriteSize: Int = 0, // 0: 8x8; 1: 8x16
    private var flagMasterSlave: Int = 0, // 0: read EXT; 1: write EXT

    // $2001 PPUMASK
    private var flagGrayscale: Int = 0, // 0: color; 1: grayscale
    private var flagShowLeftBackground: Int = 0, // 0: hide; 1: show
    private var flagShowLeftSprites: Int = 0, // 0: hide; 1: show
    private var flagShowBackground: Int = 0, // 0: hide; 1: show
    private var flagShowSprites: Int = 0, // 0: hide; 1: show
    private var flagRedTint: Int = 0, // 0: normal; 1: emphasized
    private var flagGreenTint: Int = 0, // 0: normal; 1: emphasized
    private var flagBlueTint: Int = 0, // 0: normal; 1: emphasized

    // $2002 PPUSTATUS
    private var flagSpriteZeroHit: Int = 0,
    private var flagSpriteOverflow: Int = 0,

    // $2003 OAMADDR
    private var oamAddress: Int = 0,

    // $2007 PPUDATA
    private var bufferedData: Int = 0, // for buffered reads

    private val zeroTo255: IntRange = 0..255,
    private val zeroTo63: IntRange = 0..63,
    private val zeroTo7: IntRange = 0..7,
    private var mirror: Int = cartridge.mirror
) {
  lateinit var cpu: CPU
  lateinit var mapper: Mapper
  internal var isMMC3: Boolean = false
  internal var isNoOpMapper: Boolean = false

  init {
    reset()
  }

  fun readRegister(address: Int): Int {
    return when (address) {
      0x2002 -> {
        // read status
        var result = register and 0x1F
        result = result or (flagSpriteOverflow shl 5)
        result = result or (flagSpriteZeroHit shl 6)
        if (nmiOccurred) {
          result = result or (1 shl 7)
        }
        nmiOccurred = false
        nmiChange()
        w = 0
        result
      }
      // read oam data
      0x2004 -> oamData[oamAddress]
      0x2007 -> {
        // read data
        var value = read(v)
        if (v % 0x4000 < 0x3F00) {
          val buffered = bufferedData
          bufferedData = value
          value = buffered
        } else {
          bufferedData = read(v - 0x1000)
        }
        v += if (flagIncrement == 0) 1 else 32
        value
      }
      else -> 0
    }
  }

  fun writeRegister(address: Int, value: Int) {
    register = value
    when (address) {
      0x2000 -> writeControl(value)
      0x2001 -> writeMask(value)
      // write oam address
      0x2003 -> oamAddress = value
      // write oam data
      0x2004 -> oamData[oamAddress++] = value
      0x2005 -> {
        // write scroll
        if (w == 0) {
          // t: ........ ...HGFED = d: HGFED...
          // x:               CBA = d: .....CBA
          // w:                   = 1
          t = (t and 0xFFE0) or (value ushr 3)
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
      0x2006 -> {
        // write address
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
      0x2007 -> {
        // write data
        write(v, value)
        v += if (flagIncrement == 0) 1 else 32
      }
      0x4014 -> {
        // write dma
        var address1 = value shl 8
        for (it in zeroTo255) {
          oamData[oamAddress] = cpu.read(address1)
          oamAddress = (oamAddress + 1) and 0xFF
          address1++
        }
        cpu.stall += 513
        if (cpu.cycles % 2.0 == 1.0) {
          cpu.stall++
        }
      }
    }
  }

  fun dumpState(): String {
    return StatePersistence.dumpState(
        cycle, scanLine, frame, paletteData, nameTableData,
        oamData, v, t, x, w, f, register, nmiOccurred, nmiOutput, nmiPrevious,
        nmiDelay, nameTableByte, attributeTableByte, lowTileByte, highTileByte, tileData,
        spriteCount, spritePatterns, spritePositions,
        spritePriorities, spriteIndexes, flagNameTable, flagIncrement,
        flagSpriteTable, flagBackgroundTable, flagSpriteSize, flagMasterSlave, flagGrayscale,
        flagShowLeftBackground, flagShowLeftSprites, flagShowBackground, flagShowSprites,
        flagRedTint, flagGreenTint, flagBlueTint, flagSpriteZeroHit, flagSpriteOverflow,
        oamAddress, bufferedData, mirror
    ).also { println("PPU state saved") }
  }

  fun restoreState(serializedState: String) {
    val state = StatePersistence.restoreState(serializedState)
    cycle = state.next()
    scanLine = state.next()
    frame = state.next()
    paletteData = state.next()
    nameTableData = state.next()
    oamData = state.next()
    v = state.next()
    t = state.next()
    x = state.next()
    w = state.next()
    f = state.next()
    register = state.next()
    nmiOccurred = state.next()
    nmiOutput = state.next()
    nmiPrevious = state.next()
    nmiDelay = state.next()
    nameTableByte = state.next()
    attributeTableByte = state.next()
    lowTileByte = state.next()
    highTileByte = state.next()
    tileData = state.next()
    spriteCount = state.next()
    spritePatterns = state.next()
    spritePositions = state.next()
    spritePriorities = state.next()
    spriteIndexes = state.next()
    flagNameTable = state.next()
    flagIncrement = state.next()
    flagSpriteTable = state.next()
    flagBackgroundTable = state.next()
    flagSpriteSize = state.next()
    flagMasterSlave = state.next()
    flagGrayscale = state.next()
    flagShowLeftBackground = state.next()
    flagShowLeftSprites = state.next()
    flagShowBackground = state.next()
    flagShowSprites = state.next()
    flagRedTint = state.next()
    flagGreenTint = state.next()
    flagBlueTint = state.next()
    flagSpriteZeroHit = state.next()
    flagSpriteOverflow = state.next()
    oamAddress = state.next()
    bufferedData = state.next()
    mirror = state.next()
    println("PPU state restored")
  }

  fun step(): Boolean {
//    stepCallback?.step(cycle, scanLine, frame, paletteData, nameTableData, oamData, v, t, x, w, f,
//        register, nmiOccurred, nmiOutput, nmiPrevious, nmiDelay, nameTableByte, attributeTableByte,
//        lowTileByte, highTileByte, tileData, spriteCount, spritePatterns, spritePositions, spritePriorities,
//        spriteIndexes, flagNameTable, flagIncrement, flagSpriteTable, flagBackgroundTable,
//        flagSpriteSize, flagMasterSlave, flagGrayscale, flagShowLeftBackground, flagShowLeftSprites,
//        flagShowBackground, flagShowSprites, flagRedTint, flagGreenTint, flagBlueTint,
//        flagSpriteZeroHit, flagSpriteOverflow, oamAddress, bufferedData)
    // tick()
    var tickDone = false
    if (nmiDelay > 0) {
      nmiDelay--
      if (nmiDelay == 0 && nmiOutput && nmiOccurred) {
        // trigger NMI causes a non-maskable interrupt to occur on the next cycle
        cpu.interrupt = Interrupt.NMI
      }
    }
    if (flagShowBackground != 0 || flagShowSprites != 0) {
      if (f == 1 && scanLine == 261 && cycle == 339) {
        cycle = 0
        scanLine = 0
        frame++
        f = f xor 1
        tickDone = true
      }
    }
    if (!tickDone) {
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
    val renderingEnabled = flagShowBackground != 0 || flagShowSprites != 0
    val preLine = scanLine == 261
    val visibleLine = scanLine < 240
    // postLine = scanLine == 240
    val renderLine = preLine || visibleLine
    val preFetchCycle = 321 <= cycle && cycle <= 336
    val visibleCycle = 1 <= cycle && cycle <= 256
    val fetchCycle = preFetchCycle || visibleCycle
    if (renderingEnabled) {
      if (visibleLine && visibleCycle) {
        // render pixel
        val x1 = cycle - 1
        val y = scanLine
        var background /* Byte */ = if (flagShowBackground == 0) {
          0
        } else {
          tileData.ushr(32).toInt().ushr((7 - this.x) * 4).and(0x0F)
        }
        var spritePixelI = 0
        var spritePixelSprite = 0
        // sprite pixel
        if (flagShowSprites == 0) {
          spritePixelI = 0
          spritePixelSprite = 0
        } else {
          var spritePixelDone = false
          for (i in 0 until spriteCount) {
            var offset = (cycle - 1) - spritePositions[i]
            if (offset < 0 || offset > 7) {
              continue
            }
            offset = 7 - offset
            val color = (spritePatterns[i] ushr ((offset * 4) and 0xFF)) and 0x0F
            if (color % 4 == 0) {
              continue
            }
            spritePixelI = i
            spritePixelSprite = color
            spritePixelDone = true
          }
          if (!spritePixelDone) {
            spritePixelI = 0
            spritePixelSprite = 0
          }
        }
        if (x1 < 8 && flagShowLeftBackground == 0) {
          background = 0
        }
        if (x1 < 8 && flagShowLeftSprites == 0) {
          spritePixelSprite = 0
        }
        val b = background % 4 != 0
        val s = spritePixelSprite % 4 != 0
        val color: Int /* Byte */ = if (!b && !s) {
          0
        } else if (!b && s) {
          spritePixelSprite or 0x10
        } else if (b && !s) {
          background
        } else {
          if (spriteIndexes[spritePixelI] == 0 && x1 < 255) {
            flagSpriteZeroHit = 1
          }
          if (spritePriorities[spritePixelI] == 0) {
            spritePixelSprite or 0x10
          } else {
            background
          }
        }
        back[y * IMG_WIDTH + x1] =
            PALETTE[paletteData[if (color >= 16 && color % 4 == 0) color - 16 else color] % 64]
      }
      if (renderLine && fetchCycle) {
        tileData = tileData shl 4
        when (cycle % 8) {
          1 -> {
            // fetch name table byte
            nameTableByte = (read(0x2000 or (v and 0x0FFF))) and 0xFF
          }
          3 -> {
            // fetch attribute table byte
            val address = 0x23C0 or (v and 0x0C00) or ((v ushr 4) and 0x38) or ((v ushr 2) and 0x07)
            val shift = ((v ushr 4) and 4) or (v and 2)
            attributeTableByte = ((read(address) ushr shift) and 3) shl 2
          }
          5 -> {
            // fetch low tile byte
            val fineY = (v ushr 12) and 7
            val table = flagBackgroundTable
            val tile = nameTableByte
            val address = 0x1000 * table + tile * 16 + fineY
            lowTileByte = read(address)
          }
          7 -> {
            // fetch high tile byte
            highTileByte = read(
                0x1000 * flagBackgroundTable + nameTableByte * 16 + ((v ushr 12) and 7) + 8)
          }
          0 -> {
            // store tile data
            var data = 0L
            for (i in zeroTo7) {
              val a = attributeTableByte
              val p1 = (lowTileByte and 0x80) ushr 7
              val p2 = (highTileByte and 0x80) ushr 6
              lowTileByte = (lowTileByte shl 1) and 0xFF
              highTileByte = (highTileByte shl 1) and 0xFF
              data = data shl 4
              data = data or (a or p1 or p2).toLong()
            }
            tileData = tileData or data
          }
        }
      }
      if (preLine && cycle >= 280 && cycle <= 304) {
        // copy y
        v = (v and 0x841F) or (t and 0x7BE0)
      }
      if (renderLine) {
        if (fetchCycle && cycle % 8 == 0) {
          // increment x
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
        if (cycle == 256) {
          // increment y
          if (v and 0x7000 != 0x7000) {
            // increment fine Y
            v += 0x1000
          } else {
            // fine Y = 0
            v = v and 0x8FFF
            // let y = coarse Y
            var y = (v and 0x03E0) ushr 5
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
        if (cycle == 257) {
          // copy x
          v = (v and 0xFBE0) or (t and 0x041F)
        }
      }
    }

    // sprite logic
    if (renderingEnabled) {
      if (cycle == 257) {
        if (visibleLine) {
          // evaluate sprites
          val h = if (flagSpriteSize == 0) 8 else 16
          var count = 0
          for (i in zeroTo63) {
            val y = oamData[i * 4 + 0]
            val a = oamData[i * 4 + 2]
            val x = oamData[i * 4 + 3]
            var row = scanLine - y
            if (row < 0 || row >= h) {
              continue
            }
            if (count < 8) {
              var tile = oamData[i * 4 + 1]
              val attributes: Int = oamData[i * 4 + 2] and 0xFF
              val address: Int
              if (flagSpriteSize == 0) {
                if (attributes and 0x80 == 0x80) {
                  row = 7 - row
                }
                address = 0x1000 * flagSpriteTable + tile * 16 + row
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
              val a_ = (attributes and 3) shl 2
              var lowTileByte = read(address)
              var highTileByte = read(address + 8)
              var data = 0
              for (it in zeroTo7) {
                val p1: Int
                val p2: Int
                if (attributes and 0x40 == 0x40) {
                  p1 = ((lowTileByte and 1) shl 0)
                  p2 = ((highTileByte and 1) shl 1)
                  lowTileByte = lowTileByte ushr 1
                  highTileByte = highTileByte ushr 1
                } else {
                  p1 = ((lowTileByte and 0x80) ushr 7)
                  p2 = ((highTileByte and 0x80) ushr 6)
                  lowTileByte = lowTileByte shl 1
                  highTileByte = highTileByte shl 1
                }
                data = data shl 4
                data = data or (a_ or p1 or p2)
              }
              spritePatterns[count] = data
              spritePositions[count] = x
              spritePriorities[count] = (a ushr 5) and 1
              spriteIndexes[count] = i and 0xFF
            }
            count++
          }
          if (count > 8) {
            count = 8
            flagSpriteOverflow = 1
          }
          spriteCount = count
        } else {
          spriteCount = 0
        }
      }
    }

    // vblank logic
    if (scanLine == 241 && cycle == 1) {
      // set vertical blank
      val temp = front
      this.front = back
      this.back = temp
      nmiOccurred = true
      nmiChange()
    }
    if (preLine && cycle == 1) {
      // clear vertical blank
      nmiOccurred = false
      nmiChange()
      flagSpriteZeroHit = 0
      flagSpriteOverflow = 0
    }
    // TODO: this *should* be 260
    // Returning false means we need to step the mapper too (TODO move this logic to the mapper)
    return if (isNoOpMapper)
      true
    else if (!isMMC3)
      false
    else cycle != 280
        || 240 <= scanLine && scanLine <= 260
        || (flagShowBackground == 0 && flagShowSprites == 0)
  }

  private fun read(_address: Int): Int /* Byte */ {
    val address = _address % 0x4000
    return when {
      address < 0x2000 -> mapper.read(address)
      address < 0x3F00 -> {
        // mirror address
        val newAddress = (address - 0x2000) % 0x1000
        val mirrorAddr =
            0x2000 + MIRROR_LOOKUP[mirror][newAddress / 0x0400] * 0x0400 + (newAddress % 0x0400)
        nameTableData[mirrorAddr % 2048]
      }
      address < 0x4000 -> {
        val paletteAddress = address % 32
        paletteData[
            if (paletteAddress >= 16 && paletteAddress % 4 == 0)
              paletteAddress - 16
            else
              paletteAddress
        ]
      }
      else -> throw RuntimeException("unhandled PPU memory read at address: $address")
    }
  }

  private fun write(addr: Int, value: Int /* Byte */) {
    val address = addr % 0x4000
    when {
      address < 0x2000 -> mapper.write(address, value)
      address < 0x3F00 -> {
        // mirror address
        val newAddress = (address - 0x2000) % 0x1000
        val mirrorAddr =
            0x2000 + MIRROR_LOOKUP[mirror][newAddress / 0x0400] * 0x0400 + (newAddress % 0x0400)
        nameTableData[mirrorAddr % 2048] = value and 0xFF
      }
      address < 0x4000 -> {
        val paletteAddress = address % 32
        paletteData[
            if (paletteAddress >= 16 && paletteAddress % 4 == 0)
              paletteAddress - 16
            else
              paletteAddress
        ] = value
      }
      else -> throw RuntimeException("unhandled ppu memory write at address: $address")
    }
  }

  private fun writeMask(value: Int) {
    flagGrayscale = (value ushr 0) and 1
    flagShowLeftBackground = (value ushr 1) and 1
    flagShowLeftSprites = (value ushr 2) and 1
    flagShowBackground = (value ushr 3) and 1
    flagShowSprites = (value ushr 4) and 1
    flagRedTint = (value ushr 5) and 1
    flagGreenTint = (value ushr 6) and 1
    flagBlueTint = (value ushr 7) and 1
  }

  // $2000: PPUCTRL
  private fun writeControl(value: Int) {
    flagNameTable = (value ushr 0) and 3
    flagIncrement = (value ushr 2) and 1
    flagSpriteTable = (value ushr 3) and 1
    flagBackgroundTable = (value ushr 4) and 1
    flagSpriteSize = (value ushr 5) and 1
    flagMasterSlave = (value ushr 6) and 1
    nmiOutput = (value ushr 7) and 1 == 1
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

  private fun reset() {
    cycle = 340
    scanLine = 240
    frame = 0
    writeControl(0)
    writeMask(0)
    oamAddress = 0
  }

  companion object {
    const val IMG_WIDTH = 256
    const val IMG_HEIGHT = 240
    private val PALETTE = arrayOf(
        0x666666, 0x002A88, 0x1412A7, 0x3B00A4, 0x5C007E, 0x6E0040, 0x6C0600, 0x561D00,
        0x333500, 0x0B4800, 0x005200, 0x004F08, 0x00404D, 0x000000, 0x000000, 0x000000,
        0xADADAD, 0x155FD9, 0x4240FF, 0x7527FE, 0xA01ACC, 0xB71E7B, 0xB53120, 0x994E00,
        0x6B6D00, 0x388700, 0x0C9300, 0x008F32, 0x007C8D, 0x000000, 0x000000, 0x000000,
        0xFFFEFF, 0x64B0FF, 0x9290FF, 0xC676FF, 0xF36AFF, 0xFE6ECC, 0xFE8170, 0xEA9E22,
        0xBCBE00, 0x88D800, 0x5CE430, 0x45E082, 0x48CDDE, 0x4F4F4F, 0x000000, 0x000000,
        0xFFFEFF, 0xC0DFFF, 0xD3D2FF, 0xE8C8FF, 0xFBC2FF, 0xFEC4EA, 0xFECCC5, 0xF7D8A5,
        0xE4E594, 0xCFEF96, 0xBDF4AB, 0xB3F3CC, 0xB5EBF2, 0xB8B8B8, 0x000000, 0x000000
    )
    private val MIRROR_LOOKUP = arrayOf(
        arrayOf(0, 0, 1, 1),
        arrayOf(0, 1, 0, 1),
        arrayOf(0, 0, 0, 0),
        arrayOf(1, 1, 1, 1),
        arrayOf(0, 1, 2, 3)
    )
  }
}