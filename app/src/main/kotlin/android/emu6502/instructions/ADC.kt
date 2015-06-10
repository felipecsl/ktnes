package android.emu6502.instructions

import java.util.*
import kotlin.reflect.KMemberFunction0

/** ADd with Carry */
class ADC(instructionList: HashMap<Int, KMemberFunction0<BaseInstruction, Unit>>)
    : BaseInstruction(Instruction.ADC, instructionList) {
}