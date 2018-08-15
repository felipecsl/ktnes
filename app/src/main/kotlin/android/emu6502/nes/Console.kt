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
    val display: Display,
    val ram: IntArray = IntArray(2048)
) {
  fun step(): Int {
    val cpuCycles = cpu.step()
    val ppuCycles = cpuCycles * 3
    0.until(ppuCycles).forEach {
      ppu.step()
      mapper.step()
    }
    0.until(cpuCycles).forEach {
      apu.step()
    }
    display.setView(ppu.front)
    return cpuCycles
  }

  fun reset() {
    cpu.reset()
  }

  companion object {
    fun newConsole(cartridge: Cartridge, display: Display): Console {
      val ppu = PPU()
      val cpu = CPU()
      val mapper = Mapper.newMapper(cartridge, ppu, cpu)
      val apu = APU()
      val console = Console(cpu, apu, ppu, cartridge, Controller(), Controller(), mapper, display)
      ppu.console = console
      cpu.console = console
      return console
    }
  }
}

