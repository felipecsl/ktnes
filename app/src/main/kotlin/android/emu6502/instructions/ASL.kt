package android.emu6502.instructions

import java.util.*
import kotlin.reflect.KMemberFunction0

/** Arithmetic Shift Left */
class ASL(instructionList: HashMap<Int, KMemberFunction0<BaseInstruction, Unit>>)
    : BaseInstruction(Instruction.ASL, instructionList) {
}
