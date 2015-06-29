package android.emu6502;

import android.emu6502.instructions.Symbols;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class CPUTest {

  private CPU cpu;
  private Assembler assembler;

  @Before public void setUp() {

    Memory memory = new Memory(mock(Display.class));
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

  @Test public void testSnake() {
    List<String> lines = ImmutableList.of(
        "define appleL         $00 ; screen location of apple, low byte",
        "define appleH         $01 ; screen location of apple, high byte",
        "define snakeHeadL     $10 ; screen location of snake head, low byte",
        "define snakeHeadH     $11 ; screen location of snake head, high byte",
        "define snakeBodyStart $12 ; start of snake body byte pairs",
        "define snakeDirection $02 ; direction (possible values are below)",
        "define snakeLength    $03 ; snake length, in bytes",
        "; Directions (each using a separate bit)",
        "define movingUp      1",
        "define movingRight   2",
        "define movingDown    4",
        "define movingLeft    8",
        "; ASCII values of keys controlling the snake",
        "define ASCII_w      $77",
        "define ASCII_a      $61",
        "define ASCII_s      $73",
        "define ASCII_d      $64",
        "; System variables",
        "define sysRandom    $fe",
        "define sysLastKey   $ff",
        "  jsr init",
        "  jsr loop",
        "init:",
        "  jsr initSnake",
        "  jsr generateApplePosition",
        "  rts",
        "initSnake:",
        "  lda #movingRight  ;start direction",
        "  sta snakeDirection",
        "  lda #4  ;start length (2 segments)",
        "  sta snakeLength",
        "  ",
        "  lda #$11",
        "  sta snakeHeadL",
        "  ",
        "  lda #$10",
        "  sta snakeBodyStart",
        "  ",
        "  lda #$0f",
        "  sta $14 ; body segment 1",
        "  ",
        "  lda #$04",
        "  sta snakeHeadH",
        "  sta $13 ; body segment 1",
        "  sta $15 ; body segment 2",
        "  rts",
        "generateApplePosition:",
        "  ;load a new random byte into $00",
        "  lda sysRandom",
        "  sta appleL",
        "  ;load a new random number from 2 to 5 into $01",
        "  lda sysRandom",
        "  and #$03 ;mask out lowest 2 bits",
        "  clc",
        "  adc #2",
        "  sta appleH",
        "  rts",
        "loop:",
        "  jsr readKeys",
        "  jsr checkCollision",
        "  jsr updateSnake",
        "  jsr drawApple",
        "  jsr drawSnake",
        "  jsr spinWheels",
        "  jmp loop",
        "readKeys:",
        "  lda sysLastKey",
        "  cmp #ASCII_w",
        "  beq upKey",
        "  cmp #ASCII_d",
        "  beq rightKey",
        "  cmp #ASCII_s",
        "  beq downKey",
        "  cmp #ASCII_a",
        "  beq leftKey",
        "  rts",
        "upKey:",
        "  lda #movingDown",
        "  bit snakeDirection",
        "  bne illegalMove",

        "  lda #movingUp",
        "  sta snakeDirection",
        "  rts",
        "rightKey:",
        "  lda #movingLeft",
        "  bit snakeDirection",
        "  bne illegalMove",
        "  lda #movingRight",
        "  sta snakeDirection",
        "  rts",
        "downKey:",
        "  lda #movingUp",
        "  bit snakeDirection",
        "  bne illegalMove",
        "  lda #movingDown",
        "  sta snakeDirection",
        "  rts",
        "leftKey:",
        "  lda #movingRight",
        "  bit snakeDirection",
        "  bne illegalMove",
        "  lda #movingLeft",
        "  sta snakeDirection",
        "  rts",
        "illegalMove:",
        "  rts",
        "checkCollision:",
        "  jsr checkAppleCollision",
        "  jsr checkSnakeCollision",
        "  rts",
        "checkAppleCollision:",
        "  lda appleL",
        "  cmp snakeHeadL",
        "  bne doneCheckingAppleCollision",
        "  lda appleH",
        "  cmp snakeHeadH",
        "  bne doneCheckingAppleCollision",
        "  ;eat apple",
        "  inc snakeLength",
        "  inc snakeLength ;increase length",
        "  jsr generateApplePosition",
        "doneCheckingAppleCollision:",
        "  rts",
        "checkSnakeCollision:",
        "  ldx #2 ;start with second segment",
        "snakeCollisionLoop:",
        "  lda snakeHeadL,x",
        "  cmp snakeHeadL",
        "  bne continueCollisionLoop",
        "maybeCollided:",
        "  lda snakeHeadH,x",
        "  cmp snakeHeadH",
        "  beq didCollide",
        "continueCollisionLoop:",
        "  inx",
        "  inx",
        "  cpx snakeLength          ;got to last section with no collision",
        "  beq didntCollide",
        "  jmp snakeCollisionLoop",
        "didCollide:",
        "  jmp gameOver",
        "didntCollide:",
        "  rts",
        "updateSnake:",
        "  ldx snakeLength",
        "  dex",
        "  txa",
        "updateloop:",
        "  lda snakeHeadL,x",
        "  sta snakeBodyStart,x",
        "  dex",
        "  bpl updateloop",
        "  lda snakeDirection",
        "  lsr",
        "  bcs up",
        "  lsr",
        "  bcs right",
        "  lsr",
        "  bcs down",
        "  lsr",
        "  bcs left",
        "up:",
        "  lda snakeHeadL",
        "  sec",
        "  sbc #$20",
        "  sta snakeHeadL",
        "  bcc upup",
        "  rts",
        "upup:",
        "  dec snakeHeadH",
        "  lda #$1",
        "  cmp snakeHeadH",
        "  beq collision",
        "  rts",
        "right:",
        "  inc snakeHeadL",
        "  lda #$1f",
        "  bit snakeHeadL",
        "  beq collision",
        "  rts",
        "down:",
        "  lda snakeHeadL",
        "  clc",
        "  adc #$20",
        "  sta snakeHeadL",
        "  bcs downdown",
        "  rts",
        "downdown:",
        "  inc snakeHeadH",
        "  lda #$6",
        "  cmp snakeHeadH",
        "  beq collision",
        "  rts",
        "left:",
        "  dec snakeHeadL",
        "  lda snakeHeadL",
        "  and #$1f",
        "  cmp #$1f",
        "  beq collision",
        "  rts",
        "collision:",
        "  jmp gameOver",
        "drawApple:",
        "  ldy #0",
        "  lda sysRandom",
        "  sta (appleL),y",
        "  rts",
        "drawSnake:",
        "  ldx #0",
        "  lda #1",
        "  sta (snakeHeadL,x) ; paint head",
        "  ",
        "  ldx snakeLength",
        "  lda #0",
        "  sta (snakeHeadL,x) ; erase end of tail",
        "  rts",
        "spinWheels:",
        "  ldx #0",
        "spinloop:",
        "  nop",
        "  nop",
        "  dex",
        "  bne spinloop",
        "  rts",
        "gameOver:", "\n");
    assembler.assembleCode(lines);
    cpu.execute();
    assertThat(cpu.getA(), equalTo(0x1f));
    assertThat(cpu.getX(), equalTo(0xff));
    assertThat(cpu.getY(), equalTo(0x00));
    assertThat(cpu.getSP(), equalTo(0xfb));
    assertThat(cpu.getPC(), equalTo(0x0736));
    assertThat(cpu.flags(), equalTo("00110011"));
  }
}
