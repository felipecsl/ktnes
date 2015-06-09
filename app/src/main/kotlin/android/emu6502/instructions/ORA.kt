package android.emu6502.instructions

import java.util.HashMap

/** bitwise OR with Accumulator */
class ORA(instructionList: HashMap<Int, kotlin.reflect.KMemberFunction0<BaseInstruction, Unit>>)
    : BaseInstruction(Instruction.ORA, instructionList) {
}
