#ifndef KTNES_AUDIOENGINE_H
#define KTNES_AUDIOENGINE_H


#include <oboe/Oboe.h>
#include <vector>
#include <jni.h>
#include <android/log.h>
#include <deque>

#ifndef MODULE_NAME
#define MODULE_NAME  "KTNES"
#endif

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, MODULE_NAME, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, MODULE_NAME, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, MODULE_NAME, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, MODULE_NAME, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, MODULE_NAME, __VA_ARGS__)
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL, MODULE_NAME, __VA_ARGS__)

using namespace oboe;

class AudioEngine : public AudioStreamCallback {

public:
    void start(
        std::vector<int> cpuIds,
        JavaVM *javaVm,
        jobject classLoader,
        jmethodID findClassMethod
    );

    void play(float data);

    DataCallbackResult
    onAudioReady(AudioStream *oboeStream, void *audioData, int32_t numFrames) override;
    void stop() const;
    void pause() const;
    void resume() const;

private:

    AudioStream *mStream = nullptr;
    JavaVM *mJavaVM = nullptr;
    jobject mClassLoader;
    jmethodID mFindClassMethod;
    std::vector<int> mCpuIds; // IDs of CPU cores which the audio callback should be bound to
    bool mIsThreadAffinitySet = false;
    jclass mMainActivityClass = nullptr;
    jmethodID mAudioBufferMethod = nullptr;

    void setThreadAffinity();
    JNIEnv *getEnv();
    jclass findClass(JNIEnv *env, const char *name);
    void maybeInitAudioBufferMethod(JNIEnv *jniEnv);
};


#endif //KTNES_AUDIOENGINE_H
