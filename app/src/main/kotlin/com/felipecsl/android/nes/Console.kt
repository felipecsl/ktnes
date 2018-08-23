package com.felipecsl.android.nes

import com.felipecsl.android.CPU
import com.felipecsl.android.NesGLSurfaceView
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
    val surfaceView: NesGLSurfaceView,
    val ram: IntArray = IntArray(2048)
) {
  fun step(): Long {
    val cpuCycles = cpu.step()
    for (it in 0 until cpuCycles * 3) {
      ppu.step()
      mapper.step()
    }
    for (it in 0 until cpuCycles) {
      apu.step()
    }
    surfaceView.setTexture(ppu.front)
    return cpuCycles
  }

  fun reset() {
    cpu.reset()
  }

  companion object {
    fun newConsole(
        cartridge: Cartridge,
        surfaceView: NesGLSurfaceView,
        ppu: PPU = PPU(),
        apu: APU = APU(),
        cpu: CPU = CPU(),
        mapperStepCallback: MMC3.StepCallback? = null,
        mapper: Mapper = Mapper.newMapper(cartridge, ppu, cpu, mapperStepCallback)
    ): Console {
      val console = Console(cpu, apu, ppu, cartridge, Controller(), Controller(), mapper, surfaceView)
      ppu.console = console
      cpu.console = console
      return console
    }
  }
}

