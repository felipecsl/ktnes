package android.emu6502

final class Emulator {
  val display = Display()
  val memory = Memory(display)
  val cpu = CPU(memory)
  val assembler = Assembler(Labels(), memory, Symbols())
}