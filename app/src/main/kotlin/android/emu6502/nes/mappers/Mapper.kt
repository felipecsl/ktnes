package android.emu6502.nes.mappers

import android.emu6502.CPU
import android.emu6502.nes.Cartridge
import android.emu6502.nes.PPU
import java.io.InputStream

interface Mapper {
  fun read(address: Int): Int
  fun write(address: Int, value: Int)
  fun step()

  companion object {
    fun newMapper(cartridge: Cartridge, ppu: PPU, cpu: CPU, mapperReference: InputStream): Mapper =
        when (cartridge.mapper) {
          4 -> MMC3(cartridge, ppu, cpu, mapperReference)
          else -> throw NotImplementedError("Mapper ${cartridge.mapper} not implemented")
        }
  }
}