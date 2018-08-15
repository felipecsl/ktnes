package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** DECrement memory */
class DEC(private val cpu: CPU) : BaseInstruction(Instruction.DEC, cpu) {
  override fun zeroPage() {
    DEC(cpu.popByte())
  }

  fun DEC(addr: Int) {
    var value = cpu.read(addr) - 1
    value = value.and(0xff)
    cpu.write(addr, value)
    cpu.setSVFlagsForValue(value)
  }
}
