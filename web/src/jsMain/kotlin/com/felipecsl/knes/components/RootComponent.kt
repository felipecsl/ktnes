package com.felipecsl.knes.components

import react.RBuilder
import react.RComponent
import react.RProps
import react.RState
import react.dom.h1

class RootComponent : RComponent<RProps, RState>()  {
  override fun RBuilder.render() {
    h1 { +"ktnes" }
  }
}