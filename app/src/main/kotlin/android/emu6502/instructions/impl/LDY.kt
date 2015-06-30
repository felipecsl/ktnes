package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** LoaD Y register */
class LDY(private val cpu: CPU) : BaseInstruction(Instruction.LDY, cpu) {
  override fun immediate() {
    cpu.Y = cpu.popByte()
    cpu.setSZflagsForRegY()
  }
}
