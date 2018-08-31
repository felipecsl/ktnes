package com.felipecsl.knes

import kotlinx.cinterop.*
import platform.gles2.*

actual class Surface {
  private var context: RenderContext? = null
  private val texture: Int
  private var image: Bitmap? = null
  private var isDirty = false

  init {
    val textureHandle = IntArray(1)
    glGenTextures(1, textureHandle.toCValues())
    texture = textureHandle[0]
  }

  data class RenderContext(
      val shaderProgram: Int = 0,
      val texSamplerHandle: Int = 0,
      val texCoordHandle: Int = 0,
      val posCoordHandle: Int = 0,
      val texVertices: CValuesRef<FloatVar>? = null,
      val posVertices: CValuesRef<FloatVar>? = null
  )

  actual fun setTexture(bitmap: Bitmap) {
    if (image !== this.image) {
      this.image = image
      isDirty = true
    }
    draw()
  }

  fun draw() {
    if (isDirty) {
      createTexture(image!!)
      isDirty = false
    }
    renderTexture()
  }

  private fun createTexture(bitmap: Bitmap) {
    memScoped {
      glBindTexture(GL_TEXTURE_2D, texture)
      glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, bitmap.width, bitmap.height, 0, GL_RGB,
          GL_UNSIGNED_SHORT_5_6_5, bitmap.pixels.toCValues())
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
    }
  }

  private fun renderTexture() {
    val context = createProgram() ?: return
    // Use our shader program
    glUseProgram(context.shaderProgram)
    // Disable blending
    glDisable(GL_BLEND)
    // Set the vertex attributes
    glVertexAttribPointer(context.texCoordHandle, 2, GL_FLOAT, 0, 0, context.texVertices)
    glEnableVertexAttribArray(context.texCoordHandle)
    glVertexAttribPointer(context.posCoordHandle, 2, GL_FLOAT, 0, 0, context.posVertices)
    glEnableVertexAttribArray(context.posCoordHandle)
    // Set the input texture
    glActiveTexture(GL_TEXTURE0)
    glBindTexture(GL_TEXTURE_2D, texture)
    glUniform1i(context.texSamplerHandle, 0)
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

/*  fun initialize(window: CPointer<ANativeWindow>): Boolean {
    with (container.arena) {
      LOG.info("Initializing context..")
      val display = eglGetDisplay(null)
      if (display == null) {
        logError("eglGetDisplay() returned error ${eglGetError()}")
        return false
      }
      if (eglInitialize(display, null, null) == 0) {
        LOG.error("eglInitialize() returned error ${eglGetError()}")
        return false
      }

      val attribs = cValuesOf(
          EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
          EGL_BLUE_SIZE, 8,
          EGL_GREEN_SIZE, 8,
          EGL_RED_SIZE, 8,
          EGL_NONE
      )
      val numConfigs = alloc<IntVar>()
      if (eglChooseConfig(display, attribs, null, 0, numConfigs.ptr) == 0) {
        throw Error("eglChooseConfig()#1 returned error ${eglGetError()}")
      }
      val supportedConfigs = allocArray<EGLConfigVar>(numConfigs.value)
      if (eglChooseConfig(display, attribs, supportedConfigs, numConfigs.value, numConfigs.ptr) == 0) {
        throw Error("eglChooseConfig()#2 returned error ${eglGetError()}")
      }
      var configIndex = 0
      while (configIndex < numConfigs.value) {
        val r = alloc<IntVar>()
        val g = alloc<IntVar>()
        val b = alloc<IntVar>()
        val d = alloc<IntVar>()
        if (eglGetConfigAttrib(display, supportedConfigs[configIndex], EGL_RED_SIZE, r.ptr) != 0 &&
            eglGetConfigAttrib(display, supportedConfigs[configIndex], EGL_GREEN_SIZE, g.ptr) != 0 &&
            eglGetConfigAttrib(display, supportedConfigs[configIndex], EGL_BLUE_SIZE, b.ptr) != 0 &&
            eglGetConfigAttrib(display, supportedConfigs[configIndex], EGL_DEPTH_SIZE, d.ptr) != 0 &&
            r.value == 8 && g.value == 8 && b.value == 8 && d.value == 0) break
        ++configIndex
      }
      if (configIndex >= numConfigs.value)
        configIndex = 0

      val surface = eglCreateWindowSurface(display, supportedConfigs[configIndex], window, null)
          ?: throw Error("eglCreateWindowSurface() returned error ${eglGetError()}")

      val context = eglCreateContext(display, supportedConfigs[configIndex], null, null)
          ?: throw Error("eglCreateContext() returned error ${eglGetError()}")

      if (eglMakeCurrent(display, surface, surface, context) == 0) {
        throw Error("eglMakeCurrent() returned error ${eglGetError()}")
      }

      val width = alloc<IntVar>()
      val height = alloc<IntVar>()
      if (eglQuerySurface(display, surface, EGL_WIDTH, width.ptr) == 0
          || eglQuerySurface(display, surface, EGL_HEIGHT, height.ptr) == 0) {
        throw Error("eglQuerySurface() returned error ${eglGetError()}")
      }

      glViewport(0, 0, width.value, height.value)

      return true
    }
  }*/

  private fun createProgram(): RenderContext? {
    if (context != null) {
      return context
    }
    val vertexShader = loadShader(GL_VERTEX_SHADER, VERTEX_SHADER)
    if (vertexShader == 0) {
      return null
    }
    val pixelShader = loadShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
    if (pixelShader == 0) {
      return null
    }
    val program = glCreateProgram()
    if (program != 0) {
      glAttachShader(program, vertexShader)
      glAttachShader(program, pixelShader)
      glLinkProgram(program)
      val linkStatus = IntArray(1)
      glGetProgramiv(program, GL_LINK_STATUS, linkStatus.toCValues())
      if (linkStatus[0] != GL_TRUE) {
//        val info = glGetProgramInfoLog(program)
        glDeleteProgram(program)
        throw RuntimeException("Could not link program")
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
    return context
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