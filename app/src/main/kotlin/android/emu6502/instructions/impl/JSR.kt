package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** Jump to SubRoutine */
class JSR(private val cpu: CPU) : BaseInstruction(Instruction.JSR, cpu) {
  override fun absolute() {
    val addr = cpu.popWord()
    cpu.stackPush16(cpu.PC - 1)
    cpu.PC = addr
  }
}

