package com.felipecsl.knes

import com.felipecsl.knes.AddressingMode.MODE_ABSOLUTEX
import com.felipecsl.knes.AddressingMode.MODE_ACCUMULATOR
import com.felipecsl.knes.AddressingMode.MODE_IMPLIED
import com.felipecsl.knes.AddressingMode.MODE_INDIRECT
import com.felipecsl.knes.AddressingMode.MODE_RELATIVE
import com.felipecsl.knes.AddressingMode.MODE_ZEROPAGEX
import com.felipecsl.knes.AddressingMode.UNUSED

internal class CPU(private val stepCallback: CPUStepCallback? = null) {
  lateinit var console: Console

  private var stepInfo: StepInfo = StepInfo(0, 0, 0)
  var cycles: Long = 0              // number of cycles
  var PC: Int = 0                   // Program counter
  var SP: Int = 0xFF                // Stack pointer
  var A: Int = 0                    // Accumulator
  var X: Int = 0                    // Register X
  var Y: Int = 0                    // Register Y
  var C: Int = 0                    // carry flag
  var Z: Int = 0                    // zero flag
  var I: Int = 0                    // interrupt disable flag
  var D: Int = 0                    // decimal mode flag
  var B: Int = 0                    // break command flag
  var U: Int = 0                    // unused flag
  var V: Int = 0                    // overflow flag
  var N: Int = 0                    // negative flag
  var stall: Int = 0                // number of cycles to stall
  private val addressingModes = arrayOf(
      AddressingMode.UNUSED,
      AddressingMode.MODE_ABSOLUTE,
      AddressingMode.MODE_ABSOLUTEX,
      AddressingMode.MODE_ABSOLUTEY,
      AddressingMode.MODE_ACCUMULATOR,
      AddressingMode.MODE_IMMEDIATE,
      AddressingMode.MODE_IMPLIED,
      AddressingMode.MODE_INDEXEDINDIRECT,
      AddressingMode.MODE_INDIRECT,
      AddressingMode.MODE_INDIRECTINDEXED,
      AddressingMode.MODE_RELATIVE,
      AddressingMode.MODE_ZEROPAGE,
      AddressingMode.MODE_ZEROPAGEX,
      AddressingMode.MODE_ZEROPAGEY
  )
  private var interrupt: Interrupt = Interrupt.NOT_SET

  private fun read16(address: Int): Int {
    val lo = read(address)
    val hi = read(address + 1)
    return (hi shl 8) or lo
  }

  // read16bug emulates a 6502 bug that caused the low byte to wrap without
  // incrementing the high byte
  private fun read16bug(address: Int): Int {
    val b = (address and 0xFF00) or (address + 1)
    val lo = read(address)
    val hi = read(b)
    return (hi shl 8) or lo
  }

  fun read(address: Int): Int {
    return when {
      address < 0x2000 -> console.ram[address % 0x0800]
      address < 0x4000 -> console.ppu.readRegister(0x2000 + address % 8)
      address == 0x4014 -> console.ppu.readRegister(address)
      address == 0x4015 -> console.apu.readRegister(address)
      address == 0x4016 -> console.controller1.read()
      address == 0x4017 -> console.controller2.read()
      //address < 0x6000 -> TODO: I/O registers
      address >= 0x6000 -> console.mapper.read(address)
      else ->
        throw RuntimeException("unhandled cpu memory read at address: $address")
    }
  }

  private fun write(address: Int, value: Int) {
    when {
      address < 0x2000 ->
        console.ram[address % 0x0800] = value
      address < 0x4000 ->
        console.ppu.writeRegister(0x2000 + address % 8, value)
      address < 0x4014 ->
        console.apu.writeRegister(address, value)
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
        throw RuntimeException("unhandled cpu memory write at address: $address")
    }
  }

