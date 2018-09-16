package com.felipecsl.knes

internal data class Triangle(
    var enabled: Boolean = false,
    var lengthEnabled: Boolean = false,
    var lengthValue: Int = 0, // Byte
    var timerPeriod: Int = 0,
    var timerValue: Int = 0,
    var dutyValue: Int = 0, // Byte
    var counterPeriod: Int = 0, // Byte
    var counterValue: Int = 0, // Byte
    var counterReload: Boolean = false
) {
  fun writeControl(value: Int) {
    lengthEnabled = (value shr 7) and 1 == 0
    counterPeriod = value and 0x7F
  }

  fun writeTimerLow(value: Int /* Byte */) {
    timerPeriod = (timerPeriod and 0xFF00) or value
  }

  fun writeTimerHigh(value: Int /* Byte */) {
    lengthValue = APU.LENGTH_TABLE[value shr 3]
    timerPeriod = (timerPeriod and 0x00FF) or ((value and 7) shl 8)
    timerValue = timerPeriod
    counterReload = true
  }

  fun stepLength() {
    if (lengthEnabled && lengthValue > 0) {
      lengthValue--
    }
  }

  fun stepCounter() {
    if (counterReload) {
      counterValue = counterPeriod
    } else if (counterValue > 0) {
      counterValue--
    }
    if (lengthEnabled) {
      counterReload = false
    }
  }

  fun stepTimer() {
    if (timerValue == 0) {
      timerValue = timerPeriod
      if (lengthValue > 0 && counterValue > 0) {
        dutyValue = (dutyValue + 1) % 32
      }
    } else {
      timerValue--
    }
  }

  fun output(): Int /* Byte */ {
    return if (!enabled || lengthValue == 0 || counterValue == 0) 0 else TRIANGLE_TABLE[dutyValue]
  }

  companion object {
    private val TRIANGLE_TABLE = intArrayOf( // Byte
        15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
    )
  }
}