package com.felipecsl.knes

internal class FilterChain(private val filters: Array<Filter>) {
  fun step(x: Float): Float {
    var v = x
    filters.forEach {
      v = it.step(v)
    }
    return v
  }
}