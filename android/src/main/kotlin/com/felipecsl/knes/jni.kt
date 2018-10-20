package com.felipecsl.knes

external fun nativeStartConsole(cartridgeData: ByteArray)
external fun nativeGetConsoleBuffer(): IntArray?

external fun startAudioEngine(cpuIds: IntArray)
external fun stopAudioEngine()
external fun pauseAudioEngine()
external fun resumeAudioEngine()