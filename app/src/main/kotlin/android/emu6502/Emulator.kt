package android.emu6502

import android.emu6502.instructions.Symbols

final class Emulator(display: Display) {
  val memory = Memory(display)
  val cpu = CPU(memory)
  val assembler = Assembler(memory, Symbols())
}