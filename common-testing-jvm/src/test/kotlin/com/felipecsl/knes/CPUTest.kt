package com.felipecsl.knes

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream

class CPUTest {
  @Test
  fun `make sure state changes are accurate`() {
    lateinit var expectedCPUState: List<String>
    lateinit var expectedMapperState: List<String>
    lateinit var expectedPPUState: List<String>
    val classLoader = javaClass.classLoader
    val cpuReference = GZIPInputStream(
        File(classLoader.getResource("cpu_state_reference.gz").toURI()).inputStream())
        .bufferedReader()
    val mapperReference = GZIPInputStream(classLoader.getResourceAsStream("mapper_state_reference.gz"))
        .bufferedReader()
    val ppuReference = GZIPInputStream(classLoader.getResourceAsStream("ppu_state_reference.gz"))
        .bufferedReader()
    val cpuCallback = object : CPUStepCallback {
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
          println("No more mapper data")
        }
      }
    }
    val ppuCallback = object : PPUStepCallback {
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
    val cartridge = classLoader.getResourceAsStream("smb3.nes").readBytes()
    Director.startConsole(
        cartridge,
        mapperCallback,
        cpuCallback,
        ppuCallback
    )
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
