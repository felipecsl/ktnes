package com.felipecsl.knes

import konan.internal.CName
import kotlinx.cinterop.*
import platform.android.*

@CName(fullName = "Java_com_felipecsl_knes_JniKt_startConsole")
fun startConsole(env: CPointer<JNIEnvVar>, self: jobject, cartridgeData: jbyteArray) {
  val jni = env.pointed.pointed!!
  val len = jni.GetArrayLength!!.invoke(env, cartridgeData)
  memScoped {
    val buffer = ByteArray(len)
    val surface = Surface()
    val bufferPointer = buffer.refTo(0).getPointer(memScope)
    jni.GetByteArrayRegion!!.invoke(env, cartridgeData, 0, len, bufferPointer)
    Director.startConsole(buffer, surface)
  }
}