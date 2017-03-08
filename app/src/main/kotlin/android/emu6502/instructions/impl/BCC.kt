package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** Branch on Carry Clear */
class BCC(private val cpu: CPU) : BaseInstruction(Instruction.BCC, cpu) {
  override fun branch() {
    val offset = cpu.popByte()
    if (!cpu.carry()) {
      cpu.jumpBranch(offset)
    }
  }
}

