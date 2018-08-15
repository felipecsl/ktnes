package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** Push Processor Status */
class PHP(private val cpu: CPU) : BaseInstruction(Instruction.PHP, cpu) {
  override fun single() {
    cpu.stackPush((cpu.P or 0x10))
  }
}
