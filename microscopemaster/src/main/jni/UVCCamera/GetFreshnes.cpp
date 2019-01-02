//
// Created by Mr.Wen on 2017/4/5.
//

#include "com_yeespec_libuvccamera_uvccamera_glutils_JniUtils.h"
/*
// Class:     Java_com_wobiancao_ndkjnidemo_ndk_JniUtils
* Method:    getStringFormC
* Signature: ()Ljava/lang/String;
*/
extern "C" {

    JNIEXPORT jstring JNICALL Java_com_yeespec_libuvccamera_uvccamera_glutils_JniUtils_getFreShnesFormC(JNIEnv *env, jobject obj)
    {
        return (*env)->NewStringUTF(env, "这里是来自c的string");
    }
}