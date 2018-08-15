package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** Pull Accumulator */
class PLA(private val cpu: CPU) : BaseInstruction(Instruction.PLA, cpu) {
  override fun single() {
    cpu.A = cpu.stackPop()
    cpu.setSZFlagsForRegA()
  }
}
