package com.felipecsl.knes

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

class INESFileParserTest {
  private val tempFile = File.createTempFile("foo", "bar")

  @Before fun setUp() {
    tempFile.writeBytes((0..40).map(Int::toByte).toByteArray())
  }

  @After fun tearDown() {
    tempFile.delete()
  }

  @Test fun invalidHeader() {
    val inputStream = ByteArrayInputStream(tempFile.readBytes())
    assertThat(INESFileParser.parseFileHeader(inputStream).isValid()).isFalse()
  }

  @Test fun validHeader() {
    val classLoader = javaClass.classLoader
    val testRom = classLoader.getResource("testrom.nes").toURI()
    val inputStream = ByteArrayInputStream(File(testRom).readBytes())
    val header = INESFileParser.parseFileHeader(inputStream)
    assertThat(header).isEqualTo(INESFileHeader(
        INESFileParser.INES_FILE_MAGIC, 0x10, 0x10, 0x40, 0x0, 0x0, INESFileParser.PADDING))
    assertThat(header.isValid()).isTrue()
  }

  @Test fun testMapper() {
    val classLoader = javaClass.classLoader
    val testRom = classLoader.getResource("testrom.nes").toURI()
    val inputStream = ByteArrayInputStream(File(testRom).readBytes())
    val cartridge = INESFileParser.parseCartridge(inputStream)
    // super mario bros 3 is Mapper 4 (MMC3)
    assertThat(cartridge.mapper).isEqualTo(4)
  }
}