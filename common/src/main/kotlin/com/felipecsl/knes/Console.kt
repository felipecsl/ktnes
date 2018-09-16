package com.felipecsl.knes

internal class Console(
    private val cpu: CPU,
    private val apu: APU,
    private val ppu: PPU,
    private val mapper: Mapper
) {
  fun step(): Long {
    val cpuCycles = cpu.step()
    for (it in 0 until cpuCycles * 3) {
      if (!ppu.step()) {
        mapper.step()
      }
    }
    for (it in 0 until cpuCycles) {
      apu.step()
    }
    return cpuCycles
  }

  fun buffer(): IntArray {
    return ppu.front
  }

  fun reset() {
    cpu.reset()
  }

  fun setButtons(buttons: BooleanArray) {
    cpu.controller1.buttons = buttons
  }

  companion object {
    fun newConsole(
        cartridge: Cartridge,
        audioSink: AudioSink,
        mapperCallback: MapperStepCallback? = null,
        cpuCallback: CPUStepCallback? = null,
        ppuCallback: PPUStepCallback? = null,
        ppu: PPU = PPU(cartridge, ppuCallback),
        controller1: Controller = Controller(),
        controller2: Controller = Controller(),
        apu: APU = APU(audioSink),
        mapper: Mapper = Mapper.newMapper(cartridge, mapperCallback),
        cpu: CPU = CPU(mapper, ppu, apu, controller1, controller2, IntArray(2048), cpuCallback)
    ): Console {
      val console = Console(cpu, apu, ppu, mapper)
      ppu.cpu = cpu
      ppu.mapper = mapper
      mapper.cpu = cpu
      apu.cpu = cpu
      return console
    }
  }
}

