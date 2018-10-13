package com.felipecsl.android

import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mockito.mock

class AssemblerTest {
  private val assembler = Assembler(Memory(mock(Display::class.java)), mutableMapOf())

  @Test fun testSimple() {
    val lines = ImmutableList.of(
        "LDA #$01",
        "STA $0200",
        "LDA #$05",
        "STA $0201",
        "LDA #$08",
        "STA $0202")
    assembler.assembleCode(lines)
    assertThat(assembler.hexdump()).isEqualTo("0600: A9 01 8D 00 02 A9 05 8D 01 02 A9 08 8D 02 02")
  }

  @Test fun testWithComments() {
    val lines = ImmutableList.of(
        "LDA #\$c0  ;Load the hex value \$c0 into the A register",
        "TAX       ;Transfer the value in the A register to X",
        "INX       ;Increment the value in the X register",
        "ADC #\$c4  ;Add the hex value \$c4 to the A register",
        "BRK       ;Break - we're done")
    assembler.assembleCode(lines)
    assertThat(assembler.hexdump()).isEqualTo("0600: A9 C0 AA E8 69 C4 00")
  }

  @Test fun testBranchAndLabel() {
    val lines = ImmutableList.of(
        "LDX #$08",
        "decrement:",
        "DEX",
        "STX $0200",
        "CPX #$03",
        "BNE decrement",
        "STX $0201",
        "BRK")
    assembler.assembleCode(lines)
    assertThat(assembler.hexdump()).isEqualTo("0600: A2 08 CA 8E 00 02 E0 03 D0 F8 8E 01 02 00")
  }

  @Test fun testRelative() {
    val lines = ImmutableList.of(
        "LDA #$01",
        "CMP #$02",
        "BNE notequal",
        "STA $22",
        "notequal:",
        "BRK")

    assembler.assembleCode(lines)
    assertThat(assembler.hexdump()).isEqualTo("0600: A9 01 C9 02 D0 02 85 22 00")
  }

  @Test fun testIndirect() {
    val lines = ImmutableList.of(
        "LDA #$01",
        "STA \$f0",
        "LDA #\$cc",
        "STA \$f1",
        "JMP ($00f0) ;dereferences to \$cc01")

    assembler.assembleCode(lines)
    assertThat(assembler.hexdump()).isEqualTo("0600: A9 01 85 F0 A9 CC 85 F1 6C F0 00")
  }

  @Test fun testIndirectX() {
    val lines = ImmutableList.of(
        "LDX #$01",
        "LDA #$05",
        "STA $01",
        "LDA #$06",
        "STA $02",
        "LDY #$0a",
        "STY $0605",
        "LDA ($00,X)")

    assembler.assembleCode(lines)
    assertThat(assembler.hexdump())
        .isEqualTo("0600: A2 01 A9 05 85 01 A9 06 85 02 A0 0A 8C 05 06 A1 \n0610: 00")
  }

  @Test fun testIndirectY() {
    val lines = ImmutableList.of(
        "LDY #$01",
        "LDA #$03",
        "STA $01",
        "LDA #$07",
        "STA $02",
        "LDX #$0a",
        "STX $0704",
        "LDA ($01),Y")

    assembler.assembleCode(lines)
    assertThat(assembler.hexdump())
        .isEqualTo("0600: A0 01 A9 03 85 01 A9 07 85 02 A2 0A 8E 04 07 B1 \n0610: 01")
  }

  @Test fun testJump() {
    val lines = ImmutableList.of(
        "LDA #$03",
        "JMP there",
        "BRK",
        "BRK",
        "BRK",
        "there:",
        "STA $0200")

    assembler.assembleCode(lines)
    assertThat(assembler.hexdump()).isEqualTo("0600: A9 03 4C 08 06 00 00 00 8D 00 02")
  }

  @Test fun testSymbols() {
    val lines = ImmutableList.of(
        "define  sysRandom  \$fe ; an adress",
        "define  a_dozen    $0c ; a constant",
        "LDA sysRandom  ; equivalent to \"LDA \$fe\"",
        "LDX #a_dozen   ; equivalent to \"LDX #$0c\"")
    assembler.assembleCode(lines)
    assertThat(assembler.hexdump()).isEqualTo("0600: A5 FE A2 0C")
  }

  @Test fun testSnake() {
    val lines = ImmutableList.of(
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
        "define sysRandom    \$fe",
        "define sysLastKey   \$ff",
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
        "gameOver:", "\n")
    assembler.assembleCode(lines)
    assertThat(assembler.hexdump().toLowerCase()).isEqualTo(
        "0600: 20 06 06 20 38 06 20 0d 06 20 2a 06 60 a9 02 85 \n" +
            "0610: 02 a9 04 85 03 a9 11 85 10 a9 10 85 12 a9 0f 85 \n" +
            "0620: 14 a9 04 85 11 85 13 85 15 60 a5 fe 85 00 a5 fe \n" +
            "0630: 29 03 18 69 02 85 01 60 20 4d 06 20 8d 06 20 c3 \n" +
            "0640: 06 20 19 07 20 20 07 20 2d 07 4c 38 06 a5 ff c9 \n" +
            "0650: 77 f0 0d c9 64 f0 14 c9 73 f0 1b c9 61 f0 22 60 \n" +
            "0660: a9 04 24 02 d0 26 a9 01 85 02 60 a9 08 24 02 d0 \n" +
            "0670: 1b a9 02 85 02 60 a9 01 24 02 d0 10 a9 04 85 02 \n" +
            "0680: 60 a9 02 24 02 d0 05 a9 08 85 02 60 60 20 94 06 \n" +
            "0690: 20 a8 06 60 a5 00 c5 10 d0 0d a5 01 c5 11 d0 07 \n" +
            "06a0: e6 03 e6 03 20 2a 06 60 a2 02 b5 10 c5 10 d0 06 \n" +
            "06b0: b5 11 c5 11 f0 09 e8 e8 e4 03 f0 06 4c aa 06 4c \n" +
            "06c0: 35 07 60 a6 03 ca 8a b5 10 95 12 ca 10 f9 a5 02 \n" +
            "06d0: 4a b0 09 4a b0 19 4a b0 1f 4a b0 2f a5 10 38 e9 \n" +
            "06e0: 20 85 10 90 01 60 c6 11 a9 01 c5 11 f0 28 60 e6 \n" +
            "06f0: 10 a9 1f 24 10 f0 1f 60 a5 10 18 69 20 85 10 b0 \n" +
            "0700: 01 60 e6 11 a9 06 c5 11 f0 0c 60 c6 10 a5 10 29 \n" +
            "0710: 1f c9 1f f0 01 60 4c 35 07 a0 00 a5 fe 91 00 60 \n" +
            "0720: a2 00 a9 01 81 10 a6 03 a9 00 81 10 60 a2 00 ea \n" +
            "0730: ea ca d0 fb 60")
  }
}
