package com.felipecsl.knes

internal class Console(
    private val cartridge: Cartridge,
    private val cpu: CPU,
    private val apu: APU,
    private val ppu: PPU,
    private val mapper: Mapper,
    val controller1: Controller,
    val controller2: Controller
) {
  fun step(): Double {
    val cpuCycles = cpu.step()
    var i = 0
    while (i++ < cpuCycles * 3) {
      if (!ppu.step()) {
        mapper.step()
      }
    }
    i = 0
    while (i++ < cpuCycles) {
      apu.step()
    }
    return cpuCycles
  }

  fun videoBuffer(): IntArray {
    return ppu.front
  }

  fun audioBuffer(): FloatArray {
    return apu.audioBuffer.drain()
  }

  fun reset() {
    cpu.reset()
  }

  fun state(): Map<String, String> {
    return mapOf(
        "cpu" to cpu.dumpState(),
        "ppu" to ppu.dumpState(),
        "apu" to apu.dumpState(),
        "mapper" to mapper.dumpState(),
        "cartridge" to cartridge.dumpState()
    )
  }

  fun restoreState(state: Map<String, *>) {
    cpu.restoreState(state["cpu"] as String)
    ppu.restoreState(state["ppu"] as String)
    apu.restoreState(state["apu"] as String)
    mapper.restoreState(state["mapper"] as String)
    cartridge.restoreState(state["cartridge"] as String)
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
      val console = Console(cartridge, cpu, apu, ppu, mapper, controller1, controller2)
      ppu.isMMC3 = mapper is MMC3
      ppu.isNoOpMapper = mapper is Mapper2 || mapper is MMC1
      ppu.cpu = cpu
      ppu.mapper = mapper
      mapper.cpu = cpu
      apu.cpu = cpu
      return console
    }
  }
}

