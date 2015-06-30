package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** Transfer A to X */
class TAX(private val cpu: CPU) : BaseInstruction(Instruction.TAX, cpu) {
  override fun single() {
    cpu.X = cpu.A.and(0xFF)
    cpu.setSZflagsForRegX()
  }
}
