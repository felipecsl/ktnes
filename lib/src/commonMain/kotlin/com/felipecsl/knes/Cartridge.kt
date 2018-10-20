package com.felipecsl.knes

internal data class Cartridge(
    internal var prg: IntArray,                       // PRG-ROM banks
    internal var chr: IntArray,                       // CHR-ROM banks
    internal var mapper: Int,                        // mapper type
    internal var mirror: Int,                         // mirroring mode
    internal var battery: Int,                        // battery present
    internal var sram: IntArray = IntArray(0x2000)    // Save RAM
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

  fun dumpState(): String {
    return StatePersistence.dumpState(
        prg, chr, mapper, mirror, battery, sram
    ).also { println("Cartridge state saved") }
  }

  fun restoreState(serializedState: String) {
    val state = StatePersistence.restoreState(serializedState)
    prg = state.next()
    chr = state.next()
    mapper = state.next()
    mirror = state.next()
    battery = state.next()
    sram = state.next()
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