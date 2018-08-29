package com.felipecsl.knes

import com.felipecsl.knes.CPU.Companion.FREQUENCY

object Director {
  fun startConsole(
      cartridgeData: ByteArray,
      surface: Surface? = null,
      mapperCallback: MapperStepCallback? = null,
      cpuCallback: CPUStepCallback? = null,
      ppuCallback: PPUStepCallback? = null
  ) {
    val cartridge = INESFileParser.parseCartridge(ByteArrayInputStream(cartridgeData))
    val finalSurface = surface ?: object : Surface {
      override fun setTexture(bitmap: Bitmap) {
        // no-op for now
//        println("setTexture called")
      }
    }
    val console = Console.newConsole(
        cartridge, finalSurface, ::Bitmap, mapperCallback, cpuCallback, ppuCallback)
    console.reset()
    while (true) {
      var totalCycles = 0L
      val startTime = currentTimeMs()
      while (totalCycles < FREQUENCY) {
        totalCycles += console.step()
      }
      val secondsSpent = (currentTimeMs() - startTime) / 1000
      val clockKHz = (totalCycles / secondsSpent) / 1000
      println("Clock=${clockKHz}KHz")
    }
  }
}