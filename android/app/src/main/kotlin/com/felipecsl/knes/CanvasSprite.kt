package com.felipecsl.knes

import android.graphics.Paint
import android.util.Log
import android.view.SurfaceHolder

class CanvasSprite(private val surfaceHolder: SurfaceHolder) : SpriteFacade {
  private var image: Bitmap? = null
  private val paint: Paint = Paint().also { it.style = Paint.Style.FILL }

  override fun draw() {
//    val canvas = surfaceHolder.lockCanvas()
//    if (canvas != null) {
//      val image  = image!!
//      val pixelSizeX = canvas.width / image.width
//      val pixelSizeY = canvas.height / image.height
//      image.pixels.forEachIndexed { i, color ->
//        if (color != 0) {
//          val x = i % image.width
//          val y = i / image.width
//          val right = (x * pixelSizeX).toFloat()
//          val top = (y * pixelSizeY).toFloat()
//          paint.setARGB(255, color shr 16 and 0xFF, color shr 8 and 0xFF, color and 0xFF)
//          canvas.drawRect(right, top, right + pixelSizeX, top + pixelSizeY, paint)
//        }
//      }
//      surfaceHolder.unlockCanvasAndPost(canvas)
//    } else {
//      Log.e(TAG, "Failed to lock surface canvas")
//    }
  }

  override fun setTexture(texture: Int) {
    // no-op
  }

  override fun setImage(image: Bitmap) {
    this.image = image
    draw()
  }

  companion object {
    private const val TAG = "CanvasSprite"
  }
}