package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** INCrement memory */
final class INC(private val cpu: CPU) : BaseInstruction(Instruction.INC, cpu.instructionList) {
  override fun zeroPage() {
    inc(cpu.popByte())
  }

  private fun inc(addr: Int) {
    var value = cpu.memory.get(addr)
    value = (value + 1).and(0xff)
    cpu.memory.storeByte(addr, value)
    cpu.setSVFlagsForValue(value)
  }
}

