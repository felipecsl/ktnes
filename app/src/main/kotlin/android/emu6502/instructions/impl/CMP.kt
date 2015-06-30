package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** CoMPare accumulator */
final class CMP(private val cpu: CPU) : BaseInstruction(Instruction.CMP, cpu) {
  override fun immediate() {
    cpu.doCompare(cpu.A, cpu.popByte())
  }

  override fun zeroPage() {
    val value = cpu.memory.get(cpu.popByte())
    cpu.doCompare(cpu.A, value)
  }
}


