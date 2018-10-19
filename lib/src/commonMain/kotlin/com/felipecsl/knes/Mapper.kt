package com.felipecsl.knes

internal interface Mapper {
  var cpu: CPU
  fun read(address: Int): Int
  fun write(address: Int, value: Int)
  fun step()
  fun restoreState(serializedState: String)
  fun dumpState(): String

  companion object {
    fun newMapper(cartridge: Cartridge, stepCallback: MapperStepCallback?): Mapper =
        when (cartridge.mapper) {
          0 -> Mapper2(cartridge, stepCallback)
          1 -> MMC1(cartridge, stepCallback)
          4 -> MMC3(cartridge, stepCallback)
          else -> throw NotImplementedError("Mapper ${cartridge.mapper} not implemented")
        }
  }
}