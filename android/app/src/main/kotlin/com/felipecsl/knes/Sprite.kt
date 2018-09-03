package com.felipecsl.knes

import android.opengl.GLES11Ext.GL_BGRA
import android.opengl.GLES20.*
import com.felipecsl.android.NesGLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.logging.Logger

actual class Sprite(private val nesGLSurfaceView: NesGLSurfaceView) {
  private var image: Bitmap? = null
  private var context: RenderContext? = null
  private var isDirty = false
  private var texture: Int? = null
  private var buffer: IntBuffer? = null

  data class RenderContext(
      val shaderProgram: Int = 0,
      val texSamplerHandle: Int = 0,
      val texCoordHandle: Int = 0,
      val posCoordHandle: Int = 0,
      val texVertices: FloatBuffer? = null,
      val posVertices: FloatBuffer? = null
  )

  actual fun setTexture(texture: Int) {
    this.texture = texture
  }

  actual fun draw() {
    createProgramIfNeeded()
    if (isDirty) {
      updateTexture(image!!)
      isDirty = false
    }
    renderTexture()
  }

  actual fun setImage(image: Bitmap) {
    if (image !== this.image) {
      this.image = image
      isDirty = true
    }
    nesGLSurfaceView.requestRender()
  }

  private fun renderTexture() {
    // Set the input texture
    glActiveTexture(GL_TEXTURE0)
    glBindTexture(GL_TEXTURE_2D, texture!!)
    glUniform1i(context!!.texSamplerHandle, 0)
    // Draw!
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
  }

  private fun createProgramIfNeeded() {
    if (context != null) {
      return
    }
    val vertexShader = loadShader(GL_VERTEX_SHADER, VERTEX_SHADER)
    if (vertexShader == 0) {
      throw RuntimeException("Failed to create vertex shader")
    }
    val pixelShader = loadShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
    if (pixelShader == 0) {
      throw RuntimeException("Failed to create pixel shader")
    }
    val program = glCreateProgram()
    if (program != 0) {
      glAttachShader(program, vertexShader)
      glAttachShader(program, pixelShader)
      glLinkProgram(program)
      val linkStatus = IntArray(1)
      glGetProgramiv(program, GL_LINK_STATUS, linkStatus, 0)
      if (linkStatus[0] != GL_TRUE) {
        val info = glGetProgramInfoLog(program)
        glDeleteProgram(program)
        throw RuntimeException("Could not link program: $info")
      }
    }
    // Bind attributes and uniforms
    context = RenderContext(
        texSamplerHandle = glGetUniformLocation(program, "tex_sampler"),
        texCoordHandle = glGetAttribLocation(program, "a_texcoord"),
        posCoordHandle = glGetAttribLocation(program, "a_position"),
        texVertices = createVerticesBuffer(TEX_VERTICES),
        posVertices = createVerticesBuffer(POS_VERTICES),
        shaderProgram = program
    )
  }

  private fun createTexture(width: Int, height: Int) {
    glBindTexture(GL_TEXTURE_2D, texture!!)
    glTexImage2D(GL_TEXTURE_2D, 0, GL_BGRA, width, height, 0, GL_BGRA,
        GL_UNSIGNED_BYTE, buffer)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    // Use our shader program
    val context = context!!
    glUseProgram(context.shaderProgram)
    // Disable blending
    glDisable(GL_BLEND)
    // Set the vertex attributes
    glVertexAttribPointer(context.texCoordHandle, 2, GL_FLOAT, false, 0, context.texVertices)
    glEnableVertexAttribArray(context.texCoordHandle)
    glVertexAttribPointer(context.posCoordHandle, 2, GL_FLOAT, false, 0, context.posVertices)
    glEnableVertexAttribArray(context.posCoordHandle)
  }

  private fun updateTexture(bitmap: Bitmap) {
    buffer = IntBuffer.wrap(bitmap.pixels)
    createTexture(bitmap.width, bitmap.height)
    glBindTexture(GL_TEXTURE_2D, texture!!)
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, bitmap.width, bitmap.height, GL_BGRA, GL_UNSIGNED_BYTE,
        buffer)
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
    val shader = glCreateShader(shaderType)
    if (shader != 0) {
      glShaderSource(shader, source)
      glCompileShader(shader)
      val compiled = IntArray(1)
      glGetShaderiv(shader, GL_COMPILE_STATUS, compiled, 0)
      if (compiled[0] == 0) {
        val info = glGetShaderInfoLog(shader)
        glDeleteShader(shader)
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