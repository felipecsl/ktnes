@file:Suppress("UNUSED_PARAMETER")

package com.felipecsl.knes

internal class CPU(private val stepCallback: CPUStepCallback? = null) {
  lateinit var console: Console

  private var stepAddress: Int = 0
  private var stepPC: Int = 0
  private var stepMode: Int = 0
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
      else -> throw RuntimeException("unhandled cpu memory read at address: $address")
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
    // execute interrupt
    if (interrupt == Interrupt.NMI)
      nmi()
    else if (interrupt == Interrupt.IRQ)
      irq()
    interrupt = Interrupt.NONE
    executeNextInstruction()
    return cycles - currCycles
  }

  private fun irq() {
    push16(PC)
    stepAddress = 0
    stepPC = 0
    stepMode = 0
    php(stepAddress, stepPC, stepMode)
    PC = read16(0xfffe)
    I = 1
    cycles += 7
  }

  private fun nmi() {
    push16(PC)
    stepAddress = 0
    stepPC = 0
    stepMode = 0
    php(stepAddress, stepPC, stepMode)
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
    stepAddress = address
    stepPC = PC
    stepMode = mode
    when (opcode) {
      0 -> brk(stepAddress, stepPC, stepMode)
      1, 5, 9, 13, 17, 21, 25, 29 -> ora(stepAddress, stepPC, stepMode)
      2, 18, 34, 50, 66, 82, 98, 114, 146, 178, 210, 242 -> kil(stepAddress, stepPC, stepMode)
      3, 7, 15, 19, 23, 27, 31 -> slo(stepAddress, stepPC, stepMode)
      4, 12, 20, 26, 28, 52, 58, 60, 68, 84, 90, 92, 100, 116, 122, 124, 128, 130, 137, 194, 212, 218, 220, 226, 234, 244, 250, 252 -> nop(stepAddress, stepPC, stepMode)
      6, 10, 14, 22, 30 -> asl(stepAddress, stepPC, stepMode)
      8 -> php(stepAddress, stepPC, stepMode)
      11, 43 -> anc(stepAddress, stepPC, stepMode)
      16 -> bpl(stepAddress, stepPC, stepMode)
      24 -> clc(stepAddress, stepPC, stepMode)
      32 -> jsr(stepAddress, stepPC, stepMode)
      33, 37, 41, 45, 49, 53, 57, 61 -> and(stepAddress, stepPC, stepMode)
      35, 39, 47, 51, 55, 59, 63 -> rla(stepAddress, stepPC, stepMode)
      36, 44 -> bit(stepAddress, stepPC, stepMode)
      38, 42, 46, 54, 62 -> rol(stepAddress, stepPC, stepMode)
      40 -> plp(stepAddress, stepPC, stepMode)
      48 -> bmi(stepAddress, stepPC, stepMode)
      56 -> sec(stepAddress, stepPC, stepMode)
      64 -> rti(stepAddress, stepPC, stepMode)
      65, 69, 73, 77, 81, 85, 89, 93 -> eor(stepAddress, stepPC, stepMode)
      67, 71, 79, 83, 87, 91, 95 -> sre(stepAddress, stepPC, stepMode)
      70, 74, 78, 86, 94 -> lsr(stepAddress, stepPC, stepMode)
      72 -> pha(stepAddress, stepPC, stepMode)
      75 -> alr(stepAddress, stepPC, stepMode)
      76, 108 -> jmp(stepAddress, stepPC, stepMode)
      80 -> bvc(stepAddress, stepPC, stepMode)
      88 -> cli(stepAddress, stepPC, stepMode)
      96 -> rts(stepAddress, stepPC, stepMode)
      97, 101, 105, 109, 113, 117, 121, 125 -> adc(stepAddress, stepPC, stepMode)
      99, 103, 111, 115, 119, 123, 127 -> rra(stepAddress, stepPC, stepMode)
      102, 106, 110, 118, 126 -> ror(stepAddress, stepPC, stepMode)
      104 -> pla(stepAddress, stepPC, stepMode)
      107 -> arr(stepAddress, stepPC, stepMode)
      112 -> bvs(stepAddress, stepPC, stepMode)
      120 -> sei(stepAddress, stepPC, stepMode)
      129, 133, 141, 145, 149, 153, 157 -> sta(stepAddress, stepPC, stepMode)
      131, 135, 143, 151 -> sax(stepAddress, stepPC, stepMode)
      132, 140, 148 -> sty(stepAddress, stepPC, stepMode)
      134, 142, 150 -> stx(stepAddress, stepPC, stepMode)
      136 -> dey(stepAddress, stepPC, stepMode)
      138 -> txa(stepAddress, stepPC, stepMode)
      139 -> xaa(stepAddress, stepPC, stepMode)
      144 -> bcc(stepAddress, stepPC, stepMode)
      147, 159 -> ahx(stepAddress, stepPC, stepMode)
      152 -> tya(stepAddress, stepPC, stepMode)
      154 -> txs(stepAddress, stepPC, stepMode)
      155 -> tas(stepAddress, stepPC, stepMode)
      156 -> shy(stepAddress, stepPC, stepMode)
      158 -> shx(stepAddress, stepPC, stepMode)
      160, 164, 172, 180, 188 -> ldy(stepAddress, stepPC, stepMode)
      161, 165, 169, 173, 177, 181, 185, 189 -> lda(stepAddress, stepPC, stepMode)
      162, 166, 174, 182, 190 -> ldx(stepAddress, stepPC, stepMode)
      163, 167, 171, 175, 179, 183, 191 -> lax(stepAddress, stepPC, stepMode)
      168 -> tay(stepAddress, stepPC, stepMode)
      170 -> tax(stepAddress, stepPC, stepMode)
      176 -> bcs(stepAddress, stepPC, stepMode)
      184 -> clv(stepAddress, stepPC, stepMode)
      186 -> tsx(stepAddress, stepPC, stepMode)
      187 -> las(stepAddress, stepPC, stepMode)
      192, 196, 204 -> cpy(stepAddress, stepPC, stepMode)
      193, 197, 201, 205, 209, 213, 217, 221 -> cmp(stepAddress, stepPC, stepMode)
      195, 199, 207, 211, 215, 219, 223 -> dcp(stepAddress, stepPC, stepMode)
      198, 206, 214, 222 -> dec(stepAddress, stepPC, stepMode)
      200 -> iny(stepAddress, stepPC, stepMode)
      202 -> dex(stepAddress, stepPC, stepMode)
      203 -> axs(stepAddress, stepPC, stepMode)
      208 -> bne(stepAddress, stepPC, stepMode)
      216 -> cld(stepAddress, stepPC, stepMode)
      224, 228, 236 -> cpx(stepAddress, stepPC, stepMode)
      225, 229, 233, 235, 237, 241, 245, 249, 253 -> sbc(stepAddress, stepPC, stepMode)
      227, 231, 239, 243, 247, 251, 255 -> isc(stepAddress, stepPC, stepMode)
      230, 238, 246, 254 -> inc(stepAddress, stepPC, stepMode)
      232 -> inx(stepAddress, stepPC, stepMode)
      240 -> beq(stepAddress, stepPC, stepMode)
      248 -> sed(stepAddress, stepPC, stepMode)
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

  private fun addBranchCycles(stepAddress: Int, stepPC: Int, stepMode: Int) {
    cycles++
    if (pagesDiffer(stepPC, stepAddress)) {
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
  private fun adc(stepAddress: Int, stepPC: Int, stepMode: Int) {
    val a = A
    val b = read(stepAddress)
    val c = C
    A = (a + b + c) and 0xFF
    setZN(A)
    C = if (a + b + c > 0xFF) 1 else 0
    V = if ((a xor b) and 0x80 == 0 && (a xor A) and 0x80 != 0) 1 else 0
  }

  // AND - Logical AND
  private fun and(stepAddress: Int, stepPC: Int, stepMode: Int) {
    A = A and read(stepAddress)
    setZN(A)
  }

  // ASL - Arithmetic Shift Left
  private fun asl(stepAddress: Int, stepPC: Int, stepMode: Int) {
    if (stepMode == AddressingMode.MODE_ACCUMULATOR) {
      C = (A shr 7) and 1
      A = A shl 1
      setZN(A)
    } else {
      var value = read(stepAddress)
      C = (value shr 7) and 1
      value = value shl 1
      write(stepAddress, value)
      setZN(value)
    }
  }

  // BCC - Branch if Carry Clear
  private fun bcc(stepAddress: Int, stepPC: Int, stepMode: Int) {
    if (C == 0) {
      PC = stepAddress
      addBranchCycles(stepAddress, stepPC, stepMode)
    }
  }

  // BCS - Branch if Carry Set
  private fun bcs(stepAddress: Int, stepPC: Int, stepMode: Int) {
    if (C != 0) {
      PC = stepAddress
      addBranchCycles(stepAddress, stepPC, stepMode)
    }
  }

  // BEQ - Branch if Equal
  private fun beq(stepAddress: Int, stepPC: Int, stepMode: Int) {
    if (Z != 0) {
      PC = stepAddress
      addBranchCycles(stepAddress, stepPC, stepMode)
    }
  }

  // BIT - Bit Test
  private fun bit(stepAddress: Int, stepPC: Int, stepMode: Int) {
    val value = read(stepAddress)
    V = (value shr 6) and 1
    setZFlag(value and A)
    setNFlag(value)
  }

  // BMI - Branch if Minus
  private fun bmi(stepAddress: Int, stepPC: Int, stepMode: Int) {
    if (N != 0) {
      PC = stepAddress
      addBranchCycles(stepAddress, stepPC, stepMode)
    }
  }

  // BNE - Branch if Not Equal
  private fun bne(stepAddress: Int, stepPC: Int, stepMode: Int) {
    if (Z == 0) {
      PC = stepAddress
      addBranchCycles(stepAddress, stepPC, stepMode)
    }
  }

  // BPL - Branch if Positive
  private fun bpl(stepAddress: Int, stepPC: Int, stepMode: Int) {
    if (N == 0) {
      PC = stepAddress
      addBranchCycles(stepAddress, stepPC, stepMode)
    }
  }

  // BRK - Force Interrupt
  private fun brk(stepAddress: Int, stepPC: Int, stepMode: Int) {
    push16(PC)
    php(stepAddress, stepPC, stepMode)
    sei(stepAddress, stepPC, stepMode)
    PC = read16(0xFFFE)
  }

  // BVC - Branch if Overflow Clear
  private fun bvc(stepAddress: Int, stepPC: Int, stepMode: Int) {
    if (V == 0) {
      PC = stepAddress
      addBranchCycles(stepAddress, stepPC, stepMode)
    }
  }

  // BVS - Branch if Overflow Set
  private fun bvs(stepAddress: Int, stepPC: Int, stepMode: Int) {
    if (V != 0) {
      PC = stepAddress
      addBranchCycles(stepAddress, stepPC, stepMode)
    }
  }

  // CLC - Clear Carry Flag
  private fun clc(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    C = 0
  }

  // CLD - Clear Decimal Mode
  private fun cld(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    D = 0
  }

  // CLI - Clear Interrupt Disable
  private fun cli(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    I = 0
  }

  // CLV - Clear Overflow Flag
  private fun clv(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    V = 0
  }

  // CMP - Compare
  private fun cmp(stepAddress: Int, stepPC: Int, stepMode: Int) {
    val value = read(stepAddress) and 0xFF
    compare(A, value)
  }

  // CPX - Compare X Register
  private fun cpx(stepAddress: Int, stepPC: Int, stepMode: Int) {
    val value = read(stepAddress) and 0xFF
    compare(X, value)
  }

  // CPY - Compare Y Register
  private fun cpy(stepAddress: Int, stepPC: Int, stepMode: Int) {
    val value = read(stepAddress) and 0xFF
    compare(Y, value)
  }

  // DEC - Decrement Memory
  private fun dec(stepAddress: Int, stepPC: Int, stepMode: Int) {
    val value = read(stepAddress) - 1
    write(stepAddress, value)
    setZN(value)
  }

  // DEX - Decrement X Register
  private fun dex(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    X = (X - 1) and 0xFF
    setZN(X)
  }

  // DEY - Decrement Y Register
  private fun dey(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    Y = (Y - 1) and 0xFF
    setZN(Y)
  }

  // EOR - Exclusive OR
  private fun eor(stepAddress: Int, stepPC: Int, stepMode: Int) {
    A = A xor read(stepAddress)
    setZN(A)
  }

  // INC - Increment Memory
  private fun inc(stepAddress: Int, stepPC: Int, stepMode: Int) {
    val value = (read(stepAddress) + 1) and 0xFF
    write(stepAddress, value)
    setZN(value)
  }

  // INX - Increment X Register
  private fun inx(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    X = (X + 1) and 0xFF
    setZN(X)
  }

  // INY - Increment Y Register
  private fun iny(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    Y = (Y + 1) and 0xFF
    setZN(Y)
  }

  // JMP - Jump
  private fun jmp(stepAddress: Int, stepPC: Int, stepMode: Int) {
    PC = stepAddress
  }

  // JSR - Jump to Subroutine
  private fun jsr(stepAddress: Int, stepPC: Int, stepMode: Int) {
    push16(PC - 1)
    PC = stepAddress
  }

  // LDA - Load Accumulator
  private fun lda(stepAddress: Int, stepPC: Int, stepMode: Int) {
    A = read(stepAddress) and 0xFF
    setZN(A)
  }

  // LDX - Load X Register
  private fun ldx(stepAddress: Int, stepPC: Int, stepMode: Int) {
    X = read(stepAddress) and 0xFF
    setZN(X)
  }

  // LDY - Load Y Register
  private fun ldy(stepAddress: Int, stepPC: Int, stepMode: Int) {
    Y = read(stepAddress) and 0xFF
    setZN(Y)
  }

  // LSR - Logical Shift Right
  private fun lsr(stepAddress: Int, stepPC: Int, stepMode: Int) {
    if (stepMode == AddressingMode.MODE_ACCUMULATOR) {
      C = A and 1
      A = A shr 1
      setZN(A)
    } else {
      var value = read(stepAddress)
      C = value and 1
      value = value shr 1
      write(stepAddress, value)
      setZN(value)
    }
  }

  // NOP - No Operation
  private fun nop(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  // ORA - Logical Inclusive OR
  private fun ora(stepAddress: Int, stepPC: Int, stepMode: Int) {
    A = A or (read(stepAddress) and 0xFF)
    setZN(A)
  }

  // PHA - Push Accumulator
  private fun pha(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    push(A)
  }

  // PHP - Push Processor Status
  private fun php(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    push(flags() or 0x10)
  }

  // PLA - Pull Accumulator
  private fun pla(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    A = pull()
    setZN(A)
  }

  // PLP - Pull Processor Status
  private fun plp(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    setFlags(pull() and 0xEF or 0x20)
  }

  // ROL - Rotate Left
  private fun rol(stepAddress: Int, stepPC: Int, stepMode: Int) {
    if (stepMode == AddressingMode.MODE_ACCUMULATOR) {
      val c = C
      C = (A shr 7) and 1
      A = (A shl 1) or c
      setZN(A)
    } else {
      val c = C
      var value = read(stepAddress)
      C = (value shr 7) and 1
      value = (value shl 1) or c
      write(stepAddress, value)
      setZN(value and 0xFF)
    }
  }

  // ROR - Rotate Right
  private fun ror(stepAddress: Int, stepPC: Int, stepMode: Int) {
    if (stepMode == AddressingMode.MODE_ACCUMULATOR) {
      val c = C
      C = A and 1
      A = (A shr 1) or (c shl 7)
      setZN(A)
    } else {
      val c = C
      var value = read(stepAddress)
      C = value and 1
      value = (value shr 1) or (c shl 7)
      write(stepAddress, value)
      setZN(value)
    }
  }

  // RTI - Return from Interrupt
  private fun rti(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    setFlags(pull() and 0xEF or 0x20)
    PC = pull16()
  }

  // RTS - Return from Subroutine
  private fun rts(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    PC = pull16() + 1
  }

  // SBC - Subtract with Carry
  private fun sbc(stepAddress: Int, stepPC: Int, stepMode: Int) {
    val a = A
    val b = read(stepAddress)
    val c = C
    A = (a - b - ((1 - c) and 0xFF)) and 0xFF
    setZN(A)
    C = if (a - b - ((1 - c) and 0xFF) >= 0) 1 else 0
    V = if ((a xor b) and 0x80 != 0 && (a xor A) and 0x80 != 0) 1 else 0
  }

  // SEC - Set Carry Flag
  private fun sec(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    C = 1
  }

  // SED - Set Decimal Flag
  private fun sed(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    D = 1
  }

  // SEI - Set Interrupt Disable
  private fun sei(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    I = 1
  }

  // STA - Store Accumulator
  private fun sta(stepAddress: Int, stepPC: Int, stepMode: Int) {
    write(stepAddress, A)
  }

  // STX - Store X Register
  private fun stx(stepAddress: Int, stepPC: Int, stepMode: Int) {
    write(stepAddress, X)
  }

  // STY - Store Y Register
  private fun sty(stepAddress: Int, stepPC: Int, stepMode: Int) {
    write(stepAddress, Y)
  }

  // TAX - Transfer Accumulator to X
  private fun tax(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    X = A
    setZN(X)
  }

  // TAY - Transfer Accumulator to Y
  private fun tay(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    Y = A
    setZN(Y)
  }

  // TSX - Transfer Stack Pointer to X
  private fun tsx(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    X = SP
    setZN(X)
  }

  // TXA - Transfer X to Accumulator
  private fun txa(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    A = X
    setZN(A)
  }

  // TXS - Transfer X to Stack Pointer
  private fun txs(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    SP = X
  }

  // TYA - Transfer Y to Accumulator
  private fun tya(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
    A = Y
    setZN(A)
  }

  // illegal opcodes below
  private fun ahx(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun alr(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun anc(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun arr(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun axs(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun dcp(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun isc(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun kil(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun las(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun lax(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun rla(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun rra(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun sax(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun shx(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun shy(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun slo(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun sre(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun tas(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
  }

  private fun xaa(@Suppress("UNUSED_PARAMETER") stepAddress: Int, stepPC: Int, stepMode: Int) {
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
  }
}