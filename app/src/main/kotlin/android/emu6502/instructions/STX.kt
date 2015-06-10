package android.emu6502.instructions


import java.util.*
import kotlin.reflect.KMemberFunction0

/** STore X register */
class STX(instructionList: HashMap<Int, KMemberFunction0<BaseInstruction, Unit>>)
: BaseInstruction(Instruction.STX, instructionList) {
}
