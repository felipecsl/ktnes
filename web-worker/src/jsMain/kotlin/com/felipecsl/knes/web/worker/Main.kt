package com.felipecsl.knes.web.worker

import com.felipecsl.knes.Director
import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker
import org.w3c.dom.events.Event
import kotlin.js.json

external val self: Worker

var director: Director? = null
const val FPS = 60
const val SECS_PER_FRAME = 1F / FPS

/** main() is invoked when the worker is instantiated from another script */
fun main(args: Array<String>) {
  self.onmessage = ::onMessage
}

fun onMessage(event: Event): dynamic {
  val messageEvent = event as MessageEvent
  val data: dynamic = messageEvent.data
  val message = data.message
  when (message) {
    "start" -> startConsole(data.buffer as ByteArray)
    "frame" -> onFrameRequested()
    else -> println("[web-worker] Unknown message received $message")
  }
  return null
}

fun onFrameRequested() {
  self.postMessage(json("message" to "frame", "buffer" to director!!.buffer()))
  director!!.stepSeconds(SECS_PER_FRAME)
}

private fun startConsole(buffer: ByteArray) {
  println("[web-worker] Starting console")
  director = Director(buffer).also {
    it.stepSeconds(SECS_PER_FRAME)
  }
}