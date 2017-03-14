package android.emu6502.nes

class PPU(
    // @formatter:off

    val cycle:                  Int,            // 0-340
    val scanLine:               Int,            // 0-261, 0-239=visible, 240=post, 241-260=vblank, 261=pre
    val frame:                  Int,            // frame counter

    // PPU registers
    val v:                      Int,            // current vram address (15 bit)
    val t:                      Int,            // temporary vram address (15 bit)
    val x:                      Byte,           // fine x scroll (3 bit)
    val w:                      Byte,           // write toggle (1 bit)
    val f:                      Byte,           // even/odd frame flag (1 bit)
    val register:               Byte,

    // $2000 PPUCTRL
    val flagNameTable:          Boolean = false, // 0: $2000; 1: $2400; 2: $2800; 3: $2C00
    val flagIncrement:          Boolean = false, // 0: add 1; 1: add 32
    val flagSpriteTable:        Boolean = false, // 0: $0000; 1: $1000; ignored in 8x16 mode
    val flagBackgroundTable:    Boolean = false, // 0: $0000; 1: $1000
    val flagSpriteSize:         Boolean = false, // 0: 8x8; 1: 8x16
    val flagMasterSlave:        Boolean = false, // 0: read EXT; 1: write EXT

    // $2001 PPUMASK
    val flagGrayscale:          Boolean = false, // 0: color; 1: grayscale
    val flagShowLeftBackground: Boolean = false, // 0: hide; 1: show
    val flagShowLeftSprites:    Boolean = false, // 0: hide; 1: show
    val flagShowBackground:     Boolean = false, // 0: hide; 1: show
    val flagShowSprites:        Boolean = false, // 0: hide; 1: show
    val flagRedTint:            Boolean = false, // 0: normal; 1: emphasized
    val flagGreenTint:          Boolean = false, // 0: normal; 1: emphasized
    val flagBlueTint:           Boolean = false // 0: normal; 1: emphasized

    // $2002 PPUSTATUS
    val flagSpriteZeroHit:      Boolean = false,
    val flagSpriteOverflow:     Boolean = false,

    // $2003 OAMADDR
    val oamAddress:             Byte = 0,

    // $2007 PPUDATA
    val bufferedData:           Byte = 0 // for buffered reads

    // @formatter:on
)