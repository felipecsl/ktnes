package android.emu6502

import android.emu6502.instructions.Symbols
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.mockito.Mockito.mock

class LabelsTest {
  private val symbols = Symbols()
  private val assembler = Assembler(Memory(mock(Display::class.java)), symbols)
  private val labels = Labels(assembler, symbols)

  @Test fun testAddLabel() {
    labels.indexLines(listOf("test:"))
    assertThat(labels["test"], equalTo(assembler.defaultCodePC))
  }
}
