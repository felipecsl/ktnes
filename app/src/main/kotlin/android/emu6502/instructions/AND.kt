package android.emu6502.instructions

import java.util.*
import kotlin.reflect.KMemberFunction0

/** bitwise AND with accumulator */
class AND(instructionList: HashMap<Int, KMemberFunction0<BaseInstruction, Unit>>)
    : BaseInstruction(Instruction.AND, instructionList) {
}