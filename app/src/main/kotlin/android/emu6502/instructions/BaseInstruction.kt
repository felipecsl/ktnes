package android.emu6502.instructions

import java.util.HashMap

open class BaseInstruction(private val instruction: Instruction,
    private val instructionList: HashMap<Int, InstructionTarget>) {

  init {
    val opcodes: IntArray = Opcodes.MAP[instruction] as IntArray
    val methods = arrayOf(::immediate, ::zeroPage, ::zeroPageX, ::zeroPageY, ::absolute,
        ::absoluteX, ::absoluteY, ::indirect, ::indirectX, ::indirectY, ::single, ::branch)

    opcodes.forEachIndexed { i, opcode ->
      if (opcode != 0xff) {
        instructionList.put(opcodes[i], InstructionTarget(this, methods[i]))
      }
    }
  }

  open fun immediate() {
  }

  open fun zeroPage() {
  }

  open fun zeroPageX() {
  }

  open fun zeroPageY() {
  }

  open fun absolute() {
  }

  open fun absoluteX() {
  }

  open fun absoluteY() {
  }

  open fun indirect() {
  }

  open fun indirectX() {
  }

  open fun indirectY() {
  }

  open fun single() {
  }

  open fun branch() {
  }
}
