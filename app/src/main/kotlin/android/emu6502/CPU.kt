package android.emu6502

import android.emu6502.instructions.*
import android.util.Log
import java.util.HashMap
import kotlin.reflect.KMemberFunction0

class CPU(private val memory: Memory) {
  // Accumulator
  private var A: Byte = 0
  // Registers
  private var X: Byte = 0
  private var Y: Byte = 0
  // Program counter
  private var PC = 0x600
  // Stack pointer
  private var SP = 0xFF
  private var flags: Byte = 0
  private var isRunning = false
  private var debug = false
  private var monitoring = false
  private var TAG = "CPU"
  private val instructionList: HashMap<Int, KMemberFunction0<BaseInstruction, Unit>> = HashMap()
  private val operationList: HashMap<Instruction, BaseInstruction> = hashMapOf(
      Pair(Instruction.ADC, ADC(instructionList)),
      Pair(Instruction.AND, AND(instructionList)),
      Pair(Instruction.ASL, ASL(instructionList)),
      Pair(Instruction.BIT, BIT(instructionList)),
      Pair(Instruction.BPL, BPL(instructionList)),
      Pair(Instruction.BMI, BMI(instructionList)),
      Pair(Instruction.BVC, BVC(instructionList)),
      Pair(Instruction.BVS, BVS(instructionList)),
      Pair(Instruction.BCC, BCC(instructionList)),
      Pair(Instruction.BCS, BCS(instructionList)),
      Pair(Instruction.BNE, BNE(instructionList)),
      Pair(Instruction.BEQ, BEQ(instructionList)),
      Pair(Instruction.BRK, BRK(instructionList)),
      Pair(Instruction.CMP, CMP(instructionList)),
      Pair(Instruction.CPX, CPX(instructionList)),
      Pair(Instruction.CPY, CPY(instructionList)),
      Pair(Instruction.DEC, DEC(instructionList)),
      Pair(Instruction.EOR, EOR(instructionList)),
      Pair(Instruction.CLC, CLC(instructionList)),
      Pair(Instruction.SEC, SEC(instructionList)),
      Pair(Instruction.CLI, CLI(instructionList)),
      Pair(Instruction.SEI, SEI(instructionList)),
      Pair(Instruction.CLV, CLV(instructionList)),
      Pair(Instruction.CLD, CLD(instructionList)),
      Pair(Instruction.SED, SED(instructionList)),
      Pair(Instruction.INC, INC(instructionList)),
      Pair(Instruction.JMP, JMP(instructionList)),
      Pair(Instruction.JSR, JSR(instructionList)),
      Pair(Instruction.LDA, LDA(instructionList)),
      Pair(Instruction.LDX, LDX(instructionList)),
      Pair(Instruction.LDY, LDY(instructionList)),
      Pair(Instruction.LSR, LSR(instructionList)),
      Pair(Instruction.NOP, NOP(instructionList)),
      Pair(Instruction.ORA, ORA(instructionList)),
      Pair(Instruction.TAX, TAX(instructionList)),
      Pair(Instruction.TXA, TXA(instructionList)),
      Pair(Instruction.DEX, DEX(instructionList)),
      Pair(Instruction.INX, INX(instructionList)),
      Pair(Instruction.TAY, TAY(instructionList)),
      Pair(Instruction.TYA, TYA(instructionList)),
      Pair(Instruction.DEY, DEY(instructionList)),
      Pair(Instruction.INY, INY(instructionList)),
      Pair(Instruction.ROR, ROR(instructionList)),
      Pair(Instruction.ROL, ROL(instructionList)),
      Pair(Instruction.RTI, RTI(instructionList)),
      Pair(Instruction.RTS, RTS(instructionList)),
      Pair(Instruction.SBC, SBC(instructionList)),
      Pair(Instruction.STA, STA(instructionList)),
      Pair(Instruction.TXS, TXS(instructionList)),
      Pair(Instruction.TSX, TSX(instructionList)),
      Pair(Instruction.PHA, PHA(instructionList)),
      Pair(Instruction.PLA, PLA(instructionList)),
      Pair(Instruction.PHP, PHP(instructionList)),
      Pair(Instruction.PLP, PLP(instructionList)),
      Pair(Instruction.STX, STX(instructionList)),
      Pair(Instruction.STY, STY(instructionList))
  )

  fun execute() {
    setRandomByte()
    executeNextInstruction()

    if (PC == 0 || !isRunning) {
      stop()
      Log.i(TAG, "Program end at PC=$" + (PC - 1))
    }
  }

  private fun stop() {
    isRunning = false
  }

  private fun executeNextInstruction() {
    val instruction = Integer.valueOf(popByte().toInt().toString(), 16)
    val function = instructionList.get(instruction)
    if (function != null) {
      ORA(instructionList).function()
    } else {
      Log.e(TAG, "Address $" + PC + " - unknown opcode")
    }
  }

  private fun popByte(): Byte {
    return memory.get((PC++).and(0xff));
  }

  private fun setRandomByte() {
    memory.set(0xfe, Math.floor(Math.random() * 256).toInt())
  }
}