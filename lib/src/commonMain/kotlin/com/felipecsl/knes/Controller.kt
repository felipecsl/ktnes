package com.felipecsl.knes

class Controller {
  private val buttons = BooleanArray(8)
  internal var index: Int = 0  // Byte
  private var strobe: Int = 0 // Byte

  fun onButtonUp(button: Buttons) {
    buttons[button.ordinal] = false
  }

  fun onButtonDown(button: Buttons) {
    buttons[button.ordinal] = true
  }

  internal fun read(): Int {
    var value = 0
    if (index < 8 && buttons[index]) {
      value = 1
    }
    index = (index + 1) and 0xFF
    if (strobe and 1 == 1) {
      index = 0
    }
    return value
  }

  internal fun write(value: Int /* Byte */) {
    strobe = value
    if (strobe and 1 == 1) {
      index = 0
    }
  }
}