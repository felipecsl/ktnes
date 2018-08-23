package com.felipecsl.android

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.logging.Logger

class Sprite(private val texture: Int) {
  var image: Bitmap? = null
  private var context: RenderContext? = null

  fun draw() {
    if (image != null) {
      createTexture(image!!)
    }
    renderTexture()
  }

  data class RenderContext(
      val shaderProgram: Int = 0,
      val texSamplerHandle: Int = 0,
      val texCoordHandle: Int = 0,
      val posCoordHandle: Int = 0,
      val texVertices: FloatBuffer? = null,
      val posVertices: FloatBuffer? = null
  )

  private fun renderTexture() {
    val context = createProgram() ?: return
    // Use our shader program
    GLES20.glUseProgram(context.shaderProgram)
    // Disable blending
    GLES20.glDisable(GLES20.GL_BLEND)
    // Set the vertex attributes
    GLES20.glVertexAttribPointer(
        context.texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, context.texVertices)
    GLES20.glEnableVertexAttribArray(context.texCoordHandle)
    GLES20.glVertexAttribPointer(
        context.posCoordHandle, 2, GLES20.GL_FLOAT, false, 0, context.posVertices)
    GLES20.glEnableVertexAttribArray(context.posCoordHandle)
    // Set the input texture
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
    GLES20.glUniform1i(context.texSamplerHandle, 0)
    // Draw!
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
  }

  private fun createProgram(): RenderContext? {
    if (context != null) {
      return context
    }
    val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
    if (vertexShader == 0) {
      return null
    }
    val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
    if (pixelShader == 0) {
      return null
    }
    val program = GLES20.glCreateProgram()
    if (program != 0) {
      GLES20.glAttachShader(program, vertexShader)
      GLES20.glAttachShader(program, pixelShader)
      GLES20.glLinkProgram(program)
      val linkStatus = IntArray(1)
      GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
      if (linkStatus[0] != GLES20.GL_TRUE) {
        val info = GLES20.glGetProgramInfoLog(program)
        GLES20.glDeleteProgram(program)
        throw RuntimeException("Could not link program: $info")
      }
    }
    // Bind attributes and uniforms
    context = RenderContext(
        texSamplerHandle = GLES20.glGetUniformLocation(program, "tex_sampler"),
        texCoordHandle = GLES20.glGetAttribLocation(program, "a_texcoord"),
        posCoordHandle = GLES20.glGetAttribLocation(program, "a_position"),
        texVertices = createVerticesBuffer(TEX_VERTICES),
        posVertices = createVerticesBuffer(POS_VERTICES),
        shaderProgram = program
    )
    return context
  }

  private fun createTexture(bitmap: Bitmap) {
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
    GLES20.glTexParameteri(
        GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
    GLES20.glTexParameteri(
        GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
    GLES20.glTexParameteri(
        GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(
        GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
  }

  private fun createVerticesBuffer(vertices: FloatArray): FloatBuffer {
    if (vertices.size != 8) {
      throw RuntimeException("Number of vertices should be four.")
    }
    val buffer = ByteBuffer.allocateDirect(vertices.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
    buffer.put(vertices).position(0)
    return buffer
  }

  private fun loadShader(shaderType: Int, source: String): Int {
    val shader = GLES20.glCreateShader(shaderType)
    if (shader != 0) {
      GLES20.glShaderSource(shader, source)
      GLES20.glCompileShader(shader)
      val compiled = IntArray(1)
      GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
      if (compiled[0] == 0) {
        val info = GLES20.glGetShaderInfoLog(shader)
        GLES20.glDeleteShader(shader)
        throw RuntimeException("Could not compile shader $shaderType:$info")
      }
    }
    return shader
  }

  companion object {
    private val LOG = Logger.getLogger("Sprite")
    private const val VERTEX_SHADER =
        "attribute vec4 a_position;\n" +
            "attribute vec2 a_texcoord;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {\n" +
            "  gl_Position = a_position;\n" +
            "  v_texcoord = a_texcoord;\n" +
            "}\n"
    private const val FRAGMENT_SHADER =
        "precision mediump float;\n" +
            "uniform sampler2D tex_sampler;\n" +
            "varying vec2 v_texcoord;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(tex_sampler, v_texcoord);\n" +
            "}\n"
    private val TEX_VERTICES = floatArrayOf(0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f)
    private val POS_VERTICES = floatArrayOf(-1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f)
  }
}