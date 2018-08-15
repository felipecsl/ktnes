package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.Memory
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** STore Accumulator */
class STA(private val cpu: CPU) : BaseInstruction(Instruction.STA, cpu) {
  override fun absolute() {
    cpu.write(cpu.popWord(), cpu.A)
  }

  override fun zeroPage() {
    cpu.write(cpu.popByte(), cpu.A)
  }

  override fun zeroPageX() {
    cpu.write((cpu.popByte() + cpu.X).and(0xff), cpu.A)
  }

  override fun indirectY() {
    val addr = cpu.read(cpu.popByte()) + cpu.Y
    cpu.write(addr, cpu.A)
  }

  override fun indirectX() {
    val zp = (cpu.popByte() + cpu.X).and(0xff)
    val addr = cpu.read(zp).toInt()
    cpu.write(addr, cpu.A)
  }
}

