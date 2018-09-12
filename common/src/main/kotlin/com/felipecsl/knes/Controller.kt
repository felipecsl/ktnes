package com.felipecsl. knes

internal class Controller {
  var buttons: BooleanArray? = null
  var index: Int = 0  // Byte
  var strobe: Int = 0 // Byte

  fun read(): Int {
    var value = 0
    if (index < 8 && buttons?.get(index) == true) {
      value = 1
    }
    index++
    if (strobe and 1 == 1) {
      index = 0
    }
    return value
  }

  fun write(value: Int) {
    strobe = value
    if (strobe and 1 == 1) {
      index = 0
    }
  }
}