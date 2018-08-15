package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** INcrement X */
class INX(private val cpu: CPU) : BaseInstruction(Instruction.INX, cpu) {
  override fun single() {
    cpu.X = (cpu.X + 1).and(0xff)
    cpu.setSZFlagsForRegX()
  }
}
