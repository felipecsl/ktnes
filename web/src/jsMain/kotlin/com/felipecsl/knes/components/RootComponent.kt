package com.felipecsl.knes.components

import com.felipecsl.knes.FrameTimer
import kotlinx.html.InputType
import kotlinx.html.header
import kotlinx.html.js.onClickFunction
import org.khronos.webgl.*
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
      attrs {
        width = SCREEN_WIDTH.toString()
        height = SCREEN_HEIGHT.toString()
      }
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
      FrameTimer(::requestNewFrame).also { ft ->
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
    val message = data.message
    when (message) {
      "frame" -> onFrameReceived(data.buffer as IntArray)
      else -> println("[web] Unknown message received $message")
    }
    return null
  }

  private fun onFrameReceived(buffer: IntArray) {
    for (i in 0..buffer.size) {
      val inColor = buffer[i]
      val red = (inColor shr 16) and 0xFF
      val green = (inColor shr 8) and 0xFF
      val blue = (inColor shr 0) and 0xFF
      val outColor = (blue shl 16) or (green shl 8) or (red shl 0)
      state.buffer32!![i] = 0xff000000.toInt() or outColor
    }
    state.imageData!!.data.set(state.buffer8!!)
    state.context!!.putImageData(state.imageData!!, 0.0, 0.0)
  }

  private fun Uint8Array.toByteArray(): ByteArray {
    return ByteArray(length).also { byteArray ->
      (0..length).forEach {
        byteArray[it] = this[it]
      }
    }
  }

  private fun requestNewFrame() {
    state.worker!!.postMessage(json("message" to "frame"))
  }

  override fun componentDidMount() {
    initCanvas()
  }

  override fun componentDidUpdate(prevProps: RProps, prevState: State, snapshot: Any) {
    initCanvas()
  }

  private fun initCanvas() {
    state.context = state.canvas!!.getContext("2d") as CanvasRenderingContext2D
    state.context!!.fillStyle = "black"
    state.context!!.fillRect(0.0, 0.0, SCREEN_WIDTH, SCREEN_HEIGHT)
    state.imageData = state.context!!.getImageData(0.0, 0.0, SCREEN_WIDTH, SCREEN_HEIGHT)
    state.buffer = ArrayBuffer(state.imageData!!.data.length)
    state.buffer8 = Uint8ClampedArray(state.buffer!!)
    state.buffer32 = Uint32Array(state.buffer!!)

    // set alpha
    for (i in 0..state.buffer32!!.length) {
      state.buffer32!![i] = 0xff000000.toInt()
    }
  }

  class State : RState {
    var context: CanvasRenderingContext2D? = null
    var canvas: HTMLCanvasElement? = null
    var playPauseBtn: HTMLButtonElement? = null
    var frameTimer: FrameTimer? = null
    var worker: Worker? = null
    var isRunning = false
    var romFileInput: HTMLInputElement? = null
    var imageData: ImageData? = null
    var buffer: ArrayBuffer? = null
    var buffer8: Uint8ClampedArray? = null
    var buffer32: Uint32Array? = null
  }

  companion object {
    private const val SCREEN_WIDTH = 256.0
    private const val SCREEN_HEIGHT = 240.0
  }
}