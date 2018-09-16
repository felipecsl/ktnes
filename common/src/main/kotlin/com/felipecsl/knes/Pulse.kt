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
  fun pulseWriteControl(value: Int /* Byte */) {
    dutyMode = ((value shr 6) and 3).ensureByte()
    lengthEnabled = (value shr 5) and 1 == 0
    envelopeLoop = (value shr 5) and 1 == 1
    envelopeEnabled = (value shr 4) and 1 == 0
    envelopePeriod = (value and 15).ensureByte()
    constantVolume = (value and 15).ensureByte()
    envelopeStart = true
  }
}