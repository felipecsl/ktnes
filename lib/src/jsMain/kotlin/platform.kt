package com.felipecsl.knes

import kotlin.js.Date

actual fun currentTimeMs(): Long {
  return Date.now().toLong()
}
