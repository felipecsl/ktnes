package com.felipecsl.knes

import org.khronos.webgl.Float32Array

external class AudioContext {
  val destination: AudioNode
  fun createScriptProcessor(
      bufferSize: Int,
      numberOfInputChannels: Int,
      numberOfOutputChannels: Int
  ): ScriptProcessorNode
}

open external class AudioNode {
  fun connect(destination: AudioNode, output: Int = definedExternally,
      input: Int = definedExternally): AudioNode
}

external class AudioBuffer {
  val sampleRate: Float
  val length: Int
  val duration: Double
  val numberOfChannels: Int

  fun getChannelData(channel: Int): Float32Array
}

external class AudioProcessingEvent {
  val outputBuffer: AudioBuffer
}

external class ScriptProcessorNode {
  var onaudioprocess : (AudioProcessingEvent) -> Unit
  fun connect(node: AudioNode)
}

external class OscillatorNode : AudioNode {
  fun start(time: Double = definedExternally)
}