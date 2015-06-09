package android.emu6502

import android.emu6502.instructions.BaseInstruction
import android.emu6502.instructions.ORA
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

  private val instructionList: HashMap<Int, KMemberFunction0<BaseInstruction, Unit>> = HashMap()

  fun execute() {
    setRandomByte()
    executeNextInstruction()

    if (PC == 0 || !isRunning) {
      stop()
//      message("Program end at PC=$" + addr2hex(regPC - 1))
//      ui.stop()
    }
  }

  private fun stop() {
    isRunning = false
  }

  private fun executeNextInstruction() {
    val instruction = Integer.valueOf(popByte().toInt().toString(), 16)
    val function = instructionList.get(instruction)
    ORA(instructionList).function()
//    else {
//      instructions.ierr()
//    }
  }

  private fun popByte(): Byte {
    return memory.get((PC++).and(0xff));
  }

  private fun setRandomByte() {
    memory.set(0xfe, Math.floor(Math.random() * 256).toInt())
  }
}
