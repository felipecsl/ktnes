package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** bitwise OR with Accumulator */
class ORA(cpu: CPU) : BaseInstruction(Instruction.ORA, cpu.instructionList) {
}