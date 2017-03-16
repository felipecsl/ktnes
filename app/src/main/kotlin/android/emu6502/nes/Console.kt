package android.emu6502.nes

import android.emu6502.CPU
import android.emu6502.Display
import android.emu6502.Memory
import android.emu6502.nes.mappers.Mapper

class Console(
    val cpu: CPU,
    val apu: APU,
    val ppu: PPU,
    val cartridge: Cartridge,
    val controller1: Controller,
    val controller2: Controller,
    val mapper: Mapper,
    val ram: ByteArray = ByteArray(2048)
) {
  fun step() {

  }

  companion object {
    fun newConsole(cartridge: Cartridge, display: Display): Console {
      val ppu = PPU()
      val memory = Memory(display)
      val cpu = CPU(memory)
      val mapper = Mapper.newMapper(cartridge, ppu, cpu)
      val apu = APU()
      return Console(cpu, apu, ppu, cartridge, Controller(), Controller(), mapper)
    }
  }
}

