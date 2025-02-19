/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_mediatek_a3m_Vector4 */

#ifndef _Included_com_mediatek_a3m_Vector4
#define _Included_com_mediatek_a3m_Vector4
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    create
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_mediatek_a3m_Vector4_create__
  (JNIEnv *, jobject);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    create
 * Signature: (FFFF)V
 */
JNIEXPORT void JNICALL Java_com_mediatek_a3m_Vector4_create__FFFF
  (JNIEnv *, jobject, jfloat, jfloat, jfloat, jfloat);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    create
 * Signature: (Lcom/mediatek/a3m/Vector3;F)V
 */
JNIEXPORT void JNICALL Java_com_mediatek_a3m_Vector4_create__Lcom_mediatek_a3m_Vector3_2F
  (JNIEnv *, jobject, jobject, jfloat);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    destroy
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_mediatek_a3m_Vector4_destroy
  (JNIEnv *, jobject);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    add
 * Signature: (Lcom/mediatek/a3m/Vector4;Lcom/mediatek/a3m/Vector4;)Lcom/mediatek/a3m/Vector4;
 */
JNIEXPORT jobject JNICALL Java_com_mediatek_a3m_Vector4_add
  (JNIEnv *, jclass, jobject, jobject);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    subtract
 * Signature: (Lcom/mediatek/a3m/Vector4;Lcom/mediatek/a3m/Vector4;)Lcom/mediatek/a3m/Vector4;
 */
JNIEXPORT jobject JNICALL Java_com_mediatek_a3m_Vector4_subtract
  (JNIEnv *, jclass, jobject, jobject);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    multiply
 * Signature: (Lcom/mediatek/a3m/Vector4;Lcom/mediatek/a3m/Vector4;)Lcom/mediatek/a3m/Vector4;
 */
JNIEXPORT jobject JNICALL Java_com_mediatek_a3m_Vector4_multiply__Lcom_mediatek_a3m_Vector4_2Lcom_mediatek_a3m_Vector4_2
  (JNIEnv *, jclass, jobject, jobject);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    multiply
 * Signature: (Lcom/mediatek/a3m/Vector4;F)Lcom/mediatek/a3m/Vector4;
 */
JNIEXPORT jobject JNICALL Java_com_mediatek_a3m_Vector4_multiply__Lcom_mediatek_a3m_Vector4_2F
  (JNIEnv *, jclass, jobject, jfloat);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    divide
 * Signature: (Lcom/mediatek/a3m/Vector4;Lcom/mediatek/a3m/Vector4;)Lcom/mediatek/a3m/Vector4;
 */
JNIEXPORT jobject JNICALL Java_com_mediatek_a3m_Vector4_divide__Lcom_mediatek_a3m_Vector4_2Lcom_mediatek_a3m_Vector4_2
  (JNIEnv *, jclass, jobject, jobject);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    divide
 * Signature: (Lcom/mediatek/a3m/Vector4;F)Lcom/mediatek/a3m/Vector4;
 */
JNIEXPORT jobject JNICALL Java_com_mediatek_a3m_Vector4_divide__Lcom_mediatek_a3m_Vector4_2F
  (JNIEnv *, jclass, jobject, jfloat);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    dot
 * Signature: (Lcom/mediatek/a3m/Vector4;Lcom/mediatek/a3m/Vector4;)F
 */
JNIEXPORT jfloat JNICALL Java_com_mediatek_a3m_Vector4_dot
  (JNIEnv *, jclass, jobject, jobject);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    length
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_mediatek_a3m_Vector4_length
  (JNIEnv *, jobject);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    normalize
 * Signature: ()Lcom/mediatek/a3m/Vector4;
 */
JNIEXPORT jobject JNICALL Java_com_mediatek_a3m_Vector4_normalize
  (JNIEnv *, jobject);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    x
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_mediatek_a3m_Vector4_x__
  (JNIEnv *, jobject);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    y
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_mediatek_a3m_Vector4_y__
  (JNIEnv *, jobject);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    z
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_mediatek_a3m_Vector4_z__
  (JNIEnv *, jobject);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    w
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_com_mediatek_a3m_Vector4_w__
  (JNIEnv *, jobject);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    x
 * Signature: (F)V
 */
JNIEXPORT void JNICALL Java_com_mediatek_a3m_Vector4_x__F
  (JNIEnv *, jobject, jfloat);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    y
 * Signature: (F)V
 */
JNIEXPORT void JNICALL Java_com_mediatek_a3m_Vector4_y__F
  (JNIEnv *, jobject, jfloat);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    z
 * Signature: (F)V
 */
JNIEXPORT void JNICALL Java_com_mediatek_a3m_Vector4_z__F
  (JNIEnv *, jobject, jfloat);

/*
 * Class:     com_mediatek_a3m_Vector4
 * Method:    w
 * Signature: (F)V
 */
JNIEXPORT void JNICALL Java_com_mediatek_a3m_Vector4_w__F
  (JNIEnv *, jobject, jfloat);

#ifdef __cplusplus
}
#endif
#endif
