package com.felipecsl.knes.components

import org.w3c.dom.CanvasRenderingContext2D
import org.w3c.dom.HTMLCanvasElement
import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.canvas
import react.dom.h1

class RootComponent : RComponent<RProps, RootComponent.State>()  {
  override fun RBuilder.render() {
    val rootComponentState = state
    h1 { +"ktnes" }
    canvas("screen") {
      ref {
        rootComponentState.canvas = it
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
    val context = state.canvas!!.getContext("2d") as CanvasRenderingContext2D
    context.fillStyle = "black"
    context.fillRect(0.0, 0.0, SCREEN_WIDTH.toDouble(), SCREEN_HEIGHT.toDouble())
  }

  class State : RState {
    var canvas: HTMLCanvasElement? = null
  }

  companion object {
    private const val SCREEN_WIDTH = 256
    private const val SCREEN_HEIGHT = 240
  }
}