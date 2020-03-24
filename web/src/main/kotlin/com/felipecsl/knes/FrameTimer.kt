package com.felipecsl.knes

import kotlin.browser.window

class FrameTimer(
    private val onNewFrame: () -> Unit
) {
  private var isRunning = false
  private var requestId: Int? = null

  fun start() {
    isRunning = true
    requestAnimationFrame()
  }

  fun stop() {
    isRunning = false
    if (requestId != null) {
      window.cancelAnimationFrame(requestId!!)
    }
  }

  fun running() = isRunning

  private fun requestAnimationFrame() {
    requestId = window.requestAnimationFrame(::onAnimationFrame)
  }

  private fun onAnimationFrame(@Suppress("UNUSED_PARAMETER") unused: Double) {
    if (isRunning) {
      requestAnimationFrame()
      onNewFrame.invoke()
    }
  }
}