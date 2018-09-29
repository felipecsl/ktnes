package com.felipecsl.knes

import com.felipecsl.knes.CPU.Companion.FREQUENCY

class Director(
      cartridgeData: ByteArray,
      audioSink: AudioSink,
      mapperCallback: MapperStepCallback? = null,
      cpuCallback: CPUStepCallback? = null,
      ppuCallback: PPUStepCallback? = null,
      apuCallback: APUStepCallback? = null
  ) {
  private var isRunning = false
  private val cartridge = INESFileParser.parseCartridge(ByteArrayInputStream(cartridgeData))
  internal val console = Console.newConsole(
      cartridge, audioSink, mapperCallback, cpuCallback, ppuCallback, apuCallback)

  init {
    console.reset()
  }

  fun run() {
    var startTime = currentTimeMs()
    var totalCycles = 0L
    val step = FREQUENCY
    isRunning = true
    while (isRunning) {
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

  fun setButtons1(buttons: BooleanArray) {
    console.setButtons(buttons)
  }

  fun reset() {
    isRunning = false
    console.reset()
  }

  fun buffer(): IntArray {
    return console.buffer()
  }
}