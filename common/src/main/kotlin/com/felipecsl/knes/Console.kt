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

  fun audioBuffer(): FloatArray {
    return apu.audioBuffer.drain()
  }

  fun reset() {
    cpu.reset()
  }

  fun setButtons(buttons: BooleanArray) {
    cpu.controller1.buttons = buttons
  }

  fun state(): Map<String, String> {
    val cpuState = cpu.dumpState()
    val ppuState = ppu.dumpState()
    val apuState = apu.dumpState()
    return mapOf(
        "cpu" to cpuState,
        "ppu" to ppuState,
        "apu" to apuState
    )
  }

  fun restoreState(state: Map<String, *>) {
    cpu.restoreState(state["cpu"] as String)
    ppu.restoreState(state["ppu"] as String)
    apu.restoreState(state["apu"] as String)
  }

  companion object {
    fun newConsole(
        cartridge: Cartridge,
        mapperCallback: MapperStepCallback? = null,
        cpuCallback: CPUStepCallback? = null,
        ppuCallback: PPUStepCallback? = null,
        apuCallback: APUStepCallback? = null,
        ppu: PPU = PPU(cartridge, ppuCallback),
        controller1: Controller = Controller(),
        controller2: Controller = Controller(),
        apu: APU = APU(apuCallback),
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

