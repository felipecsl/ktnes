package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/**DEcrement X */
class DEX(private val cpu: CPU) : BaseInstruction(Instruction.DEX, cpu.instructionList) {
  override fun single() {
    cpu.X = (cpu.X - 1).and(0xff)
    cpu.setSZflagsForRegX()
  }
}

