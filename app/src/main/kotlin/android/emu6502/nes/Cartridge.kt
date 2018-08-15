package android.emu6502.nes

import java.util.*

data class Cartridge(
    // @formatter:off
    val prg: Array<Int>,                       // PRG-ROM banks
    val chr: Array<Int>,                       // CHR-ROM banks
    val mapper: Int,                         // mapper type
    var mirror: Int,                          // mirroring mode
    val battery: Int,                        // battery present
    val sram: IntArray = IntArray(0x2000)   // Save RAM
    // @formatter:on
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false

    other as Cartridge

    if (!Arrays.equals(prg, other.prg)) return false
    if (!Arrays.equals(chr, other.chr)) return false
    if (!Arrays.equals(sram, other.sram)) return false
    if (mapper != other.mapper) return false
    if (mirror != other.mirror) return false
    if (battery != other.battery) return false

    return true
  }

  override fun hashCode(): Int {
    var result = Arrays.hashCode(prg)
    result = 31 * result + Arrays.hashCode(chr)
    result = 31 * result + Arrays.hashCode(sram)
    result = 31 * result + mapper
    result = 31 * result + mirror
    result = 31 * result + battery
    return result
  }
}