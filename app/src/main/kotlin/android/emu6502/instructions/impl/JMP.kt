package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** JuMP */
class JMP(private val cpu: CPU) : BaseInstruction(Instruction.JMP, cpu) {
  override fun absolute() {
    cpu.PC = cpu.popWord()
  }
}