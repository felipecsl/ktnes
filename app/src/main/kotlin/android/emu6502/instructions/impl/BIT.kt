package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** test BITs */
class BIT(cpu: CPU)
    : BaseInstruction(Instruction.BIT, cpu.instructionList) {
}
