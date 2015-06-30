package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** ComPare X register */
class CPX(private val cpu: CPU) : BaseInstruction(Instruction.CPX, cpu) {
  override fun immediate() {
    val value = cpu.popByte()
    cpu.doCompare(cpu.X, value)
  }

  override fun zeroPage() {
    val value = cpu.memory.get(cpu.popByte())
    cpu.doCompare(cpu.X, value)
  }
}