package com.felipecsl.knes

import com.felipecsl.knes.components.RootComponent
import react.dom.render
import react.router.dom.hashRouter
import react.router.dom.route
import react.router.dom.switch
import kotlin.browser.document
import kotlin.browser.window

fun main(@Suppress("UnusedMainParameter") args: Array<String>) {
  window.onload = {
    render(document.getElementById("root")!!) {
      hashRouter {
        switch {
          route("/", RootComponent::class, exact = true)
        }
      }
    }
  }
}
