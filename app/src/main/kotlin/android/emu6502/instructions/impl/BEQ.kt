package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** Branch on EQual */
class BEQ(private val cpu: CPU) : BaseInstruction(Instruction.BEQ, cpu.instructionList) {
  override fun branch() {
    val offset = cpu.popByte()
    if (cpu.zero()) {
      cpu.jumpBranch(offset)
    }
  }
}

