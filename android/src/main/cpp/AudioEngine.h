#ifndef KTNES_AUDIOENGINE_H
#define KTNES_AUDIOENGINE_H


#include <oboe/Oboe.h>
#include <vector>
#include <jni.h>
#include <deque>

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
