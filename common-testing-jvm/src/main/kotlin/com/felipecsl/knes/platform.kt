package com.felipecsl.knes

actual fun currentTimeMs(): Long {
  return System.currentTimeMillis()
}

actual class Sprite {
  actual fun draw() {}
  actual fun setTexture(texture: Int) {}
  actual fun setImage(image: Bitmap) {}
}