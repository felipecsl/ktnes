package com.felipecsl.knes

import kotlin.js.Date

actual fun currentTimeMs(): Double {
  return Date.now()
}
