package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** DEcrement Y */
class DEY(private val cpu: CPU) : BaseInstruction(Instruction.DEY, cpu.instructionList) {
  override fun single() {
    cpu.Y = (cpu.Y - 1).and(0xff)
    cpu.setSZflagsForRegY()
  }
}