  fun step(): Long {
//    stepCallback?.onStep(
//        cycles, PC, SP, A, X, Y, C, Z, I, D, B, U, V, N, interrupt.ordinal, stall, null)
    if (stall > 0) {
      stall--
      return 1
    }
    val currCycles = cycles
    executeInterrupt()
    executeNextInstruction()
    return cycles - currCycles
  }

  private fun executeInterrupt() {
    if (interrupt == Interrupt.NMI)
      nmi()
    else if (interrupt == Interrupt.IRQ)
      irq()
    interrupt = Interrupt.NONE
  }

  private fun irq() {
    push16(PC)
    stepInfo.address = 0
    stepInfo.PC = 0
    stepInfo.mode = 0
    php(stepInfo)
    PC = read16(0xfffe)
    I = 1
    cycles += 7
  }

  private fun nmi() {
    push16(PC)
    stepInfo.address = 0
    stepInfo.PC = 0
    stepInfo.mode = 0
    php(stepInfo)
    PC = read16(0xfffa)
    I = 1
    cycles += 7
  }

  private fun executeNextInstruction() {
    val opcode = read(PC)
    val mode = instructionModes[opcode]
    val addressingMode = addressingModes[mode]
    val address = addressForMode(addressingMode)
    val pageCrossed = isPageCrossed(addressingMode, address)
    PC += instructionSizes[opcode]
    cycles += instructionCycles[opcode]
    if (pageCrossed) {
      cycles += instructionPageCycles[opcode]
    }
    stepInfo.address = address
    stepInfo.PC = PC
    stepInfo.mode = mode
    when (opcode) {
      0 -> brk(stepInfo)
      1, 5, 9, 13, 17, 21, 25, 29 -> ora(stepInfo)
      2, 18, 34, 50, 66, 82, 98, 114, 146, 178, 210, 242 -> kil(stepInfo)
      3, 7, 15, 19, 23, 27, 31 -> slo(stepInfo)
      4, 12, 20, 26, 28, 52, 58, 60, 68, 84, 90, 92, 100, 116, 122, 124, 128, 130, 137, 194, 212, 218, 220, 226, 234, 244, 250, 252 -> nop(stepInfo)
      6, 10, 14, 22, 30 -> asl(stepInfo)
      8 -> php(stepInfo)
      11, 43 -> anc(stepInfo)
      16 -> bpl(stepInfo)
      24 -> clc(stepInfo)
      32 -> jsr(stepInfo)
      33, 37, 41, 45, 49, 53, 57, 61 -> and(stepInfo)
      35, 39, 47, 51, 55, 59, 63 -> rla(stepInfo)
      36, 44 -> bit(stepInfo)
      38, 42, 46, 54, 62 -> rol(stepInfo)
      40 -> plp(stepInfo)
      48 -> bmi(stepInfo)
      56 -> sec(stepInfo)
      64 -> rti(stepInfo)
      65, 69, 73, 77, 81, 85, 89, 93 -> eor(stepInfo)
      67, 71, 79, 83, 87, 91, 95 -> sre(stepInfo)
      70, 74, 78, 86, 94 -> lsr(stepInfo)
      72 -> pha(stepInfo)
      75 -> alr(stepInfo)
      76, 108 -> jmp(stepInfo)
      80 -> bvc(stepInfo)
      88 -> cli(stepInfo)
      96 -> rts(stepInfo)
      97, 101, 105, 109, 113, 117, 121, 125 -> adc(stepInfo)
      99, 103, 111, 115, 119, 123, 127 -> rra(stepInfo)
      102, 106, 110, 118, 126 -> ror(stepInfo)
      104 -> pla(stepInfo)
      107 -> arr(stepInfo)
      112 -> bvs(stepInfo)
      120 -> sei(stepInfo)
      129, 133, 141, 145, 149, 153, 157 -> sta(stepInfo)
      131, 135, 143, 151 -> sax(stepInfo)
      132, 140, 148 -> sty(stepInfo)
      134, 142, 150 -> stx(stepInfo)
      136 -> dey(stepInfo)
      138 -> txa(stepInfo)
      139 -> xaa(stepInfo)
      144 -> bcc(stepInfo)
      147, 159 -> ahx(stepInfo)
      152 -> tya(stepInfo)
      154 -> txs(stepInfo)
      155 -> tas(stepInfo)
      156 -> shy(stepInfo)
      158 -> shx(stepInfo)
      160, 164, 172, 180, 188 -> ldy(stepInfo)
      161, 165, 169, 173, 177, 181, 185, 189 -> lda(stepInfo)
      162, 166, 174, 182, 190 -> ldx(stepInfo)
      163, 167, 171, 175, 179, 183, 191 -> lax(stepInfo)
      168 -> tay(stepInfo)
      170 -> tax(stepInfo)
      176 -> bcs(stepInfo)
      184 -> clv(stepInfo)
      186 -> tsx(stepInfo)
      187 -> las(stepInfo)
      192, 196, 204 -> cpy(stepInfo)
      193, 197, 201, 205, 209, 213, 217, 221 -> cmp(stepInfo)
      195, 199, 207, 211, 215, 219, 223 -> dcp(stepInfo)
      198, 206, 214, 222 -> dec(stepInfo)
      200 -> iny(stepInfo)
      202 -> dex(stepInfo)
      203 -> axs(stepInfo)
      208 -> bne(stepInfo)
      216 -> cld(stepInfo)
      224, 228, 236 -> cpx(stepInfo)
      225, 229, 233, 235, 237, 241, 245, 249, 253 -> sbc(stepInfo)
      227, 231, 239, 243, 247, 251, 255 -> isc(stepInfo)
      230, 238, 246, 254 -> inc(stepInfo)
      232 -> inx(stepInfo)
      240 -> beq(stepInfo)
      248 -> sed(stepInfo)
    }
  }

