/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_mediatek_a3m_Glo */

#ifndef _Included_com_mediatek_a3m_Glo
#define _Included_com_mediatek_a3m_Glo
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_mediatek_a3m_Glo
 * Method:    create
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_mediatek_a3m_Glo_create__
  (JNIEnv *, jobject);

/*
 * Class:     com_mediatek_a3m_Glo
 * Method:    create
 * Signature: (Lcom/mediatek/a3m/AssetPool;Lcom/mediatek/a3m/SceneNode;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_mediatek_a3m_Glo_create__Lcom_mediatek_a3m_AssetPool_2Lcom_mediatek_a3m_SceneNode_2Ljava_lang_String_2
  (JNIEnv *, jobject, jobject, jobject, jstring);

/*
 * Class:     com_mediatek_a3m_Glo
 * Method:    destroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_mediatek_a3m_Glo_destroy
  (JNIEnv *, jobject);

/*
 * Class:     com_mediatek_a3m_Glo
 * Method:    getNode
 * Signature: ()Lcom/mediatek/a3m/SceneNode;
 */
JNIEXPORT jobject JNICALL Java_com_mediatek_a3m_Glo_getNode
  (JNIEnv *, jobject);

/*
 * Class:     com_mediatek_a3m_Glo
 * Method:    getAnimation
 * Signature: ()Lcom/mediatek/a3m/AnimationGroup;
 */
JNIEXPORT jobject JNICALL Java_com_mediatek_a3m_Glo_getAnimation
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
