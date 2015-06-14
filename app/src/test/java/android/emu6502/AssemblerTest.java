package android.emu6502;

import com.google.common.collect.ImmutableList;

import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class AssemblerTest {

  @Test public void testAssembler() {
    List<String> lines = ImmutableList.of(
        "LDA #$01",
        "STA $0200",
        "LDA #$05",
        "STA $0201",
        "LDA #$08",
        "STA $0202");
    Assembler assembler = new Assembler(new Labels(), new Memory(new Display()), new Symbols());
    assembler.assembleCode(lines);
    assertThat(assembler.hexdump(), equalTo("0600: A9 01 8D 00 02 A9 05 8D 01 02 A9 08 8D 02 02"));
  }
}
