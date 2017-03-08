package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** Branch on Not Equal */
class BNE(private val cpu: CPU) : BaseInstruction(Instruction.BNE, cpu) {
  override fun branch() {
    val offset = cpu.popByte()
    if (!cpu.zero()) {
      cpu.jumpBranch(offset)
    }
  }
}
