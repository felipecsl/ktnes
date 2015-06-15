package android.emu6502;

import android.emu6502.instructions.Symbols;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class LabelsTest {

  private Labels labels;
  private Assembler assembler;

  @Before public void setUp() {
    Symbols symbols = new Symbols();
    assembler = new Assembler(new Memory(new Display()), symbols);
    labels = new Labels(assembler, symbols);
  }

  @Test public void testAddLabel() {
    labels.indexLines(Collections.singletonList("test:"));
    assertThat(labels.get("test"), equalTo(assembler.getDefaultCodePC()));
  }
}
