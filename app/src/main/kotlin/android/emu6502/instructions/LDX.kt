package android.emu6502.instructions

import java.util.*
import kotlin.reflect.KMemberFunction0

/** LoaD X register */
class LDX(instructionList: HashMap<Int, KMemberFunction0<BaseInstruction, Unit>>)
    : BaseInstruction(Instruction.LDX, instructionList) {
}
