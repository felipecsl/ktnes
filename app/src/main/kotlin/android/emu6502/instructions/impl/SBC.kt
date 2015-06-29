package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** SuBtract with Carry */
class SBC(private val cpu: CPU) : BaseInstruction(Instruction.SBC, cpu.instructionList) {
  override fun immediate() {
    testSBC(cpu.popByte())
  }

  private fun testSBC(value: Int) {
    if (cpu.A.xor(value).and(0x80) != 0) {
      cpu.setOverflow()
    } else {
      cpu.CLV()
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
        cpu.CLC()
        if (cpu.overflow() && w < 0x80) {
          cpu.CLV()
        }
        w -= 0x60
      } else {
        cpu.SEC()
        if (cpu.overflow() && w >= 0x180) {
          cpu.CLV()
        }
      }
      w += tmp
    } else {
      w = 0xff + cpu.A - value + cpu.P.and(1)
      if (w < 0x100) {
        cpu.CLC()
        if (cpu.overflow() && w < 0x80) {
          cpu.CLV()
        }
      } else {
        cpu.SEC()
        if (cpu.overflow() && w >= 0x180) {
          cpu.CLV()
        }
      }
    }
    cpu.A = w.and(0xff)
    cpu.setSZFlagsForRegA()
  }
}

