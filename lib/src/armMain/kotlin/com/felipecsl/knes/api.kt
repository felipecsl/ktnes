package com.felipecsl.knes

import kotlin.native.CName
import kotlin.native.SharedImmutable
import kotlinx.cinterop.*
import platform.android.*

@SharedImmutable var directorBuffer: IntArray? = null

@CName(externName = "Java_com_felipecsl_knes_JniKt_nativeStartConsole")
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
    val director = Director(buffer)
    directorBuffer = director.videoBuffer()
    // TODO(felipecsl) Fix or remove this
    // director.run()
  }
}

@CName(externName = "Java_com_felipecsl_knes_JniKt_nativeGetConsoleBuffer")
fun nativeGetConsoleBuffer(
    env: CPointer<JNIEnvVar>,
    self: jobject
): IntArray? {
  return directorBuffer
}