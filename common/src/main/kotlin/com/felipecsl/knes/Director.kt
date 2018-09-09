package com.felipecsl.knes

import com.felipecsl.knes.CPU.Companion.FREQUENCY

class Director(
      cartridgeData: ByteArray,
      mapperCallback: MapperStepCallback? = null,
      cpuCallback: CPUStepCallback? = null,
      ppuCallback: PPUStepCallback? = null
  ) {
  private val cartridge = INESFileParser.parseCartridge(ByteArrayInputStream(cartridgeData))
  private val console = Console.newConsole(cartridge, mapperCallback, cpuCallback, ppuCallback)

  init {
    console.reset()
  }

  fun run() {
    var startTime = currentTimeMs()
    var totalCycles = 0L
    val step = FREQUENCY
    while (true) {
      totalCycles += console.step()
      if (totalCycles >= step) {
        val currentTime = currentTimeMs()
        val msSpent = currentTime - startTime
        val clock = (totalCycles * 1000) / msSpent
        val speed = clock / FREQUENCY.toFloat()
        println("Clock=" + clock + "Hz (" + speed + "x)")
        totalCycles = 0
        startTime = currentTime
      }
    }
  }

  fun reset() {
    console.reset()
  }

  fun buffer(): IntArray {
    return console.buffer()
  }
}