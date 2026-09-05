#include <jni.h>
#include <android/log.h>

#define LOG_TAG "BinerLauncherNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeGetEngineInfo(
        JNIEnv* env, jobject) {
    return env->NewStringUTF("BinerLauncher Native Engine / Android NDK");
}

extern "C" JNIEXPORT jint JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeAbiVersion(
        JNIEnv*, jobject) {
    LOGI("Native bridge initialized");
    return 1;
}
