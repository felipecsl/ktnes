package com.felipecsl.knes

interface CPUStepCallback {
  fun onStep(cycles: Long, PC: Int, SP: Int, A: Int, X: Int, Y: Int, C: Int, Z: Int, I: Int,
      D: Int, B: Int, U: Int, V: Int, N: Int, interrupt: Int, stall: Int, lastOpcode: String?)
}