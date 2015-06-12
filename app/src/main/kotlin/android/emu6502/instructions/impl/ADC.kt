package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** ADd with Carry */
class ADC(cpu: CPU)
    : BaseInstruction(Instruction.ADC, cpu.instructionList) {
}