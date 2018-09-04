package com.felipecsl.knes

internal object AddressingMode {
  const val UNUSED = 0
  const val MODE_ABSOLUTE = 1
  const val MODE_ABSOLUTEX = 2
  const val MODE_ABSOLUTEY = 3
  const val MODE_ACCUMULATOR = 4
  const val MODE_IMMEDIATE = 5
  const val MODE_IMPLIED = 6
  const val MODE_INDEXEDINDIRECT = 7
  const val MODE_INDIRECT = 8
  const val MODE_INDIRECTINDEXED = 9
  const val MODE_RELATIVE = 10
  const val MODE_ZEROPAGE = 11
  const val MODE_ZEROPAGEX = 12
  const val MODE_ZEROPAGEY = 13
}