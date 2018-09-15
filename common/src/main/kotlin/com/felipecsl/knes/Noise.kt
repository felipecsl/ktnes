package com.felipecsl.knes

internal data class Noise(
    var enabled: Boolean = false,
    var mode: Boolean = false,
    var shiftRegister: Int = 0,
    var lengthEnabled: Boolean = false,
    var lengthValue: Int = 0,  // Byte
    var timerPeriod: Int = 0,
    var timerValue: Int = 0,
    var envelopeEnabled: Boolean = false,
    var envelopeLoop: Boolean = false,
    var envelopeStart: Boolean = false,
    var envelopePeriod: Int = 0,  // Byte
    var envelopeValue: Int = 0,  // Byte
    var envelopeVolume: Int = 0,  // Byte
    var constantVolume: Int = 0  // Byte
) {
  fun writeControl(value: Int /* Byte */) {
    lengthEnabled = (value shr 5) and 1 == 0
    envelopeLoop = (value shr 5) and 1 == 1
    envelopeEnabled = (value shr 4) and 1 == 0
    envelopePeriod = value and 15
    constantVolume = value and 15
    envelopeStart = true
  }

  fun writePeriod(value: Int /* Byte */) {
    mode = value and 0x80 == 0x80
    timerPeriod = NOISE_TABLE[value and 0x0F]
  }

  fun writeLength(value: Int /* Byte */) {
    lengthValue = APU.LENGTH_TABLE[value shr 3]
    envelopeStart = true
  }

  fun stepLength() {
    if (lengthEnabled && lengthValue > 0) {
      lengthValue--
    }
  }

  fun stepEnvelope() {
  }

  fun stepTimer() {
    if (timerValue == 0) {
      timerValue = timerPeriod
      val shift = if (mode) 6 else 1
      val b1 = shiftRegister and 1
      val b2 = (shiftRegister shr shift) and 1
      shiftRegister = shiftRegister shr 1
      shiftRegister = shiftRegister or ((b1 xor b2) shl 14)
    } else {
      timerValue--
    }
  }

  fun output(): Int /* Byte */ {
    return if (!enabled || lengthValue == 0 || shiftRegister and 1 == 1) {
      0
    } else {
      if (envelopeEnabled) envelopeVolume else constantVolume
    }
  }

  companion object {
    private val NOISE_TABLE = intArrayOf(
        4, 8, 16, 32, 64, 96, 128, 160, 202, 254, 380, 508, 762, 1016, 2034, 4068
    )
  }
}