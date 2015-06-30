package android.emu6502

import android.emu6502.instructions.Symbols

final class Emulator(val display: Display) {
  val memory = Memory(display)
  val cpu = CPU(memory)
  val assembler = Assembler(memory, Symbols())

  init {
    display.setOnDisplayCallback(cpu)
  }

  fun reset() {
    display.reset()
    cpu.reset()
  }
}