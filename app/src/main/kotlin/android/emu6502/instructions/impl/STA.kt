package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.Memory
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** STore Accumulator */
class STA(private val memory: Memory, private val cpu: CPU)
    : BaseInstruction(Instruction.STA, cpu.instructionList) {

  override fun absolute() {
    memory.storeByte(cpu.popWord(), cpu.A);
  }
}

