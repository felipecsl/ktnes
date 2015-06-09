package android.emu6502.instructions

import java.util.HashMap

open class BaseInstruction(private val instruction: Instruction,
    private val instructionList: HashMap<Int, kotlin.reflect.KMemberFunction0<BaseInstruction, Unit>>) {

  init {
    val opcodes: IntArray = Opcodes.MAP[instruction] as IntArray
    val methods = arrayOf(::immediate, ::zeroPage, ::zeroPageX, ::zeroPageY, ::absolute,
        ::absoluteX, ::absoluteY, ::indirect, ::indirectX, ::indirectY, ::single, ::branch)

    opcodes.forEachIndexed { i, opcode ->
      if (opcode != 0xff) {
        instructionList.put(opcodes[i], methods[i])
      }
    }
  }

  fun immediate() {
  }

  fun zeroPage() {
  }

  fun zeroPageX() {
  }

  fun zeroPageY() {
  }

  fun absolute() {
  }

  fun absoluteX() {
  }

  fun absoluteY() {
  }

  fun indirect() {
  }

  fun indirectX() {
  }

  fun indirectY() {
  }

  fun single() {
  }

  fun branch() {
  }
}
