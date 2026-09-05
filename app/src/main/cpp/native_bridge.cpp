#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include <mutex>

#define LOG_TAG "BinerLauncherNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static std::mutex g_surface_mutex;
static ANativeWindow* g_window = nullptr;

extern "C" JNIEXPORT jstring JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeGetEngineInfo(
        JNIEnv* env, jobject) {
    return env->NewStringUTF("BinerLauncher Native Engine / Android NDK");
}

extern "C" JNIEXPORT jint JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeAbiVersion(
        JNIEnv*, jobject) {
    LOGI("Native bridge initialized");
    return 2;
}

extern "C" JNIEXPORT void JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeAttachSurface(
        JNIEnv* env, jobject, jobject surface) {
    std::lock_guard<std::mutex> lock(g_surface_mutex);
    if (g_window != nullptr) {
        ANativeWindow_release(g_window);
        g_window = nullptr;
    }
    g_window = ANativeWindow_fromSurface(env, surface);
    LOGI("Game surface attached: %p", g_window);
}

extern "C" JNIEXPORT void JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeDetachSurface(
        JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_surface_mutex);
    if (g_window != nullptr) {
        ANativeWindow_release(g_window);
        g_window = nullptr;
    }
    LOGI("Game surface detached");
}

extern "C" JNIEXPORT void JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeDispatchTouch(
        JNIEnv*, jobject, jint action, jfloat x, jfloat y) {
    LOGI("Touch action=%d x=%f y=%f", action, x, y);
}
