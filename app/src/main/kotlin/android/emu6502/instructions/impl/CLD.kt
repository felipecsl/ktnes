package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** CLear Decimal */
class CLD(private val cpu: CPU) : BaseInstruction(Instruction.CLD, cpu) {
  override fun single() {
    cpu.clearDecimal()
  }
}

