package android.emu6502

import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.Instruction
import android.emu6502.instructions.InstructionTarget
import android.emu6502.instructions.Opcodes
import android.emu6502.instructions.impl.*
import android.emu6502.nes.Console
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.util.concurrent.CountDownLatch
import kotlin.experimental.and

class CPU : Display.Callbacks {
  lateinit var console: Console
  private val bgHandlerThread = HandlerThread("Screencast Thread")
  private val bgHandler: Handler
  private var executionLock: CountDownLatch? = null

  init {
    bgHandlerThread.start()
    bgHandler = Handler(bgHandlerThread.looper)
  }

  var A: Int = 0                     // Accumulator
  var X: Int = 0                     // Register X
  var Y: Int = 0                     // Register Y
  var PC: Int = 0                     // Program counter
  var SP: Int = 0xFF                 // Stack pointer
  var P: Int = 0x30                   // Processor flags
  var stall: Int = 0                  // number of cycles to stall
  var cycles: Int = 0                 // number of cycles
  var interrupt: Interrupt = Interrupt.NONE

  private var TAG = "CPU"
  private val instructionList: MutableMap<Int, InstructionTarget> = mutableMapOf()
  private val operationList: Map<Instruction, BaseInstruction> = mapOf(
      Instruction.ADC to ADC(this), Instruction.AND to AND(this),
      Instruction.ASL to ASL(this), Instruction.BIT to BIT(this),
      Instruction.LDA to LDA(this), Instruction.LDX to LDX(this),
      Instruction.LDY to LDY(this), Instruction.STA to STA(this),
      Instruction.STX to STX(this), Instruction.TAX to TAX(this),
      Instruction.INX to INX(this), Instruction.DEX to DEX(this),
      Instruction.ORA to ORA(this), Instruction.CPX to CPX(this),
      Instruction.BRK to BRK(this), Instruction.BNE to BNE(this),
      Instruction.JMP to JMP(this), Instruction.JSR to JSR(this),
      Instruction.RTS to RTS(this), Instruction.SEI to SEI(this),
      Instruction.DEY to DEY(this), Instruction.CLC to CLC(this),
      Instruction.CMP to CMP(this), Instruction.BEQ to BEQ(this),
      Instruction.TXA to TXA(this), Instruction.BPL to BPL(this),
      Instruction.LSR to LSR(this), Instruction.BCS to BCS(this),
      Instruction.INC to INC(this), Instruction.NOP to NOP(this),
      Instruction.SEC to SEC(this), Instruction.SBC to SBC(this),
      Instruction.BCC to BCC(this), Instruction.DEC to DEC(this),
      Instruction.BMI to BMI(this), Instruction.BVC to BVC(this),
      Instruction.BVS to BVS(this), Instruction.CPY to CPY(this),
      Instruction.EOR to EOR(this), Instruction.CLI to CLI(this),
      Instruction.CLV to CLV(this), Instruction.CLD to CLD(this),
      Instruction.SED to SED(this), Instruction.TAY to TAY(this),
      Instruction.TYA to TYA(this), Instruction.INY to INY(this),
      Instruction.ROR to ROR(this), Instruction.ROL to ROL(this),
      Instruction.RTI to RTI(this), Instruction.TXS to TXS(this),
      Instruction.TSX to TSX(this), Instruction.PHA to PHA(this),
      Instruction.PLA to PLA(this), Instruction.PHP to PHP(this),
      Instruction.PLP to PLP(this), Instruction.STY to STY(this)
  )

  // for testing only
  fun testRun() {
    while (true) {
      executeNextInstruction()
      if (PC == 0) {
        break
      }
    }
    stop()
  }

  fun read16(address: Int): Int {
    val lo = read(address)
    val hi = read(address + 1)
    return (hi shl 8) or lo
  }

