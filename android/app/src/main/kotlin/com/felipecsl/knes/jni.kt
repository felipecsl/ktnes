package com.felipecsl.knes

external fun nativeStartConsole(cartridgeData: ByteArray)
external fun nativeGetConsoleBuffer(): IntArray?
internal external fun startEngine(cpuIds: IntArray)
external fun stopEngine()