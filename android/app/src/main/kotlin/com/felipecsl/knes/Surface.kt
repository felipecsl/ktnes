package com.felipecsl.knes

import com.felipecsl.android.NesGLSurfaceView

actual class Surface(private val surfaceView: NesGLSurfaceView) {
  actual fun setTexture(bitmap: Bitmap) {
    surfaceView.setTexture(bitmap)
  }
}