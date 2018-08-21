package com.felipecsl.android.nes

import com.felipecsl.android.CPU
import com.felipecsl.android.Display
import com.felipecsl.android.nes.mappers.MMC3
import com.felipecsl.android.nes.mappers.Mapper

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
  fun step(): Long {
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
    fun newConsole(
        cartridge: Cartridge,
        display: Display,
        ppu: PPU = PPU(),
        apu: APU = APU(),
        cpu: CPU = CPU(),
        mapperStepCallback: MMC3.StepCallback? = null,
        mapper: Mapper = Mapper.newMapper(cartridge, ppu, cpu, mapperStepCallback)
    ): Console {
      val console = Console(cpu, apu, ppu, cartridge, Controller(), Controller(), mapper, display)
      ppu.console = console
      cpu.console = console
      return console
    }
  }
}

