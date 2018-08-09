package android.emu6502.nes

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
    assertThat(INESFileParser.parseFileHeader(tempFile.inputStream()).isValid()).isFalse()
  }

  @Test fun validHeader() {
    val testRom = javaClass.classLoader!!.getResource("roms/testrom.nes").toURI()
    val header = INESFileParser.parseFileHeader(File(testRom).inputStream())
    assertThat(header).isEqualTo(INESFileHeader(
        INESFileParser.INES_FILE_MAGIC, 0x10, 0x10, 0x40, 0x0, 0x0, INESFileParser.PADDING))
    assertThat(header.isValid()).isTrue()
  }

  @Test fun testMapper() {
    val testRom = javaClass.classLoader!!.getResource("roms/testrom.nes").toURI()
    val cartridge = INESFileParser.parseCartridge(File(testRom))
    // super mario bros 3 is Mapper 4 (MMC3)
    assertThat(cartridge.mapper).isEqualTo(4)
  }
}