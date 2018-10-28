package com.felipecsl.knes.components

import com.felipecsl.knes.Director
import com.felipecsl.knes.FrameTimer
import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.files.FileReader
import react.*
import react.dom.*

const val FPS = 60
const val SECS_PER_FRAME = 1F / FPS

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
    val romFile = state.romFileInput.files?.asList()?.firstOrNull()
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
    state.director = Director(cartridgeData).also {
      it.stepSeconds(SECS_PER_FRAME)
    }
    frameTimer.start()
  }

  private fun requestNewFrame() {
    // convert BGR to ARGB
    state.director.videoBuffer().forEachIndexed { i, c ->
      state.buffer32[i] = ALPHA_MASK or (c and 0xFF shl 16) or (c and 0x00FF00) or (c ushr 16)
    }
    state.imageData.data.set(state.buffer8)
    state.context.putImageData(state.imageData, 0.0, 0.0)
    state.director.stepSeconds(SECS_PER_FRAME)
  }

  private fun Uint8Array.toByteArray(): ByteArray {
    return ByteArray(length).also { byteArray ->
      (0..length).forEach {
        byteArray[it] = this[it]
      }
    }
  }

  override fun componentDidMount() {
    initCanvas()
  }

  override fun componentDidUpdate(prevProps: RProps, prevState: State, snapshot: Any) {
    initCanvas()
  }

  private fun initCanvas() {
    state.context = state.canvas.getContext("2d") as CanvasRenderingContext2D
    state.context.fillStyle = "black"
    state.context.fillRect(0.0, 0.0, SCREEN_WIDTH, SCREEN_HEIGHT)
    state.imageData = state.context.getImageData(0.0, 0.0, SCREEN_WIDTH, SCREEN_HEIGHT)
    state.buffer = ArrayBuffer(state.imageData.data.length)
    state.buffer8 = Uint8ClampedArray(state.buffer)
    state.buffer32 = Uint32Array(state.buffer)
    for (i in 0..state.buffer32.length) {
      state.buffer32[i] = ALPHA_MASK
    }
  }

  class State : RState {
    lateinit var context: CanvasRenderingContext2D
    lateinit var director: Director
    lateinit var canvas: HTMLCanvasElement
    lateinit var playPauseBtn: HTMLButtonElement
    lateinit var romFileInput: HTMLInputElement
    lateinit var imageData: ImageData
    lateinit var buffer: ArrayBuffer
    lateinit var buffer8: Uint8ClampedArray
    lateinit var buffer32: Uint32Array
    var frameTimer: FrameTimer? = null
    var isRunning = false
  }

  companion object {
    private const val ALPHA_MASK = 0xFF000000.toInt()
    private const val SCREEN_WIDTH = 256.0
    private const val SCREEN_HEIGHT = 240.0
  }
}