package android.emu6502.instructions

import android.emu6502.CPU

open class BaseInstruction(val instruction: Instruction, private val cpu: CPU) {
  init {
    val opcodes = Opcodes.MAP[instruction]
    val methods = arrayOf(
        this::immediate,
        this::zeroPage,
        this::zeroPageX,
        this::zeroPageY,
        this::absolute,
        this::absoluteX,
        this::absoluteY,
        this::indirect,
        this::indirectX,
        this::indirectY,
        this::single,
        this::branch)

    opcodes!!.forEachIndexed { i, opcode ->
      if (opcode.opcode != 0xff) {
        cpu.addInstruction(opcodes[i].opcode, InstructionTarget(this, methods[i]))
      }
    }
  }

  open fun immediate() {
    throw IllegalStateException("Instruction " + javaClass.simpleName + " not implemented" +
        " with immediate addressing")
  }

  open fun zeroPage() {
    throw IllegalStateException("Instruction " + javaClass.simpleName + " not implemented" +
        " with zeroPage addressing")
  }

  open fun zeroPageX() {
    throw IllegalStateException("Instruction " + javaClass.simpleName + " not implemented" +
        " with zeroPageX addressing")
  }

  open fun zeroPageY() {
    throw IllegalStateException("Instruction " + javaClass.simpleName + " not implemented" +
        " with zeroPageY addressing")
  }

  open fun absolute() {
    throw IllegalStateException("Instruction " + javaClass.simpleName + " not implemented" +
        " with absolute addressing")
  }

  open fun absoluteX() {
    throw IllegalStateException("Instruction " + javaClass.simpleName + " not implemented" +
        " with absoluteX addressing")
  }

  open fun absoluteY() {
    throw IllegalStateException("Instruction " + javaClass.simpleName + " not implemented" +
        " with absoluteY addressing")
  }

  open fun indirect() {
    throw IllegalStateException("Instruction " + javaClass.simpleName + " not implemented" +
        " with indirect addressing")
  }

  open fun indirectX() {
    throw IllegalStateException("Instruction " + javaClass.simpleName + " not implemented" +
        " with indirectX addressing")
  }

  open fun indirectY() {
    throw IllegalStateException("Instruction " + javaClass.simpleName + " not implemented" +
        " with indirectY addressing")
  }

  open fun single() {
    throw IllegalStateException("Instruction " + javaClass.simpleName + " not implemented" +
        " with single addressing")
  }

  open fun branch() {
    throw IllegalStateException("Instruction " + javaClass.simpleName + " not implemented" +
        " with branch addressing")
  }
}
