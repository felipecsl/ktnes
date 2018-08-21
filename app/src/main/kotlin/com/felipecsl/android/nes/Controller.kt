package com.felipecsl.android.nes

class Controller {
  var buttons: Array<Boolean> = Array(8) { false }
  var index: Int = 0
  var strobe: Int = 0

  fun read(): Int {
    var value = 0
    if (index < 8 && buttons[index]) {
      value = 1
    }
    index++
    if (strobe and 1 == 1) {
      index = 0
    }
    return value
  }

  fun write(value: Int) {
  }
}