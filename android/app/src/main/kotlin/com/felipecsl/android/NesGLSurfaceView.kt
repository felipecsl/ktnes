package com.felipecsl.android

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.felipecsl.knes.Sprite

class NesGLSurfaceView(
    context: Context,
    attributeSet: AttributeSet
) : GLSurfaceView(context, attributeSet) {
  private lateinit var renderer: NesGLRenderer

  fun setSprite(sprite: Sprite) {
    // Create an OpenGL ES 2.0 context
    setEGLContextClientVersion(2)
    // Set the Renderer for drawing on the GLSurfaceView
    renderer = NesGLRenderer(sprite)
    setRenderer(renderer)
  }
}