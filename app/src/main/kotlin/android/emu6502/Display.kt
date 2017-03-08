package android.emu6502

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

open class Display(context: Context, attrs: AttributeSet) : View(context, attrs) {
  private val numX = 32
  private val numY = 32
  private val matrix = Array(32, { IntArray(32) })

  private val palette = arrayOf(
      "#000000", "#ffffff", "#880000", "#aaffee",
      "#cc44cc", "#00cc55", "#0000aa", "#eeee77",
      "#dd8855", "#664400", "#ff7777", "#333333",
      "#777777", "#aaff66", "#0088ff", "#bbbbbb")

  private val paint: Paint = Paint()
  private val bgPaint: Paint = Paint()
  private var listener: Callbacks? = null

  fun setOnDisplayCallback(callback: Callbacks) {
    listener = callback
  }

  open fun updatePixel(addr: Int, value: Int) {
    val offsetAddr = addr - 0x200
    val x = offsetAddr % 32
    val y = Math.floor((offsetAddr / 32).toDouble()).toInt()
    val color = palette[value]
    matrix[x][y] = Color.parseColor(color)
    postInvalidate()
    listener?.onUpdate()
  }

  override fun onDraw(canvas: Canvas) {
    val pixelSizeX = width / numX
    val pixelSizeY = height / numX

    matrix.forEachIndexed { i, _ ->
      matrix[i].forEachIndexed { j, _ ->
        val color = matrix[i][j]
        val right = (i * pixelSizeX).toFloat()
        val top = (j * pixelSizeY).toFloat()
        if (color != 0) {
          paint.color = color
          canvas.drawRect(right, top, right + pixelSizeX, top + pixelSizeY, paint)
        } else {
          canvas.drawRect(right, top, right + pixelSizeX, top + pixelSizeY, bgPaint)
        }
      }
    }
    listener?.onDraw()
  }

  interface Callbacks {
    fun onUpdate()
    fun onDraw()
  }

  fun reset() {
    matrix.forEachIndexed { i, _ ->
      matrix[i].forEachIndexed { j, _ ->
        matrix[i][j] = 0
      }
    }
    postInvalidate()
  }

  init {
    bgPaint.color = Color.BLACK
  }
}
