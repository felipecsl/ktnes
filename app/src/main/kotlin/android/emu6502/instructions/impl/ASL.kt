package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** Arithmetic Shift Left */
class ASL(private val cpu: CPU) : BaseInstruction(Instruction.ASL, cpu)
