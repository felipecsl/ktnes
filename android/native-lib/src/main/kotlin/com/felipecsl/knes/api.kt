package com.felipecsl.knes

import konan.internal.CName
import kotlinx.cinterop.*
import platform.android.*

var director: Director? = null

@CName(fullName = "Java_com_felipecsl_knes_JniKt_nativeStartConsole")
fun nativeStartConsole(
    env: CPointer<JNIEnvVar>,
    self: jobject,
    cartridgeData: jbyteArray
) {
  val jni = env.pointed.pointed!!
  val len = jni.GetArrayLength!!.invoke(env, cartridgeData)
  memScoped {
    val buffer = ByteArray(len)
    val bufferPointer = buffer.refTo(0).getPointer(memScope)
    jni.GetByteArrayRegion!!.invoke(env, cartridgeData, 0, len, bufferPointer)
    director = Director(buffer)
    director!!.run()
  }
}

@CName(fullName = "Java_com_felipecsl_knes_JniKt_nativeGetConsoleBuffer")
fun nativeGetConsoleBuffer(): IntArray? {
  return director?.buffer()
}