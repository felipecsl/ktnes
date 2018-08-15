package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** STore X register */
class STX(private val cpu: CPU) : BaseInstruction(Instruction.STX, cpu) {
  override fun zeroPage() {
    cpu.write(cpu.popByte(), cpu.X)
  }

  override fun absolute() {
    cpu.write(cpu.popWord(), cpu.X)
  }
}