  fun read(address: Int): Int {
    when {
      address < 0x2000 ->
        return console.ram[address % 0x0800]
      address < 0x4000 ->
        return console.ppu.readRegister(0x2000 + address % 8)
      address == 0x4014 ->
        return console.ppu.readRegister(address)
      address == 0x4015 ->
        return console.apu.readRegister(address)
      address == 0x4016 ->
        return console.controller1.read()
      address == 0x4017 ->
        return console.controller2.read()
    //address < 0x6000 ->
    // TODO: I/O registers
      address >= 0x6000 ->
        return console.mapper.read(address)
      else ->
        throw RuntimeException("unhandled cpu memory read at address: ${address.toHexString()}")
    }
  }

  fun write(address: Int, value: Int) {
    when {
      address < 0x2000 ->
        console.ram[address % 0x0800] = value
      address < 0x4000 ->
        console.ppu.writeRegister(0x2000 + address % 8, value)
      address == 0x4014 ->
        console.ppu.writeRegister(address, value)
      address == 0x4015 ->
        console.apu.writeRegister(address, value)
      address == 0x4016 ->
        console.controller1.write(value)
      address == 0x4017 ->
        console.controller2.write(value)
    //address < 0x6000 ->
    // TODO: I/O registers
      address >= 0x6000 ->
        console.mapper.write(address, value)
      else ->
        throw RuntimeException("unhandled cpu memory write at address: ${address.toHexString()}")
    }
  }

  fun step(): Int {
    Log.i("CPU", "step()")
    if (stall > 0) {
      stall--
      return 1
    }
    if (interrupt == Interrupt.NMI)
      nmi()
    else if (interrupt == Interrupt.IRQ)
      irq()
    interrupt = Interrupt.NONE
    val cycles = executeNextInstruction()
    this.cycles += cycles

    if (PC == 0) {
      stop()
      Log.i(TAG, "Program end at PC=$" + (PC - 1).toHexString() + ", A=$" + A.toHexString() +
          ", X=$" + X.toHexString() + ", Y=$" + Y.toHexString())
    }

    return cycles
  }

  private fun irq() {
    stackPush16(PC)
    findInstruction(Instruction.PHP).method.invoke()
    PC = read(0xfffe)
    setInterrupt()
    cycles += 7
  }

  private fun findInstruction(instruction: Instruction): InstructionTarget {
    return instructionList[Opcodes.MAP[instruction]!!.first { it.opcode != 0xff }.opcode]
        ?: throw RuntimeException("Instruction $instruction not found")
  }

  private fun nmi() {
    stackPush16(PC)
    findInstruction(Instruction.PHP).method.invoke()
    PC = read(0xfffa)
    setInterrupt()
    cycles += 7
  }

  private fun executeNextInstruction(): Int {
    val instruction = read(PC)
    val target = instructionList[instruction]
    if (target != null) {
      target.method.invoke()
      return target.cycles
    } else {
      val candidate = Opcodes.MAP.entries
          .first { (_, value) -> value.any { it.opcode == instruction } }
      throw Exception(
          "Address $${PC.toHexString()} - unknown opcode 0x${instruction.toHexString()} " +
              "(instruction ${candidate.key.name})")
    }
  }

  fun stop() {
    bgHandler.removeCallbacks(null)
  }

  fun reset() {
    A = 0
    Y = 0
    X = 0
    PC = read16(0xFFFC)
    SP = 0xFD
    P = 0x24
  }

  fun addInstruction(opcode: Int, target: InstructionTarget) {
    instructionList[opcode] = target
  }

  fun popByte(): Int {
    return console.ram[PC++].and(0xff)
  }

  fun popWord(): Int {
    return popByte() + popByte().shl(8)
  }

  fun setSZFlagsForRegA() {
    setSVFlagsForValue(A)
  }

  fun setSZFlagsForRegX() {
    setSVFlagsForValue(X)
  }

  fun setSZFlagsForRegY() {
    setSVFlagsForValue(Y)
  }

