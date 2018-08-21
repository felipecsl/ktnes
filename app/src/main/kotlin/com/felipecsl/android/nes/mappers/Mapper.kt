package com.felipecsl.android.nes.mappers

import com.felipecsl.android.CPU
import com.felipecsl.android.nes.Cartridge
import com.felipecsl.android.nes.PPU

interface Mapper {
  fun read(address: Int): Int
  fun write(address: Int, value: Int)
  fun step()

  companion object {
    fun newMapper(
        cartridge: Cartridge,
        ppu: PPU,
        cpu: CPU,
        stepCallback: MMC3.StepCallback?
    ): Mapper =
        when (cartridge.mapper) {
          4 -> MMC3(cartridge, ppu, cpu, stepCallback)
          else -> throw NotImplementedError("Mapper ${cartridge.mapper} not implemented")
        }
  }
}