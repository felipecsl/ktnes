package android.emu6502

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.util.Log
import android.view.View
import java.util
import java.util.*

open class Display : View {
  private val numX = 32
  private val numY = 32

  private val matrix = Array(32, { IntArray(32) })

  private val palette = arrayOf(
      "#000000", "#ffffff", "#880000", "#aaffee",
      "#cc44cc", "#00cc55", "#0000aa", "#eeee77",
      "#dd8855", "#664400", "#ff7777", "#333333",
      "#777777", "#aaff66", "#0088ff", "#bbbbbb")

  private val paint: Paint
  private val bgPaint: Paint
  private val TAG = "Display"
  private var listener: Callbacks? = null

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    paint = Paint()
    bgPaint = Paint()
    bgPaint.setColor(Color.BLACK);
  }

  fun setOnDisplayCallback(callback: Callbacks) {
    listener = callback;
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
    val pixelSize = getWidth() / numX

    matrix.forEachIndexed { i, _ ->
      matrix[i].forEachIndexed { j, _ ->
        val color = matrix[i][j]
        val right = (i * pixelSize).toFloat()
        val top = (j * pixelSize).toFloat()
        if (color != 0) {
          paint.setColor(color)
          canvas.drawRect(right, top, right + pixelSize, right + pixelSize, paint)
        } else {
          canvas.drawRect(right, top, right + pixelSize, right + pixelSize, bgPaint)
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
}
