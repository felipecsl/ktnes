package com.felipecsl.knes.web.worker

import com.felipecsl.knes.Director
import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker
import org.w3c.dom.events.Event
import kotlin.js.json

external val self: Worker

var director: Director? = null

/** main() is invoked when the worker is instantiated from another script */
fun main(args: Array<String>) {
  self.onmessage = ::onMessage
}

fun onMessage(event: Event): dynamic {
  val messageEvent = event as MessageEvent
  val data: dynamic = messageEvent.data
  val message = data.message
  println("[web-worker] onMessage=$message")
  when (message) {
    "start" -> startConsole(data.buffer as ByteArray)
    "frame" -> self.postMessage(json("frame" to director!!.buffer()))
    else -> println("[web-worker] Unknown message data received $data")
  }
  return null
}

private fun startConsole(buffer: ByteArray) {
  println("[web-worker] Starting console")
  director = Director(buffer).also {
    it.run()
  }
}