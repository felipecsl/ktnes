package com.felipecsl.android

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet

class NesGLSurfaceView(
    context: Context,
    attributeSet: AttributeSet
) : GLSurfaceView(context, attributeSet) {
  private val renderer: NesGLRenderer
  private val mainHandler = Handler(Looper.getMainLooper())

  init {
    // Create an OpenGL ES 2.0 context
    setEGLContextClientVersion(2)
    // Set the Renderer for drawing on the GLSurfaceView
    renderer = NesGLRenderer()
    setRenderer(renderer)
    // Render the view only when there is a change in the drawing data
    renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
  }

  fun setTexture(image: Bitmap) {
    mainHandler.post {
      renderer.setBitmap(image)
      requestRender()
    }
  }
}