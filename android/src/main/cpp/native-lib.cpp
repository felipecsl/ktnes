#include <jni.h>
#include <string>

#include "AudioEngine.h"
#include "../../../../oboe/src/common/OboeDebug.h"

AudioEngine engine;
JavaVM *gJvm = nullptr;
jobject gClassLoader;
jmethodID gFindClassMethod;

JNIEXPORT jint
JNI_OnLoad(JavaVM *vm, void *reserved) {
  gJvm = vm;
  JNIEnv* env;
  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return -1;
  }
  jclass clsMain = env->FindClass("com/felipecsl/knes/app/MainActivity");
  jclass classClass = env->GetObjectClass(clsMain);
  jclass classLoaderClass = env->FindClass("java/lang/ClassLoader");
  jmethodID getClassLoaderMethod =
      env->GetMethodID(classClass, "getClassLoader", "()Ljava/lang/ClassLoader;");
  gClassLoader = env->NewGlobalRef(env->CallObjectMethod(clsMain, getClassLoaderMethod));
  gFindClassMethod =
      env->GetMethodID(classLoaderClass, "findClass", "(Ljava/lang/String;)Ljava/lang/Class;");
  return JNI_VERSION_1_6;
}

std::vector<int> javaArrayToStdVector(JNIEnv *env, jintArray intArray) {
  std::vector<int> v;
  jsize length = env->GetArrayLength(intArray);
  if (length > 0) {
    jboolean isCopy;
    jint *elements = env->GetIntArrayElements(intArray, &isCopy);
    for (int i = 0; i < length; i++) {
      v.push_back(elements[i]);
    }
  }
  return v;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_felipecsl_knes_JniKt_startAudioEngine(
    JNIEnv *env,
    jobject instance,
    jintArray jCpuIds
) {
  std::vector<int> cpuIds = javaArrayToStdVector(env, jCpuIds);
  engine.start(cpuIds, gJvm, gClassLoader, gFindClassMethod);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_felipecsl_knes_JniKt_stopAudioEngine(JNIEnv *env, jobject instance) {
  engine.stop();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_felipecsl_knes_JniKt_pauseAudioEngine(JNIEnv *env, jobject instance) {
  engine.pause();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_felipecsl_knes_JniKt_resumeAudioEngine(JNIEnv *env, jobject instance) {
  engine.resume();
}