package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** DECrement memory */
class DEC(private val cpu: CPU) : BaseInstruction(Instruction.DEC, cpu.instructionList) {
  override fun zeroPage() {
    DEC(cpu.popByte())
  }

  fun DEC(addr: Int) {
    var value = cpu.memory.get(addr)
    value = (value - 1).and(0xff)
    cpu.memory.storeByte(addr, value)
    cpu.setSVFlagsForValue(value)
  }
}
