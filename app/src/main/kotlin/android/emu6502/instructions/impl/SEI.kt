package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** SEt Interrupt */
class SEI(private val cpu: CPU) : BaseInstruction(Instruction.SEI, cpu) {
  override fun single() {
    cpu.setInterrupt()
  }
}

