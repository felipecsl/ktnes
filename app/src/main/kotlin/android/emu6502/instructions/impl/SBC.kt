package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.and
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction
import kotlin.experimental.xor

/** SuBtract with Carry */
class SBC(private val cpu: CPU) : BaseInstruction(Instruction.SBC, cpu) {
  override fun immediate() {
    testSBC(cpu.popByte())
  }

  private fun testSBC(value: Int) {
    if (cpu.A.xor(value).and(0x80) != 0) {
      cpu.setOverflow()
    } else {
      cpu.clearOverflow()
    }

    var w: Int
    if (cpu.decimalMode()) {
      var tmp = 0xf + cpu.A.and(0xf) - value.and(0xf) + cpu.P.and(1)
      if (tmp < 0x10) {
        w = 0
        tmp -= 6
      } else {
        w = 0x10
        tmp -= 0x10
      }
      w += 0xf0 + cpu.A.and(0xf0) - value.and(0xf0)
      if (w < 0x100) {
        cpu.clearCarry()
        if (cpu.overflow() && w < 0x80) {
          cpu.clearOverflow()
        }
        w -= 0x60
      } else {
        cpu.setCarry()
        if (cpu.overflow() && w >= 0x180) {
          cpu.clearOverflow()
        }
      }
      w += tmp
    } else {
      w = 0xff + cpu.A - value + cpu.P.and(1)
      if (w < 0x100) {
        cpu.clearCarry()
        if (cpu.overflow() && w < 0x80) {
          cpu.clearOverflow()
        }
      } else {
        cpu.setCarry()
        if (cpu.overflow() && w >= 0x180) {
          cpu.clearOverflow()
        }
      }
    }
    cpu.A = w.and(0xff)
    cpu.setSZFlagsForRegA()
  }
}

