package com.felipecsl.knes

expect class Bitmap constructor(width: Int, height: Int) {
  fun setPixel(x: Int, y: Int, color: Int)
}
