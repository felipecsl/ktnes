package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** LoaD X register */
class LDX(cpu: CPU) : BaseInstruction(Instruction.LDX, cpu.instructionList) {
}
