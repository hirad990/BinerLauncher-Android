#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include <mutex>
#include <queue>
#include <cstdint>

#define LOG_TAG "BinerLauncherNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static std::mutex g_surface_mutex;
static ANativeWindow* g_window = nullptr;

struct InputEvent {
    int type;
    int code;
    float x;
    float y;
};

static std::mutex g_input_mutex;
static std::queue<InputEvent> g_input_queue;
static constexpr size_t MAX_INPUT_QUEUE = 4096;

static void push_input(int type, int code, float x = 0.0f, float y = 0.0f) {
    std::lock_guard<std::mutex> lock(g_input_mutex);
    if (g_input_queue.size() >= MAX_INPUT_QUEUE) g_input_queue.pop();
    g_input_queue.push({type, code, x, y});
}

extern "C" JNIEXPORT jstring JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeGetEngineInfo(
        JNIEnv* env, jobject) {
    return env->NewStringUTF("BinerLauncher Native Engine / Android NDK / InputPipe v3");
}

extern "C" JNIEXPORT jint JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeAbiVersion(
        JNIEnv*, jobject) {
    LOGI("Native bridge initialized");
    return 3;
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
    push_input(1, action, x, y);
}

extern "C" JNIEXPORT void JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeDispatchKey(
        JNIEnv*, jobject, jint action, jint keyCode) {
    // type 2 = keyboard; action 0 = down, 1 = up.
    push_input(2, (action << 16) | (keyCode & 0xffff));
}

extern "C" JNIEXPORT void JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeDispatchMouse(
        JNIEnv*, jobject, jint action, jint button) {
    // type 3 = mouse button; action 0 = down, 1 = up.
    push_input(3, (action << 16) | (button & 0xffff));
}

extern "C" JNIEXPORT void JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeDispatchScroll(
        JNIEnv*, jobject, jfloat amount) {
    push_input(4, 0, amount, 0.0f);
}

extern "C" JNIEXPORT jlong JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativePollInput(
        JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_input_mutex);
    if (g_input_queue.empty()) return 0;
    const auto event = g_input_queue.front();
    g_input_queue.pop();
    // Packed representation: type(8) | code(24). Coordinates are intentionally
    // delivered by the touch callback to the future GLFW bridge.
    return (static_cast<jlong>(event.type) << 32) | static_cast<uint32_t>(event.code);
}
