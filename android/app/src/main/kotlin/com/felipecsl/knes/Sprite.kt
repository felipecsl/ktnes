package com.felipecsl.knes

actual class Sprite(private val delegate: SpriteFacade) : SpriteFacade {
  actual override fun setTexture(texture: Int) {
    delegate.setTexture(texture)
  }

  actual override fun draw() {
    delegate.draw()
  }

  actual override fun setImage(image: Bitmap) {
    delegate.setImage(image)
  }
}