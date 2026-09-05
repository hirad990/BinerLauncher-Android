#include <android/log.h>

extern "C" int binerlauncher_native_ready() {
    __android_log_print(ANDROID_LOG_INFO, "BinerLauncher", "Native bridge loaded");
    return 1;
}
