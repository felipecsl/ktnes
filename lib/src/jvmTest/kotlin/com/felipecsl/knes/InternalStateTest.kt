package com.felipecsl.knes

import com.google.common.truth.Truth.assertThat
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

class InternalStateTest {
  @Test
  @Ignore("Failing on CI, only passes with the SMB3 ROM")
  fun `make sure state changes are accurate`() {
    var noMoreCPUData = false
    var noMorePPUData = false
    var noMoreAPUData = false
    var noMoreMapperData = false
    lateinit var expectedCPUState: List<String>
    lateinit var expectedMapperState: List<String>
    lateinit var expectedPPUState: List<String>
    lateinit var expectedAPUState: List<String>
    val classLoader = javaClass.classLoader
    val cpuReference = GZIPInputStream(
        File(classLoader.getResource("cpu_state_reference.gz")!!.toURI()).inputStream())
        .bufferedReader()
    val mapperReference = GZIPInputStream(
        classLoader.getResourceAsStream("mapper_state_reference.gz"))
        .bufferedReader()
    val ppuReference = GZIPInputStream(classLoader.getResourceAsStream("ppu_state_reference.gz"))
        .bufferedReader()
    val apuReference = ZipInputStream(
        classLoader.getResourceAsStream("apu_state_reference.gz.zip")).bufferedReader()
    val cpuCallback = object : CPUStepCallback {
      override fun onStep(cycles: Long, PC: Int, SP: Int, A: Int, X: Int, Y: Int, C: Int, Z: Int,
          I: Int, D: Int, B: Int, U: Int, V: Int, N: Int, interrupt: Int,
          stall: Int,
          lastOpcode: String?) {
        expectedCPUState = cpuReference.readLine()?.split(", ") ?: listOf()
        if (expectedCPUState.isNotEmpty()) {
          val propsMap = mapOf(
              "cycles" to cycles,
              "PC" to PC,
              "SP" to SP,
              "A" to A,
              "X" to X,
              "Y" to Y,
              "Y" to Y,
              "C" to C,
              "Z" to Z,
              "I" to I,
              "D" to D,
              "B" to B,
              "U" to U,
              "V" to V,
              "N" to N,
              "interrupt" to interrupt,
              "stall" to stall
          )
          assertStateIsValid("CPU", expectedCPUState, propsMap, lastOpcode)
        } else {
          if (!noMoreCPUData) {
            println("No more CPU data")
            noMoreCPUData = true
          }
        }
      }
    }
    val mapperCallback = object : MapperStepCallback {
      override fun onStep(register: Int, registers: IntArray, prgMode: Int, chrMode: Int,
          prgOffsets: IntArray, chrOffsets: IntArray, reload: Int, counter: Int,
          irqEnable: Boolean) {
        expectedMapperState = mapperReference.readLine()?.split(", ") ?: listOf()
        if (expectedMapperState.isNotEmpty()) {
          val propsMap = mapOf(
              "register" to register,
              "registers" to registers.joinAsGolangString(),
              "prgMode" to prgMode,
              "chrMode" to chrMode,
              "prgOffsets" to prgOffsets.joinAsGolangString(),
              "chrOffsets" to chrOffsets.joinAsGolangString(),
              "reload" to reload,
              "counter" to counter,
              "irqEnable" to irqEnable
          )
          assertStateIsValid("Mapper", expectedMapperState, propsMap)
        } else {
          if (!noMoreMapperData) {
            println("No more mapper data")
            noMoreMapperData = true
          }
        }
      }
    }
    val ppuCallback = object : PPUStepCallback {
      override fun step(cycle: Int, scanLine: Int, frame: Int, paletteData: IntArray,
          nameTableData: IntArray, oamData: IntArray, v: Int, t: Int, x: Int, w: Int,
          f: Int, register: Int, nmiOccurred: Boolean, nmiOutput: Boolean,
          nmiPrevious: Boolean, nmiDelay: Int, nameTableByte: Int,
          attributeTableByte: Int, lowTileByte: Int, highTileByte: Int, tileData: Int,
          spriteCount: Int, spritePatterns: IntArray, spritePositions: IntArray,
          spritePriorities: IntArray, spriteIndexes: IntArray, flagNameTable: Int,
          flagIncrement: Int, flagSpriteTable: Int, flagBackgroundTable: Int,
          flagSpriteSize: Int, flagMasterSlave: Int, flagGrayscale: Int,
          flagShowLeftBackground: Int, flagShowLeftSprites: Int,
          flagShowBackground: Int, flagShowSprites: Int, flagRedTint: Int,
          flagGreenTint: Int, flagBlueTint: Int, flagSpriteZeroHit: Int,
          flagSpriteOverflow: Int,
          oamAddress: Int, bufferedData: Int
      ) {
        expectedPPUState = ppuReference.readLine()?.split(", ") ?: listOf()
        if (expectedPPUState.isNotEmpty()) {
          val propsMap = mapOf(
              "Cycle" to cycle,
              "ScanLine" to scanLine,
              "Frame" to frame,
              "paletteData" to paletteData.joinAsGolangString(),
              "nameTableData" to nameTableData.joinAsGolangString(),
              "oamData" to oamData.joinAsGolangString(),
              "v" to v,
              "t" to t,
              "x" to x,
              "w" to w,
              "f" to f,
              "register" to register,
              "nmiOccurred" to nmiOccurred,
              "nmiOutput" to nmiOutput,
              "nmiPrevious" to nmiPrevious,
              "nmiDelay" to nmiDelay,
              "nameTableByte" to nameTableByte,
              "attributeTableByte" to attributeTableByte,
              "lowTileByte" to lowTileByte,
              "highTileByte" to highTileByte,
              "tileData" to tileData,
              "spriteCount" to spriteCount,
              "spritePatterns" to spritePatterns.joinAsGolangString(),
              "spritePositions" to spritePositions.joinAsGolangString(),
              "spritePriorities" to spritePriorities.joinAsGolangString(),
              "spriteIndexes" to spriteIndexes.joinAsGolangString(),
              "flagNameTable" to flagNameTable,
              "flagIncrement" to flagIncrement,
              "flagSpriteTable" to flagSpriteTable,
              "flagBackgroundTable" to flagBackgroundTable,
              "flagSpriteSize" to flagSpriteSize,
              "flagMasterSlave" to flagMasterSlave,
              "flagGrayscale" to flagGrayscale,
              "flagShowLeftBackground" to flagShowLeftBackground,
              "flagShowLeftSprites" to flagShowLeftSprites,
              "flagShowBackground" to flagShowBackground,
              "flagShowSprites" to flagShowSprites,
              "flagRedTint" to flagRedTint,
              "flagGreenTint" to flagGreenTint,
              "flagBlueTint" to flagBlueTint,
              "flagSpriteZeroHit" to flagSpriteZeroHit,
              "flagSpriteOverflow" to flagSpriteOverflow,
              "oamAddress" to oamAddress,
              "bufferedData" to bufferedData
          )
          assertStateIsValid("PPU", expectedPPUState, propsMap)
        } else {
          if (!noMorePPUData) {
            println("No more PPU data")
            noMorePPUData = true
          }
        }
      }
    }
    val apuCallback = object : APUStepCallback {
      override fun onStep(cycle: Long,
          framePeriod: Int, frameValue: Int, frameIRQ: Boolean,
          pulse1Enabled: Boolean, pulse1Channel: Int, pulse1LengthEnabled: Boolean,
          pulse1LengthValue: Int, pulse1TimerPeriod: Int, pulse1TimerValue: Int,
          pulse1DutyMode: Int, pulse1DutyValue: Int, pulse1SweepReload: Boolean,
          pulse1SweepEnabled: Boolean, pulse1SweepNegate: Boolean,
          pulse1SweepShift: Int, pulse1SweepPeriod: Int, pulse1SweepValue: Int,
          pulse1EnvelopeEnabled: Boolean, pulse1EnvelopeLoop: Boolean,
          pulse1EnvelopeStart: Boolean, pulse1EnvelopePeriod: Int,
          pulse1EnvelopeValue: Int, pulse1EnvelopeVolume: Int,
          pulse1ConstantVolume: Int, pulse2Enabled: Boolean, pulse2Channel: Int,
          pulse2LengthEnabled: Boolean, pulse2LengthValue: Int,
          pulse2TimerPeriod: Int, pulse2TimerValue: Int, pulse2DutyMode: Int,
          pulse2DutyValue: Int, pulse2SweepReload: Boolean,
          pulse2SweepEnabled: Boolean, pulse2SweepNegate: Boolean,
          pulse2SweepShift: Int, pulse2SweepPeriod: Int, pulse2SweepValue: Int,
          pulse2EnvelopeEnabled: Boolean, pulse2EnvelopeLoop: Boolean,
          pulse2EnvelopeStart: Boolean, pulse2EnvelopePeriod: Int,
          pulse2EnvelopeValue: Int, pulse2EnvelopeVolume: Int,
          pulse2ConstantVolume: Int, triangleEnabled: Boolean,
          triangleLengthEnabled: Boolean, triangleLengthValue: Int,
          triangleTimerPeriod: Int, triangleTimerValue: Int, triangleDutyValue: Int,
          triangleCounterPeriod: Int, triangleCounterValue: Int,
          triangleCounterReload: Boolean, noiseEnabled: Boolean, noiseMode: Boolean,
          noiseShiftRegister: Int, noiseLengthEnabled: Boolean,
          noiseLengthValue: Int, noiseTimerPeriod: Int, noiseTimerValue: Int,
          noiseEnvelopeEnabled: Boolean, noiseEnvelopeLoop: Boolean,
          noiseEnvelopeStart: Boolean, noiseEnvelopePeriod: Int,
          noiseEnvelopeValue: Int, noiseEnvelopeVolume: Int,
          noiseConstantVolume: Int, dmcEnabled: Boolean, dmcValue: Int,
          dmcSampleAddress: Int, dmcSampleLength: Int, dmcCurrentAddress: Int,
          dmcCurrentLength: Int, dmcShiftRegister: Int, dmcBitCount: Int,
          dmcTickPeriod: Int, dmcTickValue: Int, dmcLoop: Boolean,
          dmcIrq: Boolean, output: Float) {
        expectedAPUState = apuReference.readLine()?.split(", ") ?: listOf()
        if (expectedAPUState.isNotEmpty()) {
          val propsMap = mapOf(
              "cycle" to cycle,
              "framePeriod" to framePeriod,
              "frameValue" to frameValue,
              "frameIRQ" to frameIRQ,
              "pulse1Enabled" to pulse1Enabled,
              "pulse1Channel" to pulse1Channel,
              "pulse1LengthEnabled" to pulse1LengthEnabled,
              "pulse1LengthValue" to pulse1LengthValue,
              "pulse1TimerPeriod" to pulse1TimerPeriod,
              "pulse1TimerValue" to pulse1TimerValue,
              "pulse1DutyMode" to pulse1DutyMode,
              "pulse1DutyValue" to pulse1DutyValue,
              "pulse1SweepReload" to pulse1SweepReload,
              "pulse1SweepEnabled" to pulse1SweepEnabled,
              "pulse1SweepNegate" to pulse1SweepNegate,
              "pulse1SweepShift" to pulse1SweepShift,
              "pulse1SweepPeriod" to pulse1SweepPeriod,
              "pulse1SweepValue" to pulse1SweepValue,
              "pulse1EnvelopeEnabled" to pulse1EnvelopeEnabled,
              "pulse1EnvelopeLoop" to pulse1EnvelopeLoop,
              "pulse1EnvelopeStart" to pulse1EnvelopeStart,
              "pulse1EnvelopePeriod" to pulse1EnvelopePeriod,
              "pulse1EnvelopeValue" to pulse1EnvelopeValue,
              "pulse1EnvelopeVolume" to pulse1EnvelopeVolume,
              "pulse1ConstantVolume" to pulse1ConstantVolume,
              "pulse2Enabled" to pulse2Enabled,
              "pulse2Channel" to pulse2Channel,
              "pulse2LengthEnabled" to pulse2LengthEnabled,
              "pulse2LengthValue" to pulse2LengthValue,
              "pulse2TimerPeriod" to pulse2TimerPeriod,
              "pulse2TimerValue" to pulse2TimerValue,
              "pulse2DutyMode" to pulse2DutyMode,
              "pulse2DutyValue" to pulse2DutyValue,
              "pulse2SweepReload" to pulse2SweepReload,
              "pulse2SweepEnabled" to pulse2SweepEnabled,
              "pulse2SweepNegate" to pulse2SweepNegate,
              "pulse2SweepShift" to pulse2SweepShift,
              "pulse2SweepPeriod" to pulse2SweepPeriod,
              "pulse2SweepValue" to pulse2SweepValue,
              "pulse2EnvelopeEnabled" to pulse2EnvelopeEnabled,
              "pulse2EnvelopeLoop" to pulse2EnvelopeLoop,
              "pulse2EnvelopeStart" to pulse2EnvelopeStart,
              "pulse2EnvelopePeriod" to pulse2EnvelopePeriod,
              "pulse2EnvelopeValue" to pulse2EnvelopeValue,
              "pulse2EnvelopeVolume" to pulse2EnvelopeVolume,
              "pulse2ConstantVolume" to pulse2ConstantVolume,
              "triangleEnabled" to triangleEnabled,
              "triangleLengthEnabled" to triangleLengthEnabled,
              "triangleLengthValue" to triangleLengthValue,
              "triangleTimerPeriod" to triangleTimerPeriod,
              "triangleTimerValue" to triangleTimerValue,
              "triangleDutyValue" to triangleDutyValue,
              "triangleCounterPeriod" to triangleCounterPeriod,
              "triangleCounterValue" to triangleCounterValue,
              "triangleCounterReload" to triangleCounterReload,
              "noiseEnabled" to noiseEnabled,
              "noiseMode" to noiseMode,
              "noiseShiftRegister" to noiseShiftRegister,
              "noiseLengthEnabled" to noiseLengthEnabled,
              "noiseLengthValue" to noiseLengthValue,
              "noiseTimerPeriod" to noiseTimerPeriod,
              "noiseTimerValue" to noiseTimerValue,
              "noiseEnvelopeEnabled" to noiseEnvelopeEnabled,
              "noiseEnvelopeLoop" to noiseEnvelopeLoop,
              "noiseEnvelopeStart" to noiseEnvelopeStart,
              "noiseEnvelopePeriod" to noiseEnvelopePeriod,
              "noiseEnvelopeValue" to noiseEnvelopeValue,
              "noiseEnvelopeVolume" to noiseEnvelopeVolume,
              "noiseConstantVolume" to noiseConstantVolume,
              "dmcEnabled" to dmcEnabled,
              "dmcValue" to dmcValue,
              "dmcSampleAddress" to dmcSampleAddress,
              "dmcSampleLength" to dmcSampleLength,
              "dmcCurrentAddress" to dmcCurrentAddress,
              "dmcCurrentLength" to dmcCurrentLength,
              "dmcShiftRegister" to dmcShiftRegister,
              "dmcBitCount" to dmcBitCount,
              "dmcTickPeriod" to dmcTickPeriod,
              "dmcTickValue" to dmcTickValue,
              "dmcLoop" to dmcLoop,
              "dmcIrq" to dmcIrq,
              "output" to if (output == 0F) "0" else String.format("%.9f", output)
          )
          assertStateIsValid("APU", expectedAPUState, propsMap)
        } else {
          if (!noMoreAPUData) {
            println("No more APU data")
            noMoreAPUData = true
          }
        }
      }
    }
    val cartridge = classLoader.getResourceAsStream("legend_of_zelda.nes")!!.readBytes()
    Director(
        cartridge,
        mapperCallback,
        cpuCallback,
        ppuCallback,
        apuCallback
    ).stepSeconds(Double.MAX_VALUE)
  }

  private fun assertStateIsValid(
      component: String,
      expectedState: List<String>,
      propsMap: Map<String, Any>,
      additionalState: String? = null
  ) {
    propsMap.entries.forEachIndexed { i, (_, actualState) ->
      if (expectedState[i].trim() != actualState.toString().trim()) {
        val actual = propsMap.entries.joinToString { entry -> "${entry.key}=${entry.value}" }
        val expected = propsMap.keys.mapIndexed { index, k ->
          "$k=${expectedState[index]}"
        }.joinToString()
        println("last opcode=$additionalState")
        assertThat("$component $actual").isEqualTo("$component $expected")
      }
    }
  }

  private fun IntArray.joinAsGolangString() =
      joinToString(" ", prefix = "[", postfix = "]") { it.toString() }
}
