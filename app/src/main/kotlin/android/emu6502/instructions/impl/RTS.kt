package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** ReTurn from Subroutine */
class RTS(private val cpu: CPU) : BaseInstruction(Instruction.RTS, cpu) {
  override fun single() {
    cpu.PC = cpu.stackPop().or(cpu.stackPop().shl(8)) + 1
  }
}

