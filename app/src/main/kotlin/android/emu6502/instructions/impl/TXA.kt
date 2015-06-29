package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** Transfer X to A */
class TXA(private val cpu: CPU) : BaseInstruction(Instruction.TXA, cpu.instructionList) {
  override fun single() {
    cpu.A = cpu.X.and(0xff)
    cpu.setSZFlagsForRegA()
  }
}


