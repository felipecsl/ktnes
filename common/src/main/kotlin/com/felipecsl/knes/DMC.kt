package com.felipecsl.knes

internal data class DMC(
    var cpu: CPU,
    var enabled: Boolean = false,
    var value: Int = 0, // Byte
    var sampleAddress: Int = 0,
    var sampleLength: Int = 0,
    var currentAddress: Int = 0,
    var currentLength: Int = 0,
    var shiftRegister: Int = 0, // Byte
    var bitCount: Int = 0, // Byte
    var tickPeriod: Int = 0, // Byte
    var tickValue: Int = 0, // Byte
    var loop: Boolean = false,
    var irq: Boolean = false
) {
  fun writeControl(value: Int /* Byte */) {
    irq = value and 0x80 == 0x80
    loop = value and 0x40 == 0x40
    tickPeriod = DMC_TABLE[value and 0x0F]
  }

  fun writeValue(value: Int /* Byte */) {
    this.value = value and 0x7F
  }

  fun writeAddress(value: Int /* Byte */) {
    // Sample address = %11AAAAAA.AA000000
    sampleAddress = 0xC000 or (value shl 6)
  }

  fun writeLength(value: Int /* Byte */) {
    // Sample length = %0000LLLL.LLLL0001
    sampleLength = (value shl 4) or 1
  }

  fun restart() {
    currentAddress = sampleAddress
    currentLength = sampleLength
  }

  fun stepTimer() {
    if (enabled) {
      stepReader()
      if (tickValue == 0) {
        tickValue = tickPeriod
        stepShifter()
      } else {
        tickValue--
      }
    }
  }

  private fun stepShifter() {
    if (bitCount != 0) {
      if (shiftRegister and 1 == 1) {
        if (value <= 125) {
          value += 2
        }
      } else {
        if (value >= 2) {
          value -= 2
        }
      }
      shiftRegister = shiftRegister shr 1
      bitCount--
    }
  }

  private fun stepReader() {
    if (currentLength > 0 && bitCount == 0) {
      cpu.stall += 4
      shiftRegister = cpu.read(currentAddress)
      bitCount = 8
      currentAddress++
      if (currentAddress == 0) {
        currentAddress = 0x8000
      }
      currentLength--
      if (currentLength == 0 && loop) {
        restart()
      }
    }
  }

  fun output(): Int /* Byte */ {
    return value
  }

  companion object {
    private val DMC_TABLE = intArrayOf( // Byte
        214, 190, 170, 160, 143, 127, 113, 107, 95, 80, 71, 64, 53, 42, 36, 27
    )
  }
}