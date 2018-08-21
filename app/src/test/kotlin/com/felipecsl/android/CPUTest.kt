package com.felipecsl.android

import android.os.Build
import com.felipecsl.android.nes.Console
import com.felipecsl.android.nes.INESFileParser
import com.felipecsl.android.nes.PPU
import com.felipecsl.android.nes.mappers.MMC3
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.zip.GZIPInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1], constants = BuildConfig::class)
class CPUTest {
  @Test
  fun `make sure state changes are accurate`() {
    val context = RuntimeEnvironment.application
    val resources = context.resources
    val cpuReference = GZIPInputStream(resources.openRawResource(R.raw.cpu_state_reference))
        .bufferedReader()
    val mapperReference = GZIPInputStream(resources.openRawResource(R.raw.mapper_state_reference))
        .bufferedReader()
    val ppuReference = GZIPInputStream(resources.openRawResource(R.raw.ppu_state_reference))
        .bufferedReader()
    lateinit var expectedCPUState: List<String>
    lateinit var expectedMapperState: List<String>
    lateinit var expectedPPUState: List<String>
    val cpuCallback = object : CPU.StepCallback {
      override fun onStep(cycles: Long, PC: Int, SP: Int, A: Int, X: Int, Y: Int, C: Int, Z: Int,
          I: Int, D: Int, B: Int, U: Int, V: Int, N: Int, interrupt: Int, stall: Int,
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
          println("No more CPU data")
        }
      }
    }
    val mapperCallback = object : MMC3.StepCallback {
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
          println("No more mapper data")
        }
      }
    }
    val ppuCallback = object : PPU.StepCallback {
      override fun step(cycle: Int, scanLine: Int, frame: Int, paletteData: IntArray,
          nameTableData: IntArray, oamData: IntArray, v: Int, t: Int, x: Int, w: Int, f: Int,
          register: Int, nmiOccurred: Boolean, nmiOutput: Boolean,
          nmiPrevious: Boolean, nmiDelay: Int, nameTableByte: Int, attributeTableByte: Int,
          lowTileByte: Int, highTileByte: Int, tileData: Int, spriteCount: Int, spritePatterns: IntArray,
          spritePositions: IntArray, spritePriorities: IntArray, spriteIndexes: IntArray, flagNameTable: Int,
          flagIncrement: Int, flagSpriteTable: Int, flagBackgroundTable: Int, flagSpriteSize: Int,
          flagMasterSlave: Int, flagGrayscale: Int, flagShowLeftBackground: Int, flagShowLeftSprites: Int,
          flagShowBackground: Int, flagShowSprites: Int, flagRedTint: Int, flagGreenTint: Int,
          flagBlueTint: Int, flagSpriteZeroHit: Int, flagSpriteOverflow: Int, oamAddress: Int,
          bufferedData: Int
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
          println("No more PPU data")
        }
      }
    }
    val cartridge = INESFileParser.parseCartridge(resources.openRawResource(R.raw.smb3))
    val console = Console.newConsole(
        cartridge = cartridge,
        display = mock(Display::class.java),
        mapperStepCallback = mapperCallback,
        cpu = CPU(cpuCallback),
        ppu = PPU(ppuCallback)
    )
    console.reset()
    while (true) {
      console.step()
    }
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
