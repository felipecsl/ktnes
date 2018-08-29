package com.felipecsl.knes

actual fun currentTimeMs(): Long {
  return System.currentTimeMillis()
}

actual class Bitmap actual constructor(width: Int, height: Int) {
  val delegate = android.graphics.Bitmap.createBitmap(width, height,
      android.graphics.Bitmap.Config.RGB_565)

  actual fun setPixel(x: Int, y: Int, color: Int) {
    delegate.setPixel(x, y, color)
  }
}