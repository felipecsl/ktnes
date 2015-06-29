package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.Memory
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** STore Accumulator */
class STA(private val memory: Memory, private val cpu: CPU)
    : BaseInstruction(Instruction.STA, cpu.instructionList) {

  override fun absolute() {
    memory.storeByte(cpu.popWord(), cpu.A)
  }

  override fun zeroPage() {
    memory.storeByte(cpu.popByte(), cpu.A)
  }

  override fun zeroPageX() {
    cpu.memory.storeByte((cpu.popByte() + cpu.X).and(0xff), cpu.A)
  }

  override fun indirectY() {
    val addr = memory.getWord(cpu.popByte()) + cpu.Y
    memory.storeByte(addr, cpu.A)
  }

  override fun indirectX() {
    var zp = (cpu.popByte() + cpu.X).and(0xff)
    var addr = memory.getWord(zp)
    memory.storeByte(addr, cpu.A)
  }
}

