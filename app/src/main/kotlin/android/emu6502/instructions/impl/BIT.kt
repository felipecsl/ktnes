package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** test BITs */
class BIT(private val cpu: CPU) : BaseInstruction(Instruction.BIT, cpu.instructionList) {
  override fun zeroPage() {
    val value = cpu.memory.get(cpu.popByte())
    BIT(value)
  }

  private fun BIT(value: Int) {
    if (value.and(0x80) != 0) {
      cpu.P = cpu.P.or(0x80)
    } else {
      cpu.P = cpu.P.and(0x7f)
    }
    if (value.and(0x40) != 0) {
      cpu.P = cpu.P.or(0x40)
    } else {
      cpu.P = cpu.P.and(0x40.inv())
    }
    if (cpu.A.and(value) != 0) {
      cpu.P = cpu.P.and(0xfd)
    } else {
      cpu.P = cpu.P.or(0x02)
    }
  }
}
