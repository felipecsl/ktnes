package android.emu6502.nes.mappers

import android.emu6502.CPU
import android.emu6502.nes.Cartridge
import android.emu6502.nes.PPU

interface Mapper {
  fun read(address: Int): Int
  fun write(address: Int, value: Int)
  fun step()

  companion object {
    fun newMapper(cartridge: Cartridge, ppu: PPU, cpu: CPU): Mapper =
        when (cartridge.mapper) {
          4 -> MMC3(cartridge, ppu, cpu)
          else -> throw NotImplementedError("Mapper ${cartridge.mapper} not implemented")
        }
  }
}