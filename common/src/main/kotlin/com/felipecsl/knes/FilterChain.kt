package com.felipecsl.knes

internal class FilterChain(internal val filters: Array<Filter>) {
  fun step(x: Float): Float {
    filters.forEach {
      it.step(x)
    }
    return x
  }
}