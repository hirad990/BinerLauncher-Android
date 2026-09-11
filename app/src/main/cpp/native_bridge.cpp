#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>
#include <android/native_window.h>
#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <mutex>
#include <queue>
#include <cstdint>
#include <cmath>

#define LOG_TAG "BinerLauncherNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static std::mutex g_surface_mutex;
static ANativeWindow* g_window = nullptr;
static EGLDisplay g_display = EGL_NO_DISPLAY;
static EGLSurface g_surface = EGL_NO_SURFACE;
static EGLContext g_context = EGL_NO_CONTEXT;

struct InputEvent { int type; int code; float x; float y; };
static std::mutex g_input_mutex;
static std::queue<InputEvent> g_input_queue;
static constexpr size_t MAX_INPUT_QUEUE = 4096;

static void push_input(int type, int code, float x = 0.0f, float y = 0.0f) {
    std::lock_guard<std::mutex> lock(g_input_mutex);
    if (g_input_queue.size() >= MAX_INPUT_QUEUE) g_input_queue.pop();
    g_input_queue.push({type, code, x, y});
}

static void destroy_egl_locked() {
    if (g_display != EGL_NO_DISPLAY) {
        eglMakeCurrent(g_display, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (g_context != EGL_NO_CONTEXT) eglDestroyContext(g_display, g_context);
        if (g_surface != EGL_NO_SURFACE) eglDestroySurface(g_display, g_surface);
        eglTerminate(g_display);
    }
    g_display = EGL_NO_DISPLAY;
    g_surface = EGL_NO_SURFACE;
    g_context = EGL_NO_CONTEXT;
}

static bool create_egl_locked() {
    if (!g_window) return false;
    g_display = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (g_display == EGL_NO_DISPLAY || !eglInitialize(g_display, nullptr, nullptr)) return false;
    const EGLint attrs[] = {EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT, EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_ALPHA_SIZE,8,EGL_DEPTH_SIZE,24,EGL_STENCIL_SIZE,8,EGL_NONE};
    EGLConfig config = nullptr; EGLint count = 0;
    if (!eglChooseConfig(g_display, attrs, &config, 1, &count) || count == 0) { destroy_egl_locked(); return false; }
    const EGLint ctx[] = {EGL_CONTEXT_CLIENT_VERSION,2,EGL_NONE};
    g_context = eglCreateContext(g_display, config, EGL_NO_CONTEXT, ctx);
    if (g_context == EGL_NO_CONTEXT) { destroy_egl_locked(); return false; }
    g_surface = eglCreateWindowSurface(g_display, config, g_window, nullptr);
    if (g_surface == EGL_NO_SURFACE || !eglMakeCurrent(g_display,g_surface,g_surface,g_context)) { destroy_egl_locked(); return false; }
    eglSwapInterval(g_display, 1);
    glViewport(0, 0, ANativeWindow_getWidth(g_window), ANativeWindow_getHeight(g_window));
    return true;
}

extern "C" JNIEXPORT jstring JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeGetEngineInfo(JNIEnv* env, jobject) {
    return env->NewStringUTF("BinerLauncher Android Native Engine / EGL OpenGL ES2 / InputPipe v4");
}
extern "C" JNIEXPORT jint JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeAbiVersion(JNIEnv*, jobject) { return 4; }
extern "C" JNIEXPORT jboolean JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeAttachSurface(JNIEnv* env, jobject, jobject surface) {
    std::lock_guard<std::mutex> lock(g_surface_mutex);
    destroy_egl_locked();
    if (g_window) { ANativeWindow_release(g_window); g_window = nullptr; }
    g_window = ANativeWindow_fromSurface(env, surface);
    const bool ok = g_window && create_egl_locked();
    LOGI("Game surface attached, EGL=%s", ok ? "ready" : "failed");
    return ok ? JNI_TRUE : JNI_FALSE;
}
extern "C" JNIEXPORT void JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeDetachSurface(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_surface_mutex);
    destroy_egl_locked();
    if (g_window) { ANativeWindow_release(g_window); g_window = nullptr; }
}
extern "C" JNIEXPORT void JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeRenderTestFrame(JNIEnv*, jobject, jfloat t) {
    std::lock_guard<std::mutex> lock(g_surface_mutex);
    if (g_display == EGL_NO_DISPLAY || g_surface == EGL_NO_SURFACE) return;
    glClearColor(0.025f + 0.02f*std::sin(t), 0.05f, 0.12f + 0.04f*std::cos(t), 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);
    eglSwapBuffers(g_display, g_surface);
}
extern "C" JNIEXPORT void JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeDispatchTouch(JNIEnv*, jobject, jint action, jfloat x, jfloat y) { push_input(1,action,x,y); }
extern "C" JNIEXPORT void JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeDispatchKey(JNIEnv*, jobject, jint action, jint keyCode) { push_input(2,(action<<16)|(keyCode&0xffff)); }
extern "C" JNIEXPORT void JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeDispatchMouse(JNIEnv*, jobject, jint action, jint button) { push_input(3,(action<<16)|(button&0xffff)); }
extern "C" JNIEXPORT void JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativeDispatchScroll(JNIEnv*, jobject, jfloat amount) { push_input(4,0,amount,0); }
extern "C" JNIEXPORT jlong JNICALL
Java_ir_binercraft_binerlauncher_nativebridge_NativeBridge_nativePollInput(JNIEnv*, jobject) {
    std::lock_guard<std::mutex> lock(g_input_mutex);
    if (g_input_queue.empty()) return 0;
    auto e=g_input_queue.front(); g_input_queue.pop();
    return (static_cast<jlong>(e.type)<<32)|static_cast<uint32_t>(e.code);
}
