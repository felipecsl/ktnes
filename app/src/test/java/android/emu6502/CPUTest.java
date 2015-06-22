package android.emu6502;

import android.emu6502.instructions.Symbols;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class CPUTest {

  private CPU cpu;
  private Assembler assembler;

  @Before public void setUp() {
    Memory memory = new Memory(new Display());
    assembler = new Assembler(memory, new Symbols());
    cpu = new CPU(memory);
  }

  @Test public void testSimple() {
    List<String> lines = ImmutableList.of(
        "LDA #$01",
        "STA $0200",
        "LDA #$05",
        "STA $0201",
        "LDA #$08",
        "STA $0202");
    assembler.assembleCode(lines);
    cpu.execute();
    assertThat(cpu.getA(), equalTo(0x08));
    assertThat(cpu.getX(), equalTo(0x00));
    assertThat(cpu.getY(), equalTo(0x00));
    assertThat(cpu.getSP(), equalTo(0xFF));
    assertThat(cpu.getPC(), equalTo(0x0610));
    assertThat(cpu.flags(), equalTo("00110000"));
  }

  @Test public void testWithComments() {
    List<String> lines = ImmutableList.of(
        "LDA #$c0  ;Load the hex value $c0 into the A register",
        "TAX       ;Transfer the value in the A register to X",
        "INX       ;Increment the value in the X register",
        "ADC #$c4  ;Add the hex value $c4 to the A register",
        "BRK       ;Break - we're done");
    assembler.assembleCode(lines);
    cpu.execute();
    assertThat(cpu.getA(), equalTo(0x84));
    assertThat(cpu.getX(), equalTo(0xC1));
    assertThat(cpu.getY(), equalTo(0x00));
    assertThat(cpu.getSP(), equalTo(0xFF));
    assertThat(cpu.getPC(), equalTo(0x0607));
    assertThat(cpu.flags(), equalTo("10110001"));
  }

  @Test public void testBranchAndLabel() {
    List<String> lines = ImmutableList.of(
        "LDX #$08",

        "decrement:",
        "DEX",
        "STX $0200",
        "CPX #$03",
        "BNE decrement",
        "STX $0201",
        "BRK");
    assembler.assembleCode(lines);
    cpu.execute();
    assertThat(cpu.getA(), equalTo(0x00));
    assertThat(cpu.getX(), equalTo(0x03));
    assertThat(cpu.getY(), equalTo(0x00));
    assertThat(cpu.getSP(), equalTo(0xFF));
    assertThat(cpu.getPC(), equalTo(0x060e));
    assertThat(cpu.flags(), equalTo("00110011"));
  }

  @Test public void testJump() {
    List<String> lines = ImmutableList.of(
        "LDA #$03",
        "JMP there",
        "BRK",
        "BRK",
        "BRK",

        "there:",
        "STA $0200");
    assembler.assembleCode(lines);
    cpu.execute();
    assertThat(cpu.getA(), equalTo(0x03));
    assertThat(cpu.getX(), equalTo(0x00));
    assertThat(cpu.getY(), equalTo(0x00));
    assertThat(cpu.getSP(), equalTo(0xFF));
    assertThat(cpu.getPC(), equalTo(0x060c));
    assertThat(cpu.flags(), equalTo("00110000"));
  }

  @Test public void testJumpToSubroutines() {
    List<String> lines = ImmutableList.of(
        "JSR init",
        "JSR loop",
        "JSR end",

        "init:",
        "LDX #$00",
        "RTS",

        "loop:",
        "INX",
        "CPX #$05",
        "BNE loop",
        "RTS",

        "end:",
        "BRK");
    assembler.assembleCode(lines);
    cpu.execute();
    assertThat(cpu.getA(), equalTo(0x00));
    assertThat(cpu.getX(), equalTo(0x05));
    assertThat(cpu.getY(), equalTo(0x00));
    assertThat(cpu.getSP(), equalTo(0xFD));
    assertThat(cpu.getPC(), equalTo(0x0613));
    assertThat(cpu.flags(), equalTo("00110011"));
  }

  @Test public void testSymbols() {
    List<String> lines = ImmutableList.of(
        "define a_dozen $0c ; a constant",
        "LDX #a_dozen       ; equivalent to \"LDX #$0c\"");
    assembler.assembleCode(lines);
    cpu.execute();
    assertThat(cpu.getA(), equalTo(0x00));
    assertThat(cpu.getX(), equalTo(0x0C));
    assertThat(cpu.getY(), equalTo(0x00));
    assertThat(cpu.getSP(), equalTo(0xFF));
    assertThat(cpu.getPC(), equalTo(0x0603));
    assertThat(cpu.flags(), equalTo("00110000"));
  }
}
