package com.felipecsl.knes

class Bitmap constructor(val width: Int, val height: Int) {
  val pixels = IntArray(width * height)

  fun setPixel(x: Int, y: Int, color: Int) {
    pixels[y * width + x] = color
  }
}