package android.emu6502;

import android.emu6502.instructions.Symbols;

import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class LabelsTest {
  private final Symbols symbols = new Symbols();
  private final Assembler assembler = new Assembler(new Memory(mock(Display.class)), symbols);
  private final Labels labels = new Labels(assembler, symbols);

  @Test public void testAddLabel() {
    labels.indexLines(Collections.singletonList("test:"));
    assertThat(labels.get("test"), equalTo(assembler.getDefaultCodePC()));
  }
}
