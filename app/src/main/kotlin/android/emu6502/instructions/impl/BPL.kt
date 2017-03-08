package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** Branch on PLus */
class BPL(private val cpu: CPU) : BaseInstruction(Instruction.BPL, cpu) {
  override fun branch() {
    val offset = cpu.popByte()
    if (!cpu.negative()) {
      cpu.jumpBranch(offset)
    }
  }
}
