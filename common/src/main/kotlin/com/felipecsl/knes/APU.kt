package com.felipecsl.knes

internal class APU(
    private val audioSink: AudioSink,
    // Convert samples per second to cpu steps per sample
    private val sampleRate: Double = CPU.FREQUENCY / SAMPLE_RATE.toDouble(),
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
        highPassFilter(SAMPLE_RATE, 90F),
        highPassFilter(SAMPLE_RATE, 440F),
        lowPassFilter(SAMPLE_RATE, 14000F)
    ))
) {
  private lateinit var dmc: DMC
  var cpu: CPU? = null
    set(value) {
      field = value
      dmc = DMC(value!!)
    }

  init {
    for (i in 0 until 31) {
      pulseTable[i] = (95.52 / (8128.0 / i + 100)).toFloat()
    }
    for (i in 0 until 203) {
      tndTable[i] = (163.67 / (24329.0 / i + 100)).toFloat()
    }
  }

  fun readRegister(address: Int): Int /* Byte */ {
    return when (address) {
      0x4015 -> readStatus()
      else -> 0
    }.ensureByte()
  }

  private fun readStatus(): Int /* Byte */ {
    var result = 0
    if (pulse1.lengthValue > 0) {
      result = result or 1
    }
    if (pulse2.lengthValue > 0) {
      result = result or 2
    }
    if (triangle.lengthValue > 0) {
      result = result or 4
    }
    if (noise.lengthValue > 0) {
      result = result or 8
    }
    if (dmc.currentLength > 0) {
      result = result or 16
    }
    return result
  }

  fun step() {
    val cycle1 = cycle
    cycle++
    val cycle2 = cycle
    // step timer
    if (cycle % 2 == 0L) {
      pulse1.stepTimer()
      pulse2.stepTimer()
      noise.stepTimer()
      dmc.stepTimer()
    }
    triangle.stepTimer()
    if ((cycle1 / FRAME_COUNTER_RATE).toInt() != (cycle2 / FRAME_COUNTER_RATE).toInt()) {
      // step frame counter
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
              // fire irq
              if (frameIRQ) {
                cpu!!.interrupt = Interrupt.IRQ
              }
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
    if ((cycle1 / sampleRate).toInt() != (cycle2 / sampleRate).toInt()) {
      audioSink.write(filterChain.step(output()))
    }
  }

  private fun output(): Float {
    return pulseTable[pulse1.output() + pulse2.output()] +
        tndTable[3 * triangle.output() + 2 * noise.output() + dmc.output()]
  }

  fun writeRegister(address: Int, value: Int /* Byte */) {
    value.ensureByte()
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
    internal const val SAMPLE_RATE = 44100F
    private const val FRAME_COUNTER_RATE = CPU.FREQUENCY / 240.0
    val LENGTH_TABLE = intArrayOf( // Byte
        10, 254, 20, 2, 40, 4, 80, 6, 160, 8, 60, 10, 14, 12, 26, 14,
        12, 16, 24, 18, 48, 20, 96, 22, 192, 24, 72, 26, 16, 28, 32, 30
    )
  }
}