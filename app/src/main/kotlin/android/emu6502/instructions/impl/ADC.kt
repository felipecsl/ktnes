package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.and
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction
import kotlin.experimental.and
import kotlin.experimental.xor

/** ADd with Carry */
class ADC(private val cpu: CPU) : BaseInstruction(Instruction.ADC, cpu) {
  override fun immediate() {
    testADC(cpu.popByte())
  }

  private fun testADC(value: Int) {
    var tmp: Int
    if (cpu.A.xor(value).and(0x80) != 0) {
      cpu.clearOverflow()
    } else {
      cpu.setOverflow()
    }

    if (cpu.decimalMode()) {
      tmp = cpu.A.and(0xf) + value.and(0xf) + cpu.P.and(1)
      if (tmp >= 10) {
        tmp = 0x10.or((tmp + 6).and(0xf))
      }
      tmp += cpu.A.and(0xf0) + value.and(0xf0)
      if (tmp >= 160) {
        cpu.setCarry()
        if (cpu.overflow() && tmp >= 0x180) {
          cpu.clearOverflow()
        }
        tmp += 0x60
      } else {
        cpu.clearCarry()
        if (cpu.overflow() && tmp < 0x80) {
          cpu.clearOverflow()
        }
      }
    } else {
      tmp = cpu.A + value + cpu.P.and(1)
      if (tmp >= 0x100) {
        cpu.setCarry()
        if (cpu.overflow() && tmp >= 0x180) {
          cpu.clearOverflow()
        }
      } else {
        cpu.clearCarry()
        if (cpu.overflow() && tmp < 0x80) {
          cpu.clearOverflow()
        }
      }
    }
    cpu.A = tmp.and(0xff)
    cpu.setSZFlagsForRegA()
  }
}