package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

class CLI(private val cpu: CPU) : BaseInstruction(Instruction.CLI, cpu) {

}