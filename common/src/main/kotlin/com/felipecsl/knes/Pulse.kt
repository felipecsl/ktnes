package com.felipecsl.knes

internal data class Pulse(
    var enabled: Boolean = false,
    var channel: Int = 0, // Byte
    var lengthEnabled: Boolean = false,
    var lengthValue: Int = 0, // Byte
    var timerPeriod: Int = 0,
    var timerValue: Int = 0,
    var dutyMode: Int = 0, // Byte
    var dutyValue: Int = 0, // Byte
    var sweepReload: Boolean = false,
    var sweepEnabled: Boolean = false,
    var sweepNegate: Boolean = false,
    var sweepShift: Int = 0, // Byte
    var sweepPeriod: Int = 0, // Byte
    var sweepValue: Int = 0, // Byte
    var envelopeEnabled: Boolean = false,
    var envelopeLoop: Boolean = false,
    var envelopeStart: Boolean = false,
    var envelopePeriod: Int = 0, // Byte
    var envelopeValue: Int = 0, // Byte
    var envelopeVolume: Int = 0, // Byte
    var constantVolume: Int = 0 // Byte
) {
  fun writeControl(value: Int /* Byte */) {
    dutyMode = (value shr 6) and 3
    lengthEnabled = (value shr 5) and 1 == 0
    envelopeLoop = (value shr 5) and 1 == 1
    envelopeEnabled = (value shr 4) and 1 == 0
    envelopePeriod = value and 15
    constantVolume = value and 15
    envelopeStart = true
  }

  fun writeSweep(value: Int /* Byte */) {
    sweepEnabled = (value shr 7) and 1 == 1
    sweepPeriod = (value shr 4) and 7 + 1
    sweepNegate = (value shr 3) and 1 == 1
    sweepShift = value and 7
    sweepReload = true
  }

  fun writeTimerLow(value: Int /* Byte */) {
    timerPeriod = (timerPeriod and 0xFF00) or value
  }

  fun writeTimerHigh(value: Int) {
    lengthValue = APU.LENGTH_TABLE[value shr 3]
    timerPeriod = (timerPeriod and 0x00FF) or ((value and 7) shl 8)
    envelopeStart = true
    dutyValue = 0
  }

  fun stepLength() {
    if (lengthEnabled && lengthValue > 0) {
      lengthValue--
    }
  }

  fun stepEnvelope() {
    if (envelopeStart) {
      envelopeVolume = 15
      envelopeValue = envelopePeriod
      envelopeStart = false
    } else if (envelopeValue > 0) {
      envelopeValue--
    } else {
      if (envelopeVolume > 0) {
        envelopeVolume--
      } else if (envelopeLoop) {
        envelopeVolume = 15
      }
      envelopeValue = envelopePeriod
    }
  }

  fun stepSweep() {
    when {
      sweepReload -> {
        if (sweepEnabled && sweepValue == 0) {
          sweep()
        }
        sweepValue = sweepPeriod
        sweepReload = false
      }
      sweepValue > 0 -> sweepValue--
      else -> {
        if (sweepEnabled) {
          sweep()
        }
        sweepValue = sweepPeriod
      }
    }
  }

  private fun sweep() {
    val delta = timerPeriod shr sweepShift
    if (sweepNegate) {
      timerPeriod -= delta
      if (channel == 1) {
        timerPeriod--
      }
    } else {
      timerPeriod += delta
    }
  }

  fun stepTimer() {
    if (timerValue == 0) {
      timerValue = timerPeriod
      dutyValue = (dutyValue + 1) % 8
    } else {
      timerValue--
    }
  }

  fun output(): Int /* Byte */ {
    return if (!enabled
        || lengthValue == 0
        || DUTY_TABLE[dutyMode][dutyValue] == 0
        || timerPeriod < 8
        || timerPeriod > 0x7FF) {
      0
    } else {
      // if (!sweepNegate && timerPeriod+(timerPeriod>>sweepShift) > 0x7FF) {
      // 	return 0
      // }
      if (envelopeEnabled) envelopeVolume else constantVolume
    }
  }

  companion object {
    private val DUTY_TABLE = arrayOf( // Byte
        intArrayOf(0, 1, 0, 0, 0, 0, 0, 0),
        intArrayOf(0, 1, 1, 0, 0, 0, 0, 0),
        intArrayOf(0, 1, 1, 1, 1, 0, 0, 0),
        intArrayOf(1, 0, 0, 1, 1, 1, 1, 1)
    )
  }
}