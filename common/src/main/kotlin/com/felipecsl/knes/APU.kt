package com.felipecsl.knes

internal class APU(
//    private val channel: Float,
    private val sampleRate: Double = 0.0,
    private val pulse1: Pulse = Pulse(channel = 1),
    private val pulse2: Pulse = Pulse(channel = 2),
    private val triangle: Triangle = Triangle(),
    private val noise: Noise = Noise(shiftRegister = 1),
    private var cycle: Long = 0,
    private var framePeriod: Int = 0, // Byte
    private var frameValue: Int = 0, // Byte
    private var frameIRQ: Boolean = false,
    private val pulseTable: FloatArray = FloatArray(31),
    private val tndTable: FloatArray = FloatArray(203),
    private val filterChain: FilterChain = FilterChain(arrayOf(
        highPassFilter(sampleRate, 90F),
        highPassFilter(sampleRate, 440F),
        lowPassFilter(sampleRate, 14000F)
    ))
) {
  private lateinit var dmc: DMC
  var cpu: CPU? = null
    set(value) {
      field = value
      dmc = DMC(value!!)
    }

  init {
    for (i in 0..32) {
      pulseTable[i] = (95.52 / (8128.0 / i + 100)).toFloat()
    }
    for (i in 0..203) {
      tndTable[i] = (163.67 / (24329.0 / i + 100)).toFloat()
    }
  }

  fun readRegister(address: Int): Int {
    return 0
  }

  fun step() {
    val cycle1 = cycle
    cycle++
    val cycle2 = cycle
    stepTimer()
    val f1 = (cycle1 / FRAME_COUNTER_RATE).toInt()
    val f2 = (cycle2 / FRAME_COUNTER_RATE).toInt()
    if (f1 != f2) {
      stepFrameCounter()
    }
    val s1 = (cycle1 / sampleRate).toInt()
    val s2 = (cycle2 / sampleRate).toInt()
    if (s1 != s2) {
      sendSample()
    }
  }

  private fun sendSample() {
//    val output = filterChain.Step(output())
//    select {
//      case channel <- output:
//      default:
//    }
  }

  private fun output(): Any {
    val p1 = pulse1.output()
    val p2 = pulse2.output()
    val t = triangle.output()
    val n = noise.output()
    val d = dmc.output()
    val pulseOut = pulseTable[p1 + p2]
    val tndOut = tndTable[3 * t + 2 * n + d]
    return pulseOut + tndOut
  }

  // mode 0:    mode 1:       function
  // ---------  -----------  -----------------------------
  //  - - - f    - - - - -    IRQ (if bit 6 is clear)
  //  - l - l    l - l - -    Length counter and sweep
  //  e e e e    e e e e -    Envelope and linear counter
  private fun stepFrameCounter() {
    when (framePeriod) {
      4 -> {
        frameValue = (frameValue + 1) % 4
        when (frameValue) {
          0, 2 -> stepEnvelope()
          1 -> {
            stepEnvelope()
            stepSweep()
            stepLength()
          }
          3 -> {
            stepEnvelope()
            stepSweep()
            stepLength()
            fireIRQ()
          }
        }
      }
      5 -> {
        frameValue = (frameValue + 1) % 5
        when (frameValue) {
          1, 3 -> stepEnvelope()
          0, 2 -> {
            stepEnvelope()
            stepSweep()
            stepLength()
          }
        }
      }
    }
  }

  private fun fireIRQ() {
    if (frameIRQ) {
      cpu.interrupt = Interrupt.IRQ
    }
  }

  private fun stepTimer() {
    if (cycle % 2 == 0L) {
      pulse1.stepTimer()
      pulse2.stepTimer()
      noise.stepTimer()
      dmc.stepTimer()
    }
    triangle.stepTimer()
  }

  fun writeRegister(address: Int, value: Int) {
    when (address) {
      0x4000 -> pulse1.writeControl(value)
      0x4001 -> pulse1.writeSweep(value)
      0x4002 -> pulse1.writeTimerLow(value)
      0x4003 -> pulse1.writeTimerHigh(value)
      0x4004 -> pulse2.writeControl(value)
      0x4005 -> pulse2.writeSweep(value)
      0x4006 -> pulse2.writeTimerLow(value)
      0x4007 -> pulse2.writeTimerHigh(value)
      0x4008 -> triangle.writeControl(value)
      0x4009, 0x4010 -> dmc.writeControl(value)
      0x4011 -> dmc.writeValue(value)
      0x4012 -> dmc.writeAddress(value)
      0x4013 -> dmc.writeLength(value)
      0x400A -> triangle.writeTimerLow(value)
      0x400B -> triangle.writeTimerHigh(value)
      0x400C -> noise.writeControl(value)
      0x400D, 0x400E -> noise.writePeriod(value)
      0x400F -> noise.writeLength(value)
      0x4015 -> writeControl(value)
      0x4017 -> writeFrameCounter(value)
      // default:
      // 	log.Fatalf("unhandled apu register write at address: 0x%04X", address)
    }
  }

  private fun writeFrameCounter(value: Int) {
    framePeriod = 4 + (value shr 7) and 1
    frameIRQ = (value shr 6) and 1 == 0
    // frameValue = 0
    if (framePeriod == 5) {
      stepEnvelope()
      stepSweep()
      stepLength()
    }
  }

  private fun stepLength() {
    pulse1.stepLength()
    pulse2.stepLength()
    triangle.stepLength()
    noise.stepLength()
  }

  private fun stepSweep() {
    pulse1.stepSweep()
    pulse2.stepSweep()
  }

  private fun stepEnvelope() {
    pulse1.stepEnvelope()
    pulse2.stepEnvelope()
    triangle.stepCounter()
    noise.stepEnvelope()
  }

  private fun writeControl(value: Int) {
    pulse1.enabled = value and 1 == 1
    pulse2.enabled = value and 2 == 2
    triangle.enabled = value and 4 == 4
    noise.enabled = value and 8 == 8
    dmc.enabled = value and 16 == 16
    if (!pulse1.enabled) {
      pulse1.lengthValue = 0
    }
    if (!pulse2.enabled) {
      pulse2.lengthValue = 0
    }
    if (!triangle.enabled) {
      triangle.lengthValue = 0
    }
    if (!noise.enabled) {
      noise.lengthValue = 0
    }
    if (!dmc.enabled) {
      dmc.currentLength = 0
    } else {
      if (dmc.currentLength == 0) {
        dmc.restart()
      }
    }
  }

  companion object {
    private const val FRAME_COUNTER_RATE = CPU.FREQUENCY / 240.0
    val LENGTH_TABLE = intArrayOf( // Byte
        10, 254, 20, 2, 40, 4, 80, 6, 160, 8, 60, 10, 14, 12, 26, 14,
        12, 16, 24, 18, 48, 20, 96, 22, 192, 24, 72, 26, 16, 28, 32, 30
    )
  }
}