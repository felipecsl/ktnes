package com.felipecsl.knes

internal interface Mapper {
  fun read(address: Int): Int
  fun write(address: Int, value: Int)
  fun step()

  companion object {
    fun newMapper(
        cartridge: Cartridge,
        cpu: CPU,
        stepCallback: MapperStepCallback?
    ): Mapper =
        when (cartridge.mapper) {
          4 -> MMC3(cartridge, cpu, stepCallback)
          else -> throw NotImplementedError("Mapper ${cartridge.mapper} not implemented")
        }
  }
}