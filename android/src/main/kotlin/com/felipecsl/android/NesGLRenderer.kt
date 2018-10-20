package com.felipecsl.android

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.felipecsl.knes.GLSprite
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class NesGLRenderer(private val sprite: GLSprite) : GLSurfaceView.Renderer {
  override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
    // Set the background frame color
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
    val textureHandle = IntArray(1)
    GLES20.glGenTextures(1, textureHandle, 0)
    if (textureHandle[0] != 0) {
      sprite.setTexture(textureHandle[0])
    } else {
      throw RuntimeException("Cannot create GL texture")
    }
  }

  override fun onDrawFrame(unused: GL10) {
    sprite.draw()
  }

  override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
    // Adjust the viewport based on geometry changes,
    // such as screen rotation
    GLES20.glViewport(0, 0, width, height)
  }
}