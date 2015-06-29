package android.emu6502

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.util.AttributeSet
import android.util.Log
import android.view.View
import java.util.ArrayList

open class Display : View {
  private val numX = 32
  private val numY = 32

  private val palette = arrayOf(
      "#000000", "#ffffff", "#880000", "#aaffee",
      "#cc44cc", "#00cc55", "#0000aa", "#eeee77",
      "#dd8855", "#664400", "#ff7777", "#333333",
      "#777777", "#aaff66", "#0088ff", "#bbbbbb")

  private val drawingCache = ArrayList<Pixel>()
  private val paint: Paint
  private val bgPaint: Paint

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    paint = Paint()
    bgPaint = Paint()
    bgPaint.setColor(Color.BLACK);
  }

  open fun updatePixel(addr: Int, value: Int) {
    val color = palette[value]
    val x = (addr - 0x200) % 32
    val y = Math.floor(((addr - 0x200) / 32).toDouble())
    drawingCache.add(Pixel(Point(x.toInt(), y.toInt()), Color.parseColor(color)))
    postInvalidate()
  }

  override fun onDraw(canvas: Canvas) {
    val pixelSize = getWidth() / numX

    canvas.drawRect(0f, 0f, getWidth().toFloat(), getHeight().toFloat(), bgPaint)

    for (pixel in drawingCache) {
      val right = (pixel.point.x * pixelSize).toFloat()
      val top = (pixel.point.y * pixelSize).toFloat()
      paint.setColor(pixel.color)
      canvas.drawRect(right, top, right + pixelSize, top + pixelSize, paint)
    }

    drawingCache.clear()
  }

  class Pixel(val point: Point, val color: Int) {
  }
}
