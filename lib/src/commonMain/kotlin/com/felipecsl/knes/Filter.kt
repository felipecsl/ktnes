package com.felipecsl.knes

import kotlin.math.PI

internal interface Filter {
  fun step(x: Float): Float
}

// First order filters are defined by the following parameters.
// y[n] = B0*x[n] + B1*x[n-1] - A1*y[n-1]
internal data class FirstOrderFilter(
    private val B0: Float,
    private val B1: Float,
    private val A1: Float,
    private var prevX: Float = 0F,
    private var prevY: Float = 0F
) : Filter {
  override fun step(x: Float): Float {
    val y = B0 * x + B1 * prevX - A1 * prevY
    prevY = y
    prevX = x
    return y
  }
}

internal fun lowPassFilter(sampleRate: Float, cutoffFreq: Float): Filter {
  val c = (sampleRate / PI / cutoffFreq).toFloat()
  val a0i = 1 / (1 + c)
  return FirstOrderFilter(a0i, a0i, (1 - c) * a0i)
}

internal fun highPassFilter(sampleRate: Float, cutoffFreq: Float): Filter {
  val c = (sampleRate / PI / cutoffFreq).toFloat()
  val a0i = 1 / (1 + c)
  return FirstOrderFilter(c * a0i, -c * a0i, (1 - c) * a0i)
}
