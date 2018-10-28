package com.felipecsl.knes.components

import com.felipecsl.knes.Director
import com.felipecsl.knes.FrameTimer
import com.felipecsl.knes.KeyboardController
import kotlinx.html.InputType
import kotlinx.html.js.onClickFunction
import org.khronos.webgl.*
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.files.FileReader
import react.*
import react.dom.*
import kotlin.browser.document
import kotlin.browser.window

const val FPS = 60
const val SECS_PER_FRAME = 1F / FPS
const val CANVAS_SIZE_SCALE = 2

class RootComponent : RComponent<RProps, RootComponent.State>() {
  private val canvasWidth = SCREEN_WIDTH * CANVAS_SIZE_SCALE
  private val canvasHeight = SCREEN_HEIGHT * CANVAS_SIZE_SCALE

  override fun RBuilder.render() {
    val outerState = state
    div("container") {
      div("row") {
        div("col s12 m6 offset-m3") {
          h1("header") { +"ktnes" }
          div("card") {
            div("card-content") {
              div("row") {
                div("input-field col s12") {
                  input(type = InputType.file, name = "rom_file") {
                    ref {
                      outerState.romFileInput = it
                    }
                  }
                }
              }
              div("row") {
                div("col s12") {
                  canvas {
                    attrs {
                      width = canvasWidth.toString()
                      height = canvasHeight.toString()
                    }
                    ref {
                      outerState.canvas = it
                    }
                  }
                }
              }
            }
            div("card-action") {
              button(classes = "btn waves-effect waves-light") {
                if (!state.isRunning) +"Play" else +"Pause"
                attrs {
                  onClickFunction = ::playOrPause
                }
                ref {
                  outerState.playPauseBtn = it
                }
              }
            }
          }
        }
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
      FrameTimer(::onVideoFrame).also { ft ->
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

  private fun onVideoFrame() {
    val doubleScale = CANVAS_SIZE_SCALE * CANVAS_SIZE_SCALE
    val width = canvasWidth.toInt()
    state.director.videoBuffer().forEachIndexed { i, c ->
      // final the (x, y) pair for the `i` pixel in the original video buffer and scale it to the
      // new canvas size
      val x = (i % SCREEN_WIDTH.toInt()) * CANVAS_SIZE_SCALE
      val y = (i / SCREEN_WIDTH.toInt()) * CANVAS_SIZE_SCALE
      // swap blue and red colors
      val color = ALPHA_MASK or (c and 0xFF shl 16) or (c and 0x00FF00) or (c ushr 16)
      // map pixel from original buffer into new buffer
      for (j in 0..doubleScale) {
        val (x1, y1) = x to y + j
        val (x2, y2) = x + 1 to y + j
        state.buffer32[y1 * width + x1] = color
        state.buffer32[y2 * width + x2] = color
      }
    }
    state.imageData.data.set(state.buffer8)
    state.context.putImageData(state.imageData, 0.0, 0.0)
    state.director.setButtons1(state.keyboardController.buttons)
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
    initKeyboard()
  }

  override fun componentDidUpdate(prevProps: RProps, prevState: State, snapshot: Any) {
    initCanvas()
    initKeyboard()
  }

  private fun initKeyboard() {
    state.keyboardController = KeyboardController().apply {
      window.document.addEventListener("keydown", ::handleKeyDown)
      window.document.addEventListener("keyup", ::handleKeyUp)
      window.document.addEventListener("keypress", ::handleKeyPress)
    }
  }

  private fun initCanvas() {
    state.context = state.canvas.getContext("2d") as CanvasRenderingContext2D
    state.context.fillStyle = "black"
    state.context.fillRect(0.0, 0.0, canvasWidth, canvasHeight)
    state.imageData = state.context.getImageData(0.0, 0.0, canvasWidth, canvasHeight)
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
    lateinit var keyboardController: KeyboardController
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