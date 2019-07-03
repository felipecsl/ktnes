@file:Suppress("UNUSED_PARAMETER", "NOTHING_TO_INLINE")

package com.felipecsl.knes

internal class CPU(
    private val mapper: Mapper,
    private val ppu: PPU,
    private val apu: APU,
    val controller1: Controller,
    private val controller2: Controller,
    private var ram: IntArray = IntArray(2048),
    private val stepCallback: CPUStepCallback? = null
) {
  private var stepAddress: Int = 0
  private var stepPC: Int = 0
  private var stepMode: Int = 0
  internal var cycles: Double = 0.0         // number of cycles
  private var PC: Int = 0                   // (Byte) Program counter
  private var SP: Int = 0xFF                // (Byte) Stack pointer
  private var A: Int = 0                    // (Byte) Accumulator
  private var X: Int = 0                    // (Byte) Register X
  private var Y: Int = 0                    // (Byte) Register Y
  private var C: Int = 0                    // (Byte) carry flag
  private var Z: Int = 0                    // (Byte) zero flag
  internal var I: Int = 0                    // (Byte) interrupt disable flag
  private var D: Int = 0                    // (Byte) decimal mode flag
  private var B: Int = 0                    // (Byte) break command flag
  private var U: Int = 0                    // (Byte) unused flag
  private var V: Int = 0                    // (Byte) overflow flag
  private var N: Int = 0                    // (Byte) negative flag
  internal var stall: Int = 0                // number of cycles to stall
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
  var interrupt: Int = Interrupt.NOT_SET

  private inline fun read16(address: Int): Int {
    return (read(address + 1) shl 8) or read(address)
  }

  // read16bug emulates a 6502 bug that caused the low byte to wrap without
  // incrementing the high byte
  private fun read16bug(address: Int): Int {
    val b = (address and 0xFF00) or (address + 1)
    val lo = read(address)
    val hi = read(b)
    return (hi shl 8) or lo
  }

  /** returns Byte */
  fun read(address: Int): Int {
    return when {
      address < 0x2000 -> ram[address % 0x0800]
      address < 0x4000 -> ppu.readRegister(0x2000 + address % 8)
      address == 0x4014 -> ppu.readRegister(address)
      address == 0x4015 -> apu.readRegister(address)
      address == 0x4016 -> controller1.read()
      address == 0x4017 -> controller2.read()
      //address < 0x6000 -> TODO: I/O registers
      address >= 0x6000 -> mapper.read(address)
      else -> throw RuntimeException("unhandled cpu memory read at address: $address")
    }
  }

  private fun write(address: Int, value: Int /* Byte */) {
    when {
      address < 0x2000 ->
        ram[address % 0x0800] = value
      address < 0x4000 ->
        ppu.writeRegister(0x2000 + address % 8, value)
      address < 0x4014 ->
        apu.writeRegister(address, value)
      address == 0x4014 ->
        ppu.writeRegister(address, value)
      address == 0x4015 ->
        apu.writeRegister(address, value)
      address == 0x4016 -> {
        controller1.write(value)
        controller2.write(value)
      }
      address == 0x4017 ->
        apu.writeRegister(address, value)
      //address < 0x6000 ->
      // TODO: I/O registers
      address >= 0x6000 ->
        mapper.write(address, value)
      else ->
        throw RuntimeException("unhandled cpu memory write at address: $address")
    }
  }

  fun step(): Double {
//    stepCallback?.onStep(
//        cycles, PC, SP, A, X, Y, C, Z, I, D, B, U, V, N, interrupt, stall, null)
    if (stall > 0) {
      stall--
      return 1.0
    }
    val currCycles = cycles
    // execute interrupt
    if (interrupt == Interrupt.NMI) {
      // nmi
      push16(PC)
      stepAddress = 0
      stepPC = 0
      stepMode = 0
      php(stepAddress, stepPC, stepMode)
      PC = read16(0xfffa)
      I = 1
      cycles += 7
    } else if (interrupt == Interrupt.IRQ) {
      // irq
      push16(PC)
      stepAddress = 0
      stepPC = 0
      stepMode = 0
      php(stepAddress, stepPC, stepMode)
      PC = read16(0xfffe)
      I = 1
      cycles += 7
    }
    interrupt = Interrupt.NONE
    val opcode = read(PC)
    val mode = instructionModes[opcode]
    val addressingMode = addressingModes[mode]
    val address = when (addressingMode) {
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
      else -> throw RuntimeException("Invalid addressing mode $addressingMode")
    }
    val pageCrossed = when (addressingMode) {
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
      else -> throw RuntimeException("Invalid addressing mode $addressingMode")
    }
    PC += instructionSizes[opcode]
    cycles += instructionCycles[opcode]
    if (pageCrossed) {
      cycles += instructionPageCycles[opcode]
    }
    stepAddress = address
    stepPC = PC
    stepMode = mode
    when (opcode) {
      0 -> {
        // brk
        push16(PC)
        php(stepAddress, stepPC, stepMode)
        sei(stepAddress, stepPC, stepMode)
        PC = read16(0xFFFE)
      }
      1, 5, 9, 13, 17, 21, 25, 29 -> {
        // ora
        A = A or (read(stepAddress) and 0xFF)
        setZN(A)
      }
      2, 18, 34, 50, 66, 82, 98, 114, 146, 178, 210, 242 -> {
        // kil
      }
      3, 7, 15, 19, 23, 27, 31 -> {
        // slo
      }
      4, 12, 20, 26, 28, 52, 58, 60, 68, 84, 90, 92, 100, 116, 122, 124, 128, 130, 137, 194, 212,
      218, 220, 226, 234, 244, 250, 252 -> {
        // nop
      }
      6, 10, 14, 22, 30 -> {
        // asl
        if (stepMode == AddressingMode.MODE_ACCUMULATOR) {
          C = (A shr 7) and 1
          A = A shl 1 and 0xFF
          setZN(A)
        } else {
          var value = read(stepAddress)
          C = (value shr 7) and 1
          value = value shl 1 and 0xFF
          write(stepAddress, value)
          setZN(value)
        }
      }
      8 -> php(stepAddress, stepPC, stepMode)
      11, 43 -> {
        // anc
      }
      16 -> {
        // bpl
        if (N == 0) {
          PC = stepAddress
          addBranchCycles(stepAddress, stepPC, stepMode)
        }
      }
      24 -> {
        // clc
        C = 0
      }
      32 -> {
        // jsr
        push16(PC - 1)
        PC = stepAddress
      }
      33, 37, 41, 45, 49, 53, 57, 61 -> {
        // and
        A = A and read(stepAddress)
        setZN(A)
      }
      35, 39, 47, 51, 55, 59, 63 -> {
        // rla
      }
      36, 44 -> {
        // bit
        val value = read(stepAddress)
        V = (value shr 6) and 1
        setZFlag(value and A)
        setNFlag(value)
      }
      38, 42, 46, 54, 62 -> {
        // rol
        if (stepMode == AddressingMode.MODE_ACCUMULATOR) {
          val c = C
          C = (A shr 7) and 1
          A = (A shl 1) or c and 0xFF
          setZN(A)
        } else {
          val c = C
          var value = read(stepAddress)
          C = (value shr 7) and 1
          value = (value shl 1) or c and 0xFF
          write(stepAddress, value)
          setZN(value and 0xFF)
        }
      }
      40 -> {
        // plp
        setFlags(pull() and 0xEF or 0x20)
      }
      48 -> {
        // bmi
        if (N != 0) {
          PC = stepAddress
          addBranchCycles(stepAddress, stepPC, stepMode)
        }
      }
      56 -> {
        // sec
        C = 1
      }
      64 -> {
        // rti
        setFlags(pull() and 0xEF or 0x20)
        PC = pull16()
      }
      65, 69, 73, 77, 81, 85, 89, 93 -> {
        // eor
        A = A xor read(stepAddress)
        setZN(A)
      }
      67, 71, 79, 83, 87, 91, 95 -> {
        // sre
      }
      70, 74, 78, 86, 94 -> {
        // lsr
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
      72 -> {
        // pha
        push(A)
      }
      75 -> {
        // alr
      }
      76, 108 -> {
        // jmp
        PC = stepAddress
      }
      80 -> {
        // bvc
        if (V == 0) {
          PC = stepAddress
          addBranchCycles(stepAddress, stepPC, stepMode)
        }
      }
      88 -> {
        // cli
        I = 0
      }
      96 -> {
        // rts
        PC = pull16() + 1
      }
      97, 101, 105, 109, 113, 117, 121, 125 -> {
        // adc
        val a = A
        val b = read(stepAddress)
        val c = C
        A = (a + b + c) and 0xFF
        setZN(A)
        C = if (a + b + c > 0xFF) 1 else 0
        V = if ((a xor b) and 0x80 == 0 && (a xor A) and 0x80 != 0) 1 else 0
      }
      99, 103, 111, 115, 119, 123, 127 -> {
        // rra
      }
      102, 106, 110, 118, 126 -> {
        // ror
        if (stepMode == AddressingMode.MODE_ACCUMULATOR) {
          val c = C
          C = A and 1
          A = (A shr 1) or (c shl 7) and 0xFF
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
      104 -> {
        // pla
        A = pull()
        setZN(A)
      }
      107 -> {
        // arr
      }
      112 -> {
        // bvs
        if (V != 0) {
          PC = stepAddress
          addBranchCycles(stepAddress, stepPC, stepMode)
        }
      }
      120 -> {
        sei(stepAddress, stepPC, stepMode)
      }
      129, 133, 141, 145, 149, 153, 157 -> {
        // sta
        write(stepAddress, A)
      }
      131, 135, 143, 151 -> {
        // sax
      }
      132, 140, 148 -> {
        // sty
        write(stepAddress, Y)
      }
      134, 142, 150 -> {
        // stx
        write(stepAddress, X)
      }
      136 -> {
        // dey
        Y = (Y - 1) and 0xFF
        setZN(Y)
      }
      138 -> {
        // txa
        A = X
        setZN(A)
      }
      139 -> {
        // xaa
      }
      144 -> {
        // bcc
        if (C == 0) {
          PC = stepAddress
          addBranchCycles(stepAddress, stepPC, stepMode)
        }
      }
      147, 159 -> {
        // ahx
      }
      152 -> {
        // tya
        A = Y
        setZN(A)
      }
      154 -> {
        // txs
        SP = X
      }
      155 -> {
        // tas
      }
      156 -> {
        // shy
      }
      158 -> {
        // shx
      }
      160, 164, 172, 180, 188 -> {
        // ldy
        Y = read(stepAddress) and 0xFF
        setZN(Y)
      }
      161, 165, 169, 173, 177, 181, 185, 189 -> {
        // lda
        A = read(stepAddress) and 0xFF
        setZN(A)
      }
      162, 166, 174, 182, 190 -> {
        // ldx
        X = read(stepAddress) and 0xFF
        setZN(X)
      }
      163, 167, 171, 175, 179, 183, 191 -> {
        // lax
      }
      168 -> {
        // tay
        Y = A
        setZN(Y)
      }
      170 -> {
        // tax
        X = A
        setZN(X)
      }
      176 -> {
        // bcs
        if (C != 0) {
          PC = stepAddress
          addBranchCycles(stepAddress, stepPC, stepMode)
        }
      }
      184 -> {
        // clv
        V = 0
      }
      186 -> {
        // tsx
        X = SP
        setZN(X)
      }
      187 -> {
        // las
      }
      192, 196, 204 -> {
        // cpy
        val value = read(stepAddress) and 0xFF
        compare(Y, value)
      }
      193, 197, 201, 205, 209, 213, 217, 221 -> {
        // cmp
        val value = read(stepAddress) and 0xFF
        compare(A, value)
      }
      195, 199, 207, 211, 215, 219, 223 -> {
        // dcp
      }
      198, 206, 214, 222 -> {
        // dec
        val value = read(stepAddress) - 1 and 0xFF
        write(stepAddress, value)
        setZN(value)
      }
      200 -> {
        // iny
        Y = (Y + 1) and 0xFF
        setZN(Y)
      }
      202 -> {
        // dex
        X = (X - 1) and 0xFF
        setZN(X)
      }
      203 -> {
        // axs
      }
      208 -> {
        // bne
        if (Z == 0) {
          PC = stepAddress
          addBranchCycles(stepAddress, stepPC, stepMode)
        }
      }
      216 -> {
        // cld
        D = 0
      }
      224, 228, 236 -> {
        // cpx
        compare(X, read(stepAddress) and 0xFF)
      }
      225, 229, 233, 235, 237, 241, 245, 249, 253 -> {
        // sbc
        val a = A
        val b = read(stepAddress)
        val c = C
        A = (a - b - ((1 - c) and 0xFF)) and 0xFF
        setZN(A)
        C = if (a - b - ((1 - c) and 0xFF) >= 0) 1 else 0
        V = if ((a xor b) and 0x80 != 0 && (a xor A) and 0x80 != 0) 1 else 0
      }
      227, 231, 239, 243, 247, 251, 255 -> {
        // isc
      }
      230, 238, 246, 254 -> {
        // inc
        val value = (read(stepAddress) + 1) and 0xFF
        write(stepAddress, value)
        setZN(value)
      }
      232 -> {
        // inx
        X = (X + 1) and 0xFF
        setZN(X)
      }
      240 -> {
        // beq
        if (Z != 0) {
          PC = stepAddress
          addBranchCycles(stepAddress, stepPC, stepMode)
        }
      }
      248 -> {
        // sed
        D = 1
      }
    }
    return cycles - currCycles
  }

  fun dumpState(): String {
    return StatePersistence.dumpState(
        ram, cycles, PC, SP, A, X, Y, C, Z, I, D, B, U, V, N, interrupt, stall
    ).also { println("CPU state saved") }
  }

  fun restoreState(serializedState: String) {
    val state = StatePersistence.restoreState(serializedState)
    ram = state.next()
    cycles = state.next()
    PC = state.next()
    SP = state.next()
    A = state.next()
    X = state.next()
    Y = state.next()
    C = state.next()
    Z = state.next()
    I = state.next()
    D = state.next()
    B = state.next()
    U = state.next()
    V = state.next()
    N = state.next()
    interrupt = state.next()
    stall = state.next()
    println("CPU state restored")
  }

  private inline fun pagesDiffer(a: Int, b: Int) =
    a and 0xFF00 != b and 0xFF00

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
    push(value shr 8)
    push(value and 0xFF)
  }

  /** pull pops a byte from the stack. Returns Byte */
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

  private inline fun setZN(value: Int) {
    setZFlag(value)
    setNFlag(value)
  }

  private inline fun setZFlag(value: Int) {
    Z = if (value == 0) 1 else 0
  }

  // setN sets the negative flag if the argument is negative (high bit is set)
  private inline fun setNFlag(value: Int) {
    N = if (value and 0x80 != 0) 1 else 0
  }

  private fun compare(a: Int, b: Int) {
    setZN(a - b)
    C = if (a >= b) 1 else 0
  }

  private inline fun setFlags(flags: Int) {
    C = (flags shr 0) and 1
    Z = (flags shr 1) and 1
    I = (flags shr 2) and 1
    D = (flags shr 3) and 1
    B = (flags shr 4) and 1
    U = (flags shr 5) and 1
    V = (flags shr 6) and 1
    N = (flags shr 7) and 1
  }

  // PHP - Push Processor Status
  private inline fun php(stepAddress: Int, stepPC: Int, stepMode: Int) {
    push(flags() or 0x10)
  }

  // SEI - Set Interrupt Disable
  private inline fun sei(stepAddress: Int, stepPC: Int, stepMode: Int) {
    I = 1
  }

  companion object {
    const val FREQUENCY_HZ = 1789773 // 1.789773 MHz
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