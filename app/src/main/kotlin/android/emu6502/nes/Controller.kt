package android.emu6502.nes

class Controller {
  var buttons: Array<Boolean> = Array(8) { false }
  var index: Byte = 0
  var strobe: Byte = 0

  fun read() {
  }

  fun write() {
  }
}