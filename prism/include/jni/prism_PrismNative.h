/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class prism_PrismNative */

#ifndef _Included_prism_PrismNative
#define _Included_prism_PrismNative
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     prism_PrismNative
 * Method:    PN_FreeGlobalRefs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1FreeGlobalRefs
  (JNIEnv *, jclass);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetPrism
 * Signature: (Lprism/Prism;)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetPrism
  (JNIEnv *, jclass, jobject);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetCompact
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetCompact
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetLinEqMethod
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetLinEqMethod
  (JNIEnv *, jclass, jint);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetLinEqMethodParam
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetLinEqMethodParam
  (JNIEnv *, jclass, jdouble);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetTermCrit
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetTermCrit
  (JNIEnv *, jclass, jint);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetTermCritParam
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetTermCritParam
  (JNIEnv *, jclass, jdouble);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetMaxIters
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetMaxIters
  (JNIEnv *, jclass, jint);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetSBMaxMem
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetSBMaxMem
  (JNIEnv *, jclass, jint);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetNumSBLevels
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetNumSBLevels
  (JNIEnv *, jclass, jint);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetSORMaxMem
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetSORMaxMem
  (JNIEnv *, jclass, jint);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetNumSORLevels
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetNumSORLevels
  (JNIEnv *, jclass, jint);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetDoSSDetect
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetDoSSDetect
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetExportAdv
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetExportAdv
  (JNIEnv *, jclass, jint);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetExportAdvFilename
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetExportAdvFilename
  (JNIEnv *, jclass, jstring);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetDefaultExportIterationsFilename
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetDefaultExportIterationsFilename
  (JNIEnv *, jclass, jstring);

/*
 * Class:     prism_PrismNative
 * Method:    PN_SetWorkingDirectory
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_prism_PrismNative_PN_1SetWorkingDirectory
  (JNIEnv *, jclass, jstring);

/*
 * Class:     prism_PrismNative
 * Method:    PN_GetStdout
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_prism_PrismNative_PN_1GetStdout
  (JNIEnv *, jclass);

/*
 * Class:     prism_PrismNative
 * Method:    PN_OpenFile
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_prism_PrismNative_PN_1OpenFile
  (JNIEnv *, jclass, jstring);

/*
 * Class:     prism_PrismNative
 * Method:    PN_OpenFileAppend
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_prism_PrismNative_PN_1OpenFileAppend
  (JNIEnv *, jclass, jstring);

/*
 * Class:     prism_PrismNative
 * Method:    PN_PrintToFile
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1PrintToFile
  (JNIEnv *, jclass, jlong, jstring);

/*
 * Class:     prism_PrismNative
 * Method:    PN_FlushFile
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1FlushFile
  (JNIEnv *, jclass, jlong);

/*
 * Class:     prism_PrismNative
 * Method:    PN_CloseFile
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1CloseFile
  (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif
