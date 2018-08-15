package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** INCrement memory */
class INC(private val cpu: CPU) : BaseInstruction(Instruction.INC, cpu) {
  override fun zeroPage() {
    inc(cpu.popByte())
  }

  private fun inc(addr: Int) {
    var value = cpu.read(addr) + 1
    value = value.and(0xff)
    cpu.write(addr, value)
    cpu.setSVFlagsForValue(value)
  }
}
