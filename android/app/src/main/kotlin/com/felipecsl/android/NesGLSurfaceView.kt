package com.felipecsl.android

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import com.felipecsl.knes.Bitmap

class NesGLSurfaceView(
    context: Context,
    attributeSet: AttributeSet
) : GLSurfaceView(context, attributeSet) {
  private lateinit var renderer: NesGLRenderer
  private var bitmap: Bitmap? = null
  private val mainHandler = Handler(Looper.getMainLooper())
  private val renderRunnable = Runnable {
    renderer.setBitmap(bitmap!!)
    requestRender()
  }

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
    bitmap = image
    mainHandler.post(renderRunnable)
  }
}