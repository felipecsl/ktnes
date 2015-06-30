package android.emu6502.instructions.impl

import android.emu6502.CPU
import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction

/** ADd with Carry */
class ADC(private val cpu: CPU) : BaseInstruction(Instruction.ADC, cpu.instructionList) {
  override fun immediate() {
    testADC(cpu.popByte())
  }

  private fun testADC(value: Int) {
    var tmp: Int
    if (cpu.A.xor(value).and(0x80) != 0) {
      cpu.CLV()
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
        cpu.SEC()
        if (cpu.overflow() && tmp >= 0x180) {
          cpu.CLV()
        }
        tmp += 0x60
      } else {
        cpu.CLC()
        if (cpu.overflow() && tmp < 0x80) {
          cpu.CLV()
        }
      }
    } else {
      tmp = cpu.A + value + cpu.P.and(1)
      if (tmp >= 0x100) {
        cpu.SEC()
        if (cpu.overflow() && tmp >= 0x180) {
          cpu.CLV()
        }
      } else {
        cpu.CLC()
        if (cpu.overflow() && tmp < 0x80) {
          cpu.CLV()
        }
      }
    }
    cpu.A = tmp.and(0xff)
    cpu.setSZFlagsForRegA()
  }
}