  private fun isPageCrossed(mode: Int, address: Int) =
      when (mode) {
        AddressingMode.MODE_ABSOLUTE -> false
        AddressingMode.MODE_ABSOLUTEX -> pagesDiffer(address - X, address)
        AddressingMode.MODE_ABSOLUTEY -> pagesDiffer(address - Y, address)
        AddressingMode.MODE_ACCUMULATOR -> false
        AddressingMode.MODE_IMMEDIATE -> false
        AddressingMode.MODE_IMPLIED -> false
        AddressingMode.MODE_INDEXEDINDIRECT -> false
        AddressingMode.MODE_INDIRECT -> false
        AddressingMode.MODE_INDIRECTINDEXED -> pagesDiffer(address - Y, address)
        AddressingMode.MODE_RELATIVE -> false
        AddressingMode.MODE_ZEROPAGE -> false
        AddressingMode.MODE_ZEROPAGEX -> false
        AddressingMode.MODE_ZEROPAGEY -> false
        else -> throw RuntimeException("Invalid addressing mode $mode")
      }

  private fun pagesDiffer(a: Int, b: Int) =
      a and 0xFF00 != b and 0xFF00

  private fun addressForMode(mode: Int) =
      when (mode) {
        AddressingMode.MODE_ABSOLUTE -> read16(PC + 1)
        AddressingMode.MODE_ABSOLUTEX -> read16(PC + 1) + X
        AddressingMode.MODE_ABSOLUTEY -> read16(PC + 1) + Y
        AddressingMode.MODE_ACCUMULATOR -> 0
        AddressingMode.MODE_IMMEDIATE -> PC + 1
        AddressingMode.MODE_IMPLIED -> 0
        AddressingMode.MODE_INDEXEDINDIRECT -> read16bug(read(PC + 1) + X)
        AddressingMode.MODE_INDIRECT -> read16bug(read16(PC + 1))
        AddressingMode.MODE_INDIRECTINDEXED -> read16bug(read(PC + 1)) + Y
        AddressingMode.MODE_RELATIVE -> {
          val offset = read(PC + 1)
          if (offset < 0x80) PC + 2 + offset else PC + 2 + offset - 0x100
        }
        AddressingMode.MODE_ZEROPAGE -> read(PC + 1)
        AddressingMode.MODE_ZEROPAGEX -> (read(PC + 1) + X) and 0xff
        AddressingMode.MODE_ZEROPAGEY -> (read(PC + 1) + Y) and 0xff
        else -> throw RuntimeException("Invalid addressing mode $mode")
      }

