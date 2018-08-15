package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction
import kotlin.experimental.and

/** LoaD Accumulator */
class LDA(private val cpu: CPU) : BaseInstruction(Instruction.LDA, cpu) {
  override fun immediate() {
    cpu.A = cpu.popByte()
    cpu.setSZFlagsForRegA()
  }

  override fun zeroPage() {
    cpu.A = cpu.read(cpu.popByte())
    cpu.setSZFlagsForRegA()
  }

  override fun zeroPageX() {
    cpu.A = cpu.read(cpu.popByte() + cpu.X).and(0xff)
    cpu.setSZFlagsForRegA()
  }
}
