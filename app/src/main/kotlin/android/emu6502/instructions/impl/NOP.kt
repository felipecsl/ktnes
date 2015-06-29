package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** No OPeration */
class NOP(private val cpu: CPU) : BaseInstruction(Instruction.NOP, cpu.instructionList) {
  override fun single() {
  }
}

