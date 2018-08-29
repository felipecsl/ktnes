package com.felipecsl.knes

internal data class Cartridge(
    // @formatter:off
    val prg: Array<Int>,                       // PRG-ROM banks
    val chr: Array<Int>,                       // CHR-ROM banks
    val mapper: Int,                         // mapper_state_reference type
    var mirror: Int,                          // mirroring mode
    val battery: Int,                        // battery present
    val sram: IntArray = IntArray(0x2000)   // Save RAM
    // @formatter:on
) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other === null) return false
    if (other::class != this::class) return false

    other as Cartridge

    if (!prg.contentEquals(other.prg)) return false
    if (!chr.contentEquals(other.chr)) return false
    if (!sram.contentEquals(other.sram)) return false
    if (mapper != other.mapper) return false
    if (mirror != other.mirror) return false
    if (battery != other.battery) return false

    return true
  }

  override fun hashCode(): Int {
    var result = prg.hashCode()
    result = 31 * result + chr.hashCode()
    result = 31 * result + sram.hashCode()
    result = 31 * result + mapper
    result = 31 * result + mirror
    result = 31 * result + battery
    return result
  }
}