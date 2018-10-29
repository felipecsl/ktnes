package com.felipecsl.knes.components

import com.felipecsl.knes.Controller
import com.felipecsl.knes.Director
import com.felipecsl.knes.FrameTimer
import com.felipecsl.knes.KeyboardController
import kotlinx.html.*
import kotlinx.html.js.onChangeFunction
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
        div("col s12 m6") {
          h1("header logo") { +"Ktnes" }
        }
      }
      div("row") {
        div("col s12 m6") {
          input(type = InputType.file, name = "rom_file", classes = "inputfile") {
            ref {
              @Suppress("UnsafeCastFromDynamic")
              outerState.romFileInput = it
            }
            attrs {
              onChangeFunction = ::onFileInputChange
              id = "rom_file"
            }
          }
          label("rom_file", classes = "btn") {
            ref {
              @Suppress("UnsafeCastFromDynamic")
              outerState.romFileLabel = it
            }
            i("material-icons left") {
              +"attachment"
            }
            +"Choose a ROM file..."
          }
        }
      }
      div("row") {
        div("col s12 m12 l6") {
          div("card") {
            div("card-content") {
              div("row nomargin") {
                div("col s12 nopadding") {
                  canvas {
                    attrs {
                      width = canvasWidth.toString()
                      height = canvasHeight.toString()
                    }
                    ref {
                      @Suppress("UnsafeCastFromDynamic")
                      outerState.canvas = it
                    }
                  }
                }
              }
            }
            div("card-action") {
              button(classes = "btn waves-effect waves-light") {
                i("material-icons left") {
                  +"play_arrow"
                }
                if (!state.isRunning) +"Play" else +"Pause"
                attrs {
                  onClickFunction = ::playOrPause
                }
                ref {
                  @Suppress("UnsafeCastFromDynamic")
                  outerState.playPauseBtn = it
                }
              }
            }
          }
        }
        div("col s12 m12 l6") {
          h2("header") { +"What's this?" }
          p {
            +"Ktnes is a cross platform NES emulator written in Kotlin."
          }
          p {
            +"Its main goal is to showcase Kotlin's multiplatform features while being a fun and challenging side project."
          }
          p {
            +"It's currently under active development. You can check out the Android app by getting the APK "
            a("#") { +"here." }
          }
        }
        div("col s12 m12 l6") {
          h2("header") { +"How it works?" }
          p {
            +"TODO"
          }
        }
      }
    }
  }

  private fun romFile() = state.romFileInput.files?.asList()?.firstOrNull()

  private fun onFileInputChange(@Suppress("UNUSED_PARAMETER") e: Event) {
    val romFile = romFile()
    if (romFile != null) {
      state.romFileLabel.innerText = romFile.name
    }
  }

  private fun playOrPause(@Suppress("UNUSED_PARAMETER") event: Event) {
    // load ROM file
    val romFile = romFile()
    if (romFile != null) {
      val reader = FileReader()
      reader.onload = ::onRomFileLoaded
      reader.readAsArrayBuffer(romFile)
    } else {
      window.alert("No ROM file selected.")
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
      initKeyboard(it.controller1)
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

  private fun initKeyboard(controller: Controller) {
    state.keyboardController = KeyboardController(controller).apply {
      document.addEventListener("keydown", ::handleKeyDown)
      document.addEventListener("keyup", ::handleKeyUp)
      document.addEventListener("keypress", ::handleKeyPress)
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
    lateinit var romFileLabel: HTMLLabelElement
    var frameTimer: FrameTimer? = null
    var isRunning = false
  }

  companion object {
    private const val ALPHA_MASK = 0xFF000000.toInt()
    private const val SCREEN_WIDTH = 256.0
    private const val SCREEN_HEIGHT = 240.0
  }
}

inline fun RBuilder.label(
    htmlFor: String? = null,
    classes: String? = null,
    block: RDOMBuilder<LABEL>.() -> Unit
): ReactElement =
    tag(block) { LABEL(attributesMapOf("htmlFor", htmlFor, "class", classes), it) }