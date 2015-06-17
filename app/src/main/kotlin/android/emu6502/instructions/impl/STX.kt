package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** STore X register */
class STX(private val cpu: CPU) : BaseInstruction(Instruction.STX, cpu.instructionList) {
  override fun zeroPage() {
    cpu.memory.storeByte(cpu.popByte(), cpu.X)
  }

  override fun absolute() {
    cpu.memory.storeByte(cpu.popWord(), cpu.X)
  }
}
