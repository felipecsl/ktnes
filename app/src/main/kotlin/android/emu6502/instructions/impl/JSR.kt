package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** Jump to SubRoutine */
class JSR(private val cpu: CPU) : BaseInstruction(Instruction.JSR, cpu.instructionList) {
  override fun absolute() {
    val addr = cpu.popWord()
    val currAddr = cpu.PC - 1
    cpu.stackPush(currAddr.shr(8).and(0xff))
    cpu.stackPush(currAddr.and(0xff))
    cpu.PC = addr
  }
}

