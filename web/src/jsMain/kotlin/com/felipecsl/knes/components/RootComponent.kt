package com.felipecsl.knes.components

import com.felipecsl.knes.FrameTimer
import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.files.FileReader
import react.*
import react.dom.*
import kotlin.js.json

@JsModule("web-worker")
external class Worker {
  var onmessage: ((Event) -> dynamic)?
  var onerror: ((Event) -> dynamic)?
  fun terminate()
  fun postMessage(message: Any?, transfer: Array<dynamic> = definedExternally)
}

class RootComponent : RComponent<RProps, RootComponent.State>() {
  override fun RBuilder.render() {
    val outerState = state
    h1 { +"ktnes" }
    input(type = InputType.file, name = "rom_file") {
      ref {
        outerState.romFileInput = it
      }
    }
    br {}
    canvas("screen") {
      ref {
        outerState.canvas = it
      }
    }
    br {}
    button {
      if (!state.isRunning) +"Play" else +"Pause"
      attrs {
        onClickFunction = ::playOrPause
      }
      ref {
        outerState.playPauseBtn = it
      }
    }
  }

  private fun playOrPause(@Suppress("UNUSED_PARAMETER") event: Event) {
    // load ROM file
    val romFile = state.romFileInput!!.files?.asList()?.firstOrNull()
    if (romFile != null) {
      val reader = FileReader()
      reader.onload = ::onRomFileLoaded
      reader.readAsArrayBuffer(romFile)
    } else {
      console.log("No ROM file selected.")
    }
  }

  private fun onRomFileLoaded(event: Event) {
    // lazily initialize FrameTimer
    val frameTimer = state.frameTimer ?: let {
      FrameTimer(::onNewFrame).also { ft ->
        state.frameTimer = ft
      }
    }
    if (!frameTimer.running()) {
      startConsole(event, frameTimer)
    } else {
      frameTimer.stop()
    }
    setState {
      isRunning = frameTimer.running()
    }
  }

  private fun startConsole(event: Event, frameTimer: FrameTimer) {
    // load ROM file contents
    val buffer = (event.target!! as FileReader).result as ArrayBuffer
    val cartridgeData = Uint8Array(buffer).toByteArray()
    console.log("ROM file loaded, size=${cartridgeData.size}")
    state.worker = Worker().also {
      // send cartridge data to worker
      it.onmessage = ::onWorkerMessage
      it.postMessage(json("message" to "start", "buffer" to cartridgeData))
    }
    frameTimer.start()
  }

  private fun onWorkerMessage(event: Event): dynamic {
    val messageEvent = event as MessageEvent
    val data: dynamic = messageEvent.data
    println("[web] worker#onmessage: $data")
    return null
  }

  private fun Uint8Array.toByteArray(): ByteArray {
    return ByteArray(length).also { byteArray ->
      (0..length).forEach {
        byteArray[it] = this[it]
      }
    }
  }

  private fun onNewFrame() {
    println("[web] requesting new frame")
    state.worker!!.postMessage(json("message" to "frame"))
  }

  override fun componentDidMount() {
    initCanvas()
  }

  override fun componentDidUpdate(prevProps: RProps, prevState: State, snapshot: Any) {
    initCanvas()
  }

  private fun initCanvas() {
    val context = state.canvas!!.getContext("2d") as CanvasRenderingContext2D
    context.fillStyle = "black"
    context.fillRect(0.0, 0.0, SCREEN_WIDTH.toDouble(), SCREEN_HEIGHT.toDouble())
  }

  class State : RState {
    var canvas: HTMLCanvasElement? = null
    var playPauseBtn: HTMLButtonElement? = null
    var frameTimer: FrameTimer? = null
    var worker: Worker? = null
    var isRunning = false
    var romFileInput: HTMLInputElement? = null
  }

  companion object {
    private const val SCREEN_WIDTH = 256
    private const val SCREEN_HEIGHT = 240
  }
}