#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_com_ojhdtapp_tgs2gif_NativeLib_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_ojhdtapp_tgs2gif_NativeLib_tgs2gif(JNIEnv *env, jobject thiz, jstring tgs_path,
                                            jstring gif_path, jint width, jint height, jint fps,
                                            jint quality) {
    char path[300] = "./bin/tgs_to_gif.sh";
    // 拼接参数
    char width_str[10];
    sprintf(width_str, "%d", width);
    char height_str[10];
    sprintf(height_str, "%d", height);
    char fps_str[10];
    sprintf(fps_str, "%d", fps);
    char quality_str[10];
    sprintf(quality_str, "%d", quality);
    // 拼接命令
    strcat(path, " ");
    strcat(path, env->GetStringUTFChars(tgs_path, 0));
    strcat(path, " --output ");
    strcat(path, env->GetStringUTFChars(gif_path, 0));
    strcat(path, " --width ");
    strcat(path, width_str);
    strcat(path, " --height ");
    strcat(path, height_str);
    strcat(path, " --fps ");
    strcat(path, fps_str);
    strcat(path, " --quality ");
    strcat(path, quality_str);
    // 执行命令
    return system(path);
}