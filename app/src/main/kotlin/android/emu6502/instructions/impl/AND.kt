package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.and
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction
import kotlin.experimental.and

/** bitwise AND with accumulator */
class AND(private val cpu: CPU) : BaseInstruction(Instruction.AND, cpu) {
  override fun immediate() {
    cpu.A = cpu.A.and(cpu.popByte())
    cpu.setSZFlagsForRegA()
  }
}