package com.felipecsl.knes

import com.felipecsl.knes.CPU.Companion.FREQUENCY
import kotlin.math.roundToInt

class Director(
      cartridgeData: ByteArray,
      sprite: Sprite,
      mapperCallback: MapperStepCallback? = null,
      cpuCallback: CPUStepCallback? = null,
      ppuCallback: PPUStepCallback? = null
  ) {
  private val cartridge = INESFileParser.parseCartridge(ByteArrayInputStream(cartridgeData))
  private val console = Console.newConsole(
      cartridge, sprite, ::Bitmap, mapperCallback, cpuCallback, ppuCallback)

  init {
    console.reset()
  }

  fun startConsole() {
    var totalCycles = 0L
    var startTime = currentTimeMs()
    while (true) {
      totalCycles += console.step()
      if (totalCycles >= FREQUENCY) {
        val secondsSpent = (currentTimeMs() - startTime) / 1000L
        val clock = totalCycles / secondsSpent
        val speed = (clock / FREQUENCY.toFloat()) * 100F
        println("Clock=${clock}Hz (${speed.roundToInt()}% speed)")
        totalCycles = 0
        startTime = currentTimeMs()
      }
    }
  }
}