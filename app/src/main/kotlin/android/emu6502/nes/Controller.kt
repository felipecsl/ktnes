package android.emu6502.nes

import kotlin.experimental.and

class Controller {
  var buttons: Array<Boolean> = Array(8) { false }
  var index: Byte = 0
  var strobe: Byte = 0

  fun read(): Int {
    var value = 0
    if (index < 8 && buttons[index.toInt()]) {
      value = 1
    }
    index++
    if (strobe and 1 == 1.toByte()) {
      index = 0
    }
    return value
  }

  fun write(value: Int) {
  }
}