package com.felipecsl.knes

internal class FilterChain(private val filters: Array<Filter>) {
  fun step(x: Float): Float {
    filters.forEach {
      it.step(x)
    }
    return x
  }
}