  private fun stop() {
    TODO()
  }

  fun reset() {
    PC = read16(0xFFFC)
    SP = 0xFD
    setFlags(0x24)
  }

  // push pushes a byte onto the stack
  private fun push(value: Int) {
    write(0x100 or SP, value)
    SP = (SP - 1) and 0xFF
  }

  // push16 pushes two bytes onto the stack
  private fun push16(value: Int) {
    val hi = value shr 8
    val lo = value and 0xff
    push(hi)
    push(lo)
  }

  // pull pops a byte from the stack
  private fun pull(): Int {
    SP = (SP + 1) and 0xFF
    return read(0x100 or SP)
  }

  private fun pull16(): Int {
    val lo = pull()
    val hi = pull()
    return (hi shl 8) or lo
  }

  private fun addBranchCycles(info: StepInfo) {
    cycles++
    if (pagesDiffer(info.PC, info.address)) {
      cycles++
    }
  }

  fun flags(): Int {
    var flags = 0
    flags = flags or (C shl 0)
    flags = flags or (Z shl 1)
    flags = flags or (I shl 2)
    flags = flags or (D shl 3)
    flags = flags or (B shl 4)
    flags = flags or (U shl 5)
    flags = flags or (V shl 6)
    flags = flags or (N shl 7)
    return flags
  }

  // triggerIRQ causes an IRQ interrupt to occur on the next cycle
  fun triggerIRQ() {
    if (I == 0) {
      interrupt = Interrupt.IRQ
    }
  }

  // triggerNMI causes a non-maskable interrupt to occur on the next cycle
  fun triggerNMI() {
    interrupt = Interrupt.NMI
  }

  private fun setZN(value: Int) {
    setZFlag(value)
    setNFlag(value)
  }

  private fun setZFlag(value: Int) {
    Z = if (value == 0) 1 else 0
  }

  // setN sets the negative flag if the argument is negative (high bit is set)
  private fun setNFlag(value: Int) {
    N = if (value and 0x80 != 0) 1 else 0
  }

  private fun compare(a: Int, b: Int) {
    setZN(a - b)
    C = if (a >= b) 1 else 0
  }

  private fun setFlags(flags: Int) {
    C = (flags shr 0) and 1
    Z = (flags shr 1) and 1
    I = (flags shr 2) and 1
    D = (flags shr 3) and 1
    B = (flags shr 4) and 1
    U = (flags shr 5) and 1
    V = (flags shr 6) and 1
    N = (flags shr 7) and 1
  }

  /**
   * Instructions below
   */
  // ADC - Add with Carry
  private fun adc(info: StepInfo) {
    val a = A
    val b = read(info.address)
    val c = C
    A = (a + b + c) and 0xFF
    setZN(A)
    C = if (a + b + c > 0xFF) 1 else 0
    V = if ((a xor b) and 0x80 == 0 && (a xor A) and 0x80 != 0) 1 else 0
  }

  // AND - Logical AND
  private fun and(info: StepInfo) {
    A = A and read(info.address)
    setZN(A)
  }

  // ASL - Arithmetic Shift Left
  private fun asl(info: StepInfo) {
    if (info.mode == AddressingMode.MODE_ACCUMULATOR) {
      C = (A shr 7) and 1
      A = A shl 1
      setZN(A)
    } else {
      var value = read(info.address)
      C = (value shr 7) and 1
      value = value shl 1
      write(info.address, value)
      setZN(value)
    }
  }

