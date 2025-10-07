#include <jni.h>
#include <string>
#include <android/log.h>

// For OpenGL (we'll add OpenCV later)
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>

#define LOG_TAG "EdgeDetection"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT void JNICALL
Java_com_example_edgedetection_MainActivity_initializeOpenGL(JNIEnv *env, jobject thiz) {
    LOGI("OpenGL initialized - Native");
    
    // Basic OpenGL initialization
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    LOGI("OpenGL clear color set");
}

JNIEXPORT void JNICALL
Java_com_example_edgedetection_MainActivity_processFrame(JNIEnv *env, jobject thiz,
                                                         jbyteArray frame_data,
                                                         jint width, jint height) {
    // Get array elements
    jbyte* frameBytes = env->GetByteArrayElements(frame_data, nullptr);
    
    if (frameBytes != nullptr) {
        LOGI("Processing frame: %dx%d - Data received", width, height);
        
        // TODO: Add OpenCV processing here
        
        // Release array elements
        env->ReleaseByteArrayElements(frame_data, frameBytes, JNI_ABORT);
    } else {
        LOGE("Failed to get frame data");
    }
}

JNIEXPORT void JNICALL
Java_com_example_edgedetection_MainActivity_shutdown(JNIEnv *env, jobject thiz) {
    LOGI("Native resources cleaned up");
}

} // extern "C"