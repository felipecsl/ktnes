package android.emu6502.instructions

import kotlin.reflect.KMemberFunction0

class InstructionTarget(val operation: BaseInstruction,
    val method: KMemberFunction0<BaseInstruction, Unit>) {
}