  // set sign and overflow
  fun setSVFlagsForValue(value: Int) {
    P = if (value != 0) P.and(0xfd) else P.or(0x02)
    P = if (value.and(0x80) != 0) P.or(0x80) else P.and(0x7f)
  }

  fun overflow(): Boolean {
    return P.and(0x40) != 0
  }

  fun decimalMode(): Boolean {
    return P.and(8) != 0
  }

  fun carry(): Boolean {
    return P.and(1) != 0
  }

  fun negative(): Boolean {
    return P.and(0x80) != 0
  }

  fun zero(): Boolean {
    return P.and(0x02) != 0
  }

  fun setCarryFlagFromBit0(value: Int) {
    P = P.and(0xfe).or(value.and(1).toInt())
  }

  fun jumpBranch(offset: Int) {
    if (offset > 0x7f) {
      PC -= (0x100 - offset)
    } else {
      PC += offset
    }
  }

  fun doCompare(reg: Int, value: Int) {
    if (reg >= value) {
      setCarry()
    } else {
      clearCarry()
    }
    setSVFlagsForValue(reg - value)
  }

  /** CLear Carry */
  fun clearCarry() {
    P = P.and(0xfe)
  }

  /** SEt Carry */
  fun setCarry() {
    P = P.or(1)
  }

  /** SEt Interrupt */
  fun setInterrupt() {
    P = P.or(4)
  }

  /** CLear Decimal */
  fun clearDecimal() {
    P = P.and(0xf7)
  }

  /** CLear oVerflow */
  fun clearOverflow() {
    P = P.and(0xbf)
  }

  fun setOverflow() {
    P = P.or(0x40)
  }

  fun stackPush(value: Int) {
    write(0x100 or SP, value)
    SP.dec()
  }

  // push16 pushes two bytes onto the stack
  fun stackPush16(value: Int) {
    val hi = value shr 8
    val lo = value and 0xff
    stackPush(hi)
    stackPush(lo)
  }

  fun stackPop(): Int {
    SP.inc()
    return read(0x100 or SP)
  }

  /**
   * http://nesdev.com/6502.txt
   * Returns the processor flags in the format SV-BDIZC
   * Sign - this is set if the result of an operation is negative, cleared if positive.
   * Overflow - when an arithmetic operation produces a result too large to be represented in a byte
   * Unused - Supposed to be logical 1 at all times.
   * Break - this is set when a software interrupt (BRK instruction) is executed.
   * Decimal Mode - When set, and an Add with Carry or Subtract with Carry instruction is executed,
   *  the source values are treated as valid BCD (Binary Coded Decimal, eg. 0x00-0x99 = 0-99) numbers.
   *  The result generated is also a BCD number.
   * Interrupt - If it is set, interrupts are disabled. If it is cleared, interrupts are enabled.
   * Zero - this is set to 1 when any arithmetic or logical operation produces a zero result, and is
   *  set to 0 if the result is non-zero.
   * Carry - this holds the carry out of the most significant bit in any arithmetic operation.
   *  In subtraction operations however, this flag is cleared - set to 0 - if a borrow is required,
   *  set to 1 - if no borrow is required. The carry flag is also used in shift and rotate logical
   *  operations.
   * */
  fun flags(): String {
    val flags = StringBuilder()
    for (i in 7 downTo 0) {
      flags.append(P.shr(i).and(1))
    }
    return flags.toString()
  }

  override fun onUpdate() {
    executionLock = CountDownLatch(1)
    executionLock?.await()
  }

  override fun onDraw() {
    executionLock?.countDown()
  }

  // triggerIRQ causes an IRQ interrupt to occur on the next cycle
  fun triggerIRQ() {
    if (P and 0x20 == 0) {
      interrupt = Interrupt.IRQ
    }
  }

  // triggerNMI causes a non-maskable interrupt to occur on the next cycle
  fun triggerNMI() {
    interrupt = Interrupt.NMI
  }

  companion object {
    const val FREQUENCY = 1789773
  }
}