  // BCC - Branch if Carry Clear
  private fun bcc(info: StepInfo) {
    if (C == 0) {
      PC = info.address
      addBranchCycles(info)
    }
  }

  // BCS - Branch if Carry Set
  private fun bcs(info: StepInfo) {
    if (C != 0) {
      PC = info.address
      addBranchCycles(info)
    }
  }

  // BEQ - Branch if Equal
  private fun beq(info: StepInfo) {
    if (Z != 0) {
      PC = info.address
      addBranchCycles(info)
    }
  }

  // BIT - Bit Test
  private fun bit(info: StepInfo) {
    val value = read(info.address)
    V = (value shr 6) and 1
    setZFlag(value and A)
    setNFlag(value)
  }

  // BMI - Branch if Minus
  private fun bmi(info: StepInfo) {
    if (N != 0) {
      PC = info.address
      addBranchCycles(info)
    }
  }

  // BNE - Branch if Not Equal
  private fun bne(info: StepInfo) {
    if (Z == 0) {
      PC = info.address
      addBranchCycles(info)
    }
  }

  // BPL - Branch if Positive
  private fun bpl(info: StepInfo) {
    if (N == 0) {
      PC = info.address
      addBranchCycles(info)
    }
  }

  // BRK - Force Interrupt
  private fun brk(info: StepInfo) {
    push16(PC)
    php(info)
    sei(info)
    PC = read16(0xFFFE)
  }

  // BVC - Branch if Overflow Clear
  private fun bvc(info: StepInfo) {
    if (V == 0) {
      PC = info.address
      addBranchCycles(info)
    }
  }

  // BVS - Branch if Overflow Set
  private fun bvs(info: StepInfo) {
    if (V != 0) {
      PC = info.address
      addBranchCycles(info)
    }
  }

  // CLC - Clear Carry Flag
  private fun clc(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    C = 0
  }

  // CLD - Clear Decimal Mode
  private fun cld(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    D = 0
  }

  // CLI - Clear Interrupt Disable
  private fun cli(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    I = 0
  }

  // CLV - Clear Overflow Flag
  private fun clv(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    V = 0
  }

  // CMP - Compare
  private fun cmp(info: StepInfo) {
    val value = read(info.address) and 0xFF
    compare(A, value)
  }

  // CPX - Compare X Register
  private fun cpx(info: StepInfo) {
    val value = read(info.address) and 0xFF
    compare(X, value)
  }

  // CPY - Compare Y Register
  private fun cpy(info: StepInfo) {
    val value = read(info.address) and 0xFF
    compare(Y, value)
  }

  // DEC - Decrement Memory
  private fun dec(info: StepInfo) {
    val value = read(info.address) - 1
    write(info.address, value)
    setZN(value)
  }

  // DEX - Decrement X Register
  private fun dex(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    X = (X - 1) and 0xFF
    setZN(X)
  }

  // DEY - Decrement Y Register
  private fun dey(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    Y = (Y - 1) and 0xFF
    setZN(Y)
  }

  // EOR - Exclusive OR
  private fun eor(info: StepInfo) {
    A = A xor read(info.address)
    setZN(A)
  }

  // INC - Increment Memory
  private fun inc(info: StepInfo) {
    val value = (read(info.address) + 1) and 0xFF
    write(info.address, value)
    setZN(value)
  }

  // INX - Increment X Register
  private fun inx(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    X = (X + 1) and 0xFF
    setZN(X)
  }

  // INY - Increment Y Register
  private fun iny(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    Y = (Y + 1) and 0xFF
    setZN(Y)
  }

  // JMP - Jump
  private fun jmp(info: StepInfo) {
    PC = info.address
  }

  // JSR - Jump to Subroutine
  private fun jsr(info: StepInfo) {
    push16(PC - 1)
    PC = info.address
  }

  // LDA - Load Accumulator
  private fun lda(info: StepInfo) {
    A = read(info.address) and 0xFF
    setZN(A)
  }

