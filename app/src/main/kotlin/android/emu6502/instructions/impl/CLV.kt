package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** CLear overflow */
class CLV(private val cpu: CPU) : BaseInstruction(Instruction.CLV, cpu) {
  override fun single() {
    cpu.clearOverflow()
  }
}

