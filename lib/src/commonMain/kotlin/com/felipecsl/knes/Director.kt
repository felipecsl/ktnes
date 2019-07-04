package com.felipecsl.knes

import com.felipecsl.knes.CPU.Companion.FREQUENCY_HZ

const val FPS = 60
const val SECS_PER_FRAME = 1.0 / FPS
const val MS_PER_FRAME = (SECS_PER_FRAME * 1000).toLong()

class Director(
    cartridgeData: ByteArray,
    mapperCallback: MapperStepCallback? = null,
    cpuCallback: CPUStepCallback? = null,
    ppuCallback: PPUStepCallback? = null,
    apuCallback: APUStepCallback? = null
) {
  private var isRunning = false
  private val cartridge = INESFileParser.parseCartridge(ByteArrayInputStream(cartridgeData))
  private val console = Console.newConsole(
      cartridge, mapperCallback, cpuCallback, ppuCallback, apuCallback)
  val controller1 = console.controller1
  val controller2 = console.controller2

  init {
    console.reset()
  }

  fun stepSeconds(seconds: Double, logSpeed: Boolean = false): Long {
    isRunning = true
    val cyclesToRun = seconds * FREQUENCY_HZ
    var totalCycles = 0.0
    val startTime = currentTimeMs()
    while (isRunning && totalCycles < cyclesToRun) {
      totalCycles += console.step()
    }
    if (logSpeed) {
      trackConsoleSpeed(startTime, totalCycles)
    }
    return (currentTimeMs() - startTime).toLong()
  }

  private fun trackConsoleSpeed(startTime: Double, totalCycles: Double): Double {
    val currentTime = currentTimeMs()
    val secondsSpent = (currentTime - startTime) / 1000
    val expectedClock = FREQUENCY_HZ.toFloat()
    val actualClock = totalCycles / secondsSpent
    val relativeSpeed = actualClock / expectedClock
    println("Clock=${actualClock}Hz (${relativeSpeed}x)")
    return currentTime
  }

  fun audioBuffer() = console.audioBuffer()

  fun reset() {
    isRunning = false
    console.reset()
  }

  fun videoBuffer(): IntArray {
    return console.videoBuffer()
  }

  fun pause() {
    isRunning = false
  }

  fun dumpState(): Map<String, String> {
    return console.state()
  }

  fun restoreState(state: Map<String, *>) {
    console.restoreState(state)
  }
}