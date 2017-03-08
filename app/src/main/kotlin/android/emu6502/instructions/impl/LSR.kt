package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** Logical Shift Right */
class LSR(private val cpu: CPU) : BaseInstruction(Instruction.LSR, cpu) {
  override fun single() {
    cpu.setCarryFlagFromBit0(cpu.A)
    cpu.A = cpu.A.shr(1)
    cpu.setSZFlagsForRegA()
  }
}

