package com.felipecsl.knes

import com.felipecsl.knes.CPU.Companion.FREQUENCY
import kotlin.math.roundToInt

object Director {
  fun startConsole(
      cartridgeData: ByteArray,
      sprite: Sprite,
      mapperCallback: MapperStepCallback? = null,
      cpuCallback: CPUStepCallback? = null,
      ppuCallback: PPUStepCallback? = null
  ) {
    val cartridge = INESFileParser.parseCartridge(ByteArrayInputStream(cartridgeData))
    val console = Console.newConsole(
        cartridge, sprite, ::Bitmap, mapperCallback, cpuCallback, ppuCallback)
    console.reset()
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