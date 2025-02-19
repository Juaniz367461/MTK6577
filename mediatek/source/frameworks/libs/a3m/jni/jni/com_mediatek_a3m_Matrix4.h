/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_mediatek_a3m_Matrix4 */

#ifndef _Included_com_mediatek_a3m_Matrix4
#define _Included_com_mediatek_a3m_Matrix4
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_mediatek_a3m_Matrix4
 * Method:    destroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_mediatek_a3m_Matrix4_destroy
  (JNIEnv *, jobject);

/*
 * Class:     com_mediatek_a3m_Matrix4
 * Method:    multiply
 * Signature: (Lcom/mediatek/a3m/Matrix4;Lcom/mediatek/a3m/Vector4;)Lcom/mediatek/a3m/Vector4;
 */
JNIEXPORT jobject JNICALL Java_com_mediatek_a3m_Matrix4_multiply__Lcom_mediatek_a3m_Matrix4_2Lcom_mediatek_a3m_Vector4_2
  (JNIEnv *, jclass, jobject, jobject);

/*
 * Class:     com_mediatek_a3m_Matrix4
 * Method:    multiply
 * Signature: (Lcom/mediatek/a3m/Matrix4;Lcom/mediatek/a3m/Matrix4;)Lcom/mediatek/a3m/Matrix4;
 */
JNIEXPORT jobject JNICALL Java_com_mediatek_a3m_Matrix4_multiply__Lcom_mediatek_a3m_Matrix4_2Lcom_mediatek_a3m_Matrix4_2
  (JNIEnv *, jclass, jobject, jobject);

/*
 * Class:     com_mediatek_a3m_Matrix4
 * Method:    inverse
 * Signature: ()Lcom/mediatek/a3m/Matrix4;
 */
JNIEXPORT jobject JNICALL Java_com_mediatek_a3m_Matrix4_inverse
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
