package com.felipecsl.knes

import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLES30.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

class GLSprite {
  private var context: RenderContext? = null
  private var texture: Int? = null
  private var buffer: IntBuffer? = null
  var director: Director? = null

  data class RenderContext(
      val shaderProgram: Int = 0,
      val texSamplerHandle: Int = 0,
      val texCoordHandle: Int = 0,
      val posCoordHandle: Int = 0,
      val texVertices: FloatBuffer? = null,
      val posVertices: FloatBuffer? = null
  )

  fun setTexture(texture: Int) {
    this.texture = texture
  }

  fun draw() {
    val image = if (director != null) {
      director!!.buffer()
    } else {
      nativeGetConsoleBuffer()
    }
    if (image != null) {
      createProgramIfNeeded()
      updateTexture(image)
    }
  }

  private fun createProgramIfNeeded() {
    if (context != null) {
      return
    }
    val vertexShader = loadShader(GLES30.GL_VERTEX_SHADER, VERTEX_SHADER)
    if (vertexShader == 0) {
      throw RuntimeException("Failed to create vertex shader")
    }
    val pixelShader = loadShader(GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
    if (pixelShader == 0) {
      throw RuntimeException("Failed to create pixel shader")
    }
    val program = GLES30.glCreateProgram()
    if (program != 0) {
      GLES30.glAttachShader(program, vertexShader)
      GLES30.glAttachShader(program, pixelShader)
      GLES30.glLinkProgram(program)
      val linkStatus = IntArray(1)
      GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
      if (linkStatus[0] != GLES30.GL_TRUE) {
        val info = GLES30.glGetProgramInfoLog(program)
        GLES30.glDeleteProgram(program)
        throw RuntimeException("Could not link program: $info")
      }
    }
    // Bind attributes and uniforms
    context = RenderContext(
        texSamplerHandle = GLES30.glGetUniformLocation(program, "tex_sampler"),
        texCoordHandle = GLES30.glGetAttribLocation(program, "a_texcoord"),
        posCoordHandle = GLES30.glGetAttribLocation(program, "a_position"),
        texVertices = createVerticesBuffer(TEX_VERTICES),
        posVertices = createVerticesBuffer(POS_VERTICES),
        shaderProgram = program
    )
  }

  private fun createTexture(image: Bitmap) {
    buffer = IntBuffer.wrap(image.pixels)
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture!!)
    GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES11Ext.GL_BGRA, image.width, image.height, 0,
        GLES11Ext.GL_BGRA, GLES30.GL_UNSIGNED_BYTE, buffer)
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST)
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST)
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
    GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
    // Use our shader program
    val context = context!!
    GLES30.glUseProgram(context.shaderProgram)
    // Disable blending
    GLES30.glDisable(GLES30.GL_BLEND)
    // Set the vertex attributes
    GLES30.glVertexAttribPointer(context.texCoordHandle, 2, GLES30.GL_FLOAT, false, 0,
        context.texVertices)
    GLES30.glEnableVertexAttribArray(context.texCoordHandle)
    GLES30.glVertexAttribPointer(context.posCoordHandle, 2, GLES30.GL_FLOAT, false, 0,
        context.posVertices)
    GLES30.glEnableVertexAttribArray(context.posCoordHandle)
  }

  private fun updateTexture(image: Bitmap) {
    glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
    if (buffer == null) {
      createTexture(image)
    } else {
      buffer = IntBuffer.wrap(image.pixels)
    }
    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture!!)
    GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, image.width, image.height,
        GLES11Ext.GL_BGRA, GLES30.GL_UNSIGNED_BYTE, buffer)
    // Set the input texture
//    GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
//    GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture!!)
//    GLES30.glUniform1i(context!!.texSamplerHandle, 0)
    // Draw!
    GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
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
    val shader = GLES30.glCreateShader(shaderType)
    if (shader != 0) {
      GLES30.glShaderSource(shader, source)
      GLES30.glCompileShader(shader)
      val compiled = IntArray(1)
      GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compiled, 0)
      if (compiled[0] == 0) {
        val info = GLES30.glGetShaderInfoLog(shader)
        GLES30.glDeleteShader(shader)
        throw RuntimeException("Could not compile shader $shaderType:$info")
      }
    }
    return shader
  }

  companion object {
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