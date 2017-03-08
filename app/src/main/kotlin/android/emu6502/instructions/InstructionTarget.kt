package android.emu6502.instructions

data class InstructionTarget(val operation: BaseInstruction, val method: () -> Unit)