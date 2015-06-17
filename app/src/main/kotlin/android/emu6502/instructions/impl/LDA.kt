package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** LoaD Accumulator */
class LDA(private val cpu: CPU) : BaseInstruction(Instruction.LDA, cpu.instructionList) {
  override fun immediate() {
    cpu.A = cpu.popByte()
    cpu.setSZFlagsForRegA()
  }
}
