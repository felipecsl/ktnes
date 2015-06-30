package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** SEt Carry */
class SEC(private val cpu: CPU) : BaseInstruction(Instruction.SEC, cpu) {
  override fun single() {
    cpu.carry()
  }
}