  // LDX - Load X Register
  private fun ldx(info: StepInfo) {
    X = read(info.address) and 0xFF
    setZN(X)
  }

  // LDY - Load Y Register
  private fun ldy(info: StepInfo) {
    Y = read(info.address) and 0xFF
    setZN(Y)
  }

  // LSR - Logical Shift Right
  private fun lsr(info: StepInfo) {
    if (info.mode == AddressingMode.MODE_ACCUMULATOR) {
      C = A and 1
      A = A shr 1
      setZN(A)
    } else {
      var value = read(info.address)
      C = value and 1
      value = value shr 1
      write(info.address, value)
      setZN(value)
    }
  }

  // NOP - No Operation
  private fun nop(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  // ORA - Logical Inclusive OR
  private fun ora(info: StepInfo) {
    A = A or (read(info.address) and 0xFF)
    setZN(A)
  }

  // PHA - Push Accumulator
  private fun pha(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    push(A)
  }

  // PHP - Push Processor Status
  private fun php(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    push(flags() or 0x10)
  }

  // PLA - Pull Accumulator
  private fun pla(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    A = pull()
    setZN(A)
  }

  // PLP - Pull Processor Status
  private fun plp(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    setFlags(pull() and 0xEF or 0x20)
  }

  // ROL - Rotate Left
  private fun rol(info: StepInfo) {
    if (info.mode == AddressingMode.MODE_ACCUMULATOR) {
      val c = C
      C = (A shr 7) and 1
      A = (A shl 1) or c
      setZN(A)
    } else {
      val c = C
      var value = read(info.address)
      C = (value shr 7) and 1
      value = (value shl 1) or c
      write(info.address, value)
      setZN(value and 0xFF)
    }
  }

  // ROR - Rotate Right
  private fun ror(info: StepInfo) {
    if (info.mode == AddressingMode.MODE_ACCUMULATOR) {
      val c = C
      C = A and 1
      A = (A shr 1) or (c shl 7)
      setZN(A)
    } else {
      val c = C
      var value = read(info.address)
      C = value and 1
      value = (value shr 1) or (c shl 7)
      write(info.address, value)
      setZN(value)
    }
  }

  // RTI - Return from Interrupt
  private fun rti(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    setFlags(pull() and 0xEF or 0x20)
    PC = pull16()
  }

  // RTS - Return from Subroutine
  private fun rts(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    PC = pull16() + 1
  }

  // SBC - Subtract with Carry
  private fun sbc(info: StepInfo) {
    val a = A
    val b = read(info.address)
    val c = C
    A = (a - b - ((1 - c) and 0xFF)) and 0xFF
    setZN(A)
    C = if (a - b - ((1 - c) and 0xFF) >= 0) 1 else 0
    V = if ((a xor b) and 0x80 != 0 && (a xor A) and 0x80 != 0) 1 else 0
  }

  // SEC - Set Carry Flag
  private fun sec(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    C = 1
  }

  // SED - Set Decimal Flag
  private fun sed(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    D = 1
  }

  // SEI - Set Interrupt Disable
  private fun sei(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    I = 1
  }

  // STA - Store Accumulator
  private fun sta(info: StepInfo) {
    write(info.address, A)
  }

  // STX - Store X Register
  private fun stx(info: StepInfo) {
    write(info.address, X)
  }

  // STY - Store Y Register
  private fun sty(info: StepInfo) {
    write(info.address, Y)
  }

  // TAX - Transfer Accumulator to X
  private fun tax(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    X = A
    setZN(X)
  }

  // TAY - Transfer Accumulator to Y
  private fun tay(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    Y = A
    setZN(Y)
  }

  // TSX - Transfer Stack Pointer to X
  private fun tsx(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    X = SP
    setZN(X)
  }

  // TXA - Transfer X to Accumulator
  private fun txa(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    A = X
    setZN(A)
  }

  // TXS - Transfer X to Stack Pointer
  private fun txs(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    SP = X
  }

  // TYA - Transfer Y to Accumulator
  private fun tya(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
    A = Y
    setZN(A)
  }

  // illegal opcodes below
  private fun ahx(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun alr(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun anc(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun arr(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun axs(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun dcp(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun isc(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun kil(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun las(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun lax(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun rla(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun rra(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun sax(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun shx(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun shy(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun slo(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun sre(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun tas(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  private fun xaa(@Suppress("UNUSED_PARAMETER") info: StepInfo) {
  }

  companion object {
    const val FREQUENCY = 1789773
    val instructionModes = arrayOf(
        6, 7, 6, 7, 11, 11, 11, 11, 6, 5, 4, 5, 1, 1, 1, 1,
        10, 9, 6, 9, 12, 12, 12, 12, 6, 3, 6, 3, 2, 2, 2, 2,
        1, 7, 6, 7, 11, 11, 11, 11, 6, 5, 4, 5, 1, 1, 1, 1,
        10, 9, 6, 9, 12, 12, 12, 12, 6, 3, 6, 3, 2, 2, 2, 2,
        6, 7, 6, 7, 11, 11, 11, 11, 6, 5, 4, 5, 1, 1, 1, 1,
        10, 9, 6, 9, 12, 12, 12, 12, 6, 3, 6, 3, 2, 2, 2, 2,
        6, 7, 6, 7, 11, 11, 11, 11, 6, 5, 4, 5, 8, 1, 1, 1,
        10, 9, 6, 9, 12, 12, 12, 12, 6, 3, 6, 3, 2, 2, 2, 2,
        5, 7, 5, 7, 11, 11, 11, 11, 6, 5, 6, 5, 1, 1, 1, 1,
        10, 9, 6, 9, 12, 12, 13, 13, 6, 3, 6, 3, 2, 2, 3, 3,
        5, 7, 5, 7, 11, 11, 11, 11, 6, 5, 6, 5, 1, 1, 1, 1,
        10, 9, 6, 9, 12, 12, 13, 13, 6, 3, 6, 3, 2, 2, 3, 3,
        5, 7, 5, 7, 11, 11, 11, 11, 6, 5, 6, 5, 1, 1, 1, 1,
        10, 9, 6, 9, 12, 12, 12, 12, 6, 3, 6, 3, 2, 2, 2, 2,
        5, 7, 5, 7, 11, 11, 11, 11, 6, 5, 6, 5, 1, 1, 1, 1,
        10, 9, 6, 9, 12, 12, 12, 12, 6, 3, 6, 3, 2, 2, 2, 2
    )
    val instructionSizes = arrayOf(
        1, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0,
        2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0,
        3, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0,
        2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0,
        1, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0,
        2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0,
        1, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0,
        2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0,
        2, 2, 0, 0, 2, 2, 2, 0, 1, 0, 1, 0, 3, 3, 3, 0,
        2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 0, 3, 0, 0,
        2, 2, 2, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0,
        2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0,
        2, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0,
        2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0,
        2, 2, 0, 0, 2, 2, 2, 0, 1, 2, 1, 0, 3, 3, 3, 0,
        2, 2, 0, 0, 2, 2, 2, 0, 1, 3, 1, 0, 3, 3, 3, 0
    )
    val instructionCycles = arrayOf(
        7, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 4, 4, 6, 6,
        2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        6, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 4, 4, 6, 6,
        2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        6, 6, 2, 8, 3, 3, 5, 5, 3, 2, 2, 2, 3, 4, 6, 6,
        2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        6, 6, 2, 8, 3, 3, 5, 5, 4, 2, 2, 2, 5, 4, 6, 6,
        2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
        2, 6, 2, 6, 4, 4, 4, 4, 2, 5, 2, 5, 5, 5, 5, 5,
        2, 6, 2, 6, 3, 3, 3, 3, 2, 2, 2, 2, 4, 4, 4, 4,
        2, 5, 2, 5, 4, 4, 4, 4, 2, 4, 2, 4, 4, 4, 4, 4,
        2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
        2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7,
        2, 6, 2, 8, 3, 3, 5, 5, 2, 2, 2, 2, 4, 4, 6, 6,
        2, 5, 2, 8, 4, 4, 6, 6, 2, 4, 2, 7, 4, 4, 7, 7
    )
    val instructionPageCycles = arrayOf(
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 1,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0
    )
    val instructionNames = arrayOf(
        "BRK", "ORA", "KIL", "SLO", "NOP", "ORA", "ASL", "SLO",
        "PHP", "ORA", "ASL", "ANC", "NOP", "ORA", "ASL", "SLO",
        "BPL", "ORA", "KIL", "SLO", "NOP", "ORA", "ASL", "SLO",
        "CLC", "ORA", "NOP", "SLO", "NOP", "ORA", "ASL", "SLO",
        "JSR", "AND", "KIL", "RLA", "BIT", "AND", "ROL", "RLA",
        "PLP", "AND", "ROL", "ANC", "BIT", "AND", "ROL", "RLA",
        "BMI", "AND", "KIL", "RLA", "NOP", "AND", "ROL", "RLA",
        "SEC", "AND", "NOP", "RLA", "NOP", "AND", "ROL", "RLA",
        "RTI", "EOR", "KIL", "SRE", "NOP", "EOR", "LSR", "SRE",
        "PHA", "EOR", "LSR", "ALR", "JMP", "EOR", "LSR", "SRE",
        "BVC", "EOR", "KIL", "SRE", "NOP", "EOR", "LSR", "SRE",
        "CLI", "EOR", "NOP", "SRE", "NOP", "EOR", "LSR", "SRE",
        "RTS", "ADC", "KIL", "RRA", "NOP", "ADC", "ROR", "RRA",
        "PLA", "ADC", "ROR", "ARR", "JMP", "ADC", "ROR", "RRA",
        "BVS", "ADC", "KIL", "RRA", "NOP", "ADC", "ROR", "RRA",
        "SEI", "ADC", "NOP", "RRA", "NOP", "ADC", "ROR", "RRA",
        "NOP", "STA", "NOP", "SAX", "STY", "STA", "STX", "SAX",
        "DEY", "NOP", "TXA", "XAA", "STY", "STA", "STX", "SAX",
        "BCC", "STA", "KIL", "AHX", "STY", "STA", "STX", "SAX",
        "TYA", "STA", "TXS", "TAS", "SHY", "STA", "SHX", "AHX",
        "LDY", "LDA", "LDX", "LAX", "LDY", "LDA", "LDX", "LAX",
        "TAY", "LDA", "TAX", "LAX", "LDY", "LDA", "LDX", "LAX",
        "BCS", "LDA", "KIL", "LAX", "LDY", "LDA", "LDX", "LAX",
        "CLV", "LDA", "TSX", "LAS", "LDY", "LDA", "LDX", "LAX",
        "CPY", "CMP", "NOP", "DCP", "CPY", "CMP", "DEC", "DCP",
        "INY", "CMP", "DEX", "AXS", "CPY", "CMP", "DEC", "DCP",
        "BNE", "CMP", "KIL", "DCP", "NOP", "CMP", "DEC", "DCP",
        "CLD", "CMP", "NOP", "DCP", "NOP", "CMP", "DEC", "DCP",
        "CPX", "SBC", "NOP", "ISC", "CPX", "SBC", "INC", "ISC",
        "INX", "SBC", "NOP", "SBC", "CPX", "SBC", "INC", "ISC",
        "BEQ", "SBC", "KIL", "ISC", "NOP", "SBC", "INC", "ISC",
        "SED", "SBC", "NOP", "ISC", "NOP", "SBC", "INC", "ISC"
    )
  }
}