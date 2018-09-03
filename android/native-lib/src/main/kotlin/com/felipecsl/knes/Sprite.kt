package com.felipecsl.knes

import cnames.structs.ANativeWindow
import platform.android.*
import kotlinx.cinterop.*
import platform.egl.*
import platform.gles2.*

actual class Sprite {
  private var context: RenderContext? = null
  private var image: Bitmap? = null
  private var isDirty = false
  private var texture: Int? = null

  data class RenderContext(
      val shaderProgram: Int = 0,
      val texSamplerHandle: Int = 0,
      val texCoordHandle: Int = 0,
      val posCoordHandle: Int = 0,
      val texVertices: CValuesRef<FloatVar>? = null,
      val posVertices: CValuesRef<FloatVar>? = null
  )

  actual fun setTexture(texture: Int) {
    this.texture = texture
  }

  actual fun setImage(image: Bitmap) {
    if (image !== this.image) {
      this.image = image
      isDirty = true
    }
    draw()
  }

  actual fun draw() {
    createProgramIfNeeded()
    if (isDirty) {
      updateTexture(image!!)
      isDirty = false
    }
    renderTexture()
    // need to call requestRender() here
  }

  private fun updateTexture(bitmap: Bitmap) {
    createTexture(bitmap)
    glBindTexture(GL_TEXTURE_2D, texture!!)
    glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, bitmap.width, bitmap.height, GL_BGRA_IMG,
        GL_UNSIGNED_BYTE, bitmap.pixels.toCValues())
  }

  private fun createTexture(bitmap: Bitmap) {
    memScoped {
      glBindTexture(GL_TEXTURE_2D, texture!!)
      glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, bitmap.width, bitmap.height, 0, GL_RGB,
          GL_UNSIGNED_SHORT_5_6_5, bitmap.pixels.toCValues())
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
      glVertexAttribPointer(context.texCoordHandle, 2, GL_FLOAT, 0, 0, context.texVertices)
      glEnableVertexAttribArray(context.texCoordHandle)
      glVertexAttribPointer(context.posCoordHandle, 2, GL_FLOAT, 0, 0, context.posVertices)
      glEnableVertexAttribArray(context.posCoordHandle)
    }
  }

  private fun renderTexture() {
    // Set the input texture
    glActiveTexture(GL_TEXTURE0)
    glBindTexture(GL_TEXTURE_2D, texture!!)
    glUniform1i(context!!.texSamplerHandle, 0)
    // Draw!
    glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)
  }

  private fun loadShader(shaderType: Int, source: String): Int {
    val shader = glCreateShader(shaderType)
    if (shader != 0) {
      memScoped {
        val bytes = source.cstr.getPointer(memScope)
        glShaderSource(shader, 1, cValuesOf(bytes), null)
        glCompileShader(shader)
        val compiled = IntArray(1)
        glGetShaderiv(shader, GL_COMPILE_STATUS, compiled.toCValues())
        if (compiled[0] == 0) {
          val infoLen = alloc<IntVar>()
          glGetShaderiv(shader, GL_INFO_LOG_LENGTH, infoLen.ptr)
          if (infoLen.value > 0) {
            val buf = ByteArray(infoLen.value)
            glGetShaderInfoLog(shader, infoLen.value, null, buf.toCValues())
            glDeleteShader(shader)
            throw RuntimeException("Could not compile shader $shaderType:${buf.stringFromUtf8()}")
          }
        }
      }
    }
    return shader
  }

  private fun createProgramIfNeeded() {
    if (context != null) {
      return
    }
    val vertexShader = loadShader(GL_VERTEX_SHADER, VERTEX_SHADER)
    if (vertexShader == 0) {
      return
    }
    val pixelShader = loadShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
    if (pixelShader == 0) {
      return
    }
    val program = glCreateProgram()
    if (program != 0) {
      glAttachShader(program, vertexShader)
      glAttachShader(program, pixelShader)
      glLinkProgram(program)
      val linkStatus = IntArray(1)
      glGetProgramiv(program, GL_LINK_STATUS, linkStatus.toCValues())
      if (linkStatus[0] != GL_TRUE) {
        val buff = ByteArray(256)
        memScoped {
          val len = alloc<IntVar>()
          glGetProgramInfoLog(program, buff.size, len.ptr, buff.toCValues())
          glDeleteProgram(program)
          println("Could not link program: ${buff.stringFromUtf8()}")
          return
        }
      }
    }
    // Bind attributes and uniforms
    context = RenderContext(
        texSamplerHandle = glGetUniformLocation(program, "tex_sampler"),
        texCoordHandle = glGetAttribLocation(program, "a_texcoord"),
        posCoordHandle = glGetAttribLocation(program, "a_position"),
        texVertices = TEX_VERTICES.toCValues(),
        posVertices = POS_VERTICES.toCValues(),
        shaderProgram = program
    )
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
