/*
	This software is a java wrapper for the lp_solve optimization library.

	Copyright (C) 2004  Juergen Ebert (juergen.ebert@web.de)

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License as published by the Free Software Foundation; either
	version 2.1 of the License, or (at your option) any later version.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
	Lesser General Public License for more details.

	You should have received a copy of the GNU Lesser General Public
	License along with this library; if not, write to the Free Software
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

#include <stdio.h>
#include <stdlib.h>
#include "lpsolve_LpSolve.h"
#include "lp_lib.h"


// =======================================================================
// -----------------  global variables  ----------------------------------
// =======================================================================

/* The global JavaVM object - reference is obtained in OnLoad */
JavaVM *g_jvm = NULL;

/* globally cached method and field IDs */
jmethodID MID_lpsolve_constr = NULL;
jmethodID MID_getLp = NULL;
jfieldID FID_lp = NULL;
jmethodID MID_abortfunc = NULL;
jfieldID FID_abortListener = NULL;
jfieldID FID_abortHandle = NULL;
jmethodID MID_logfunc = NULL;
jfieldID FID_logListener = NULL;
jfieldID FID_logHandle = NULL;
jmethodID MID_msgfunc = NULL;
jfieldID FID_msgListener = NULL;
jfieldID FID_msgHandle = NULL;
jmethodID MID_bbfunc = NULL;
jfieldID FID_bbBranchListener = NULL;
jfieldID FID_bbBranchHandle = NULL;
jfieldID FID_bbNodeListener = NULL;
jfieldID FID_bbNodeHandle = NULL;
jmethodID MID_String_getBytes = NULL;
jmethodID MID_String_init = NULL;


// =======================================================================
// -----------------  DLL initialisation and finalization methods --------
// =======================================================================

extern "C"
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
	//printf("JNI_OnLoad\n");
	g_jvm = vm;
	return JNI_VERSION_1_2;
}

extern "C"
JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
	//printf("JNI_OnUnload\n");
	g_jvm = NULL;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_init
  (JNIEnv *env, jclass cls)
{
	MID_lpsolve_constr = env->GetMethodID(cls, "<init>", "(J)V");
	MID_getLp = env->GetStaticMethodID(cls, "getLp", "(J)Llpsolve/LpSolve;");
	FID_lp = env->GetFieldID(cls, "lp", "J");

	FID_abortListener = env->GetFieldID(cls, "abortListener", "Llpsolve/AbortListener;");
	FID_abortHandle = env->GetFieldID(cls, "abortUserhandle", "Ljava/lang/Object;");
	jclass abort_listener_cls = env->FindClass("lpsolve/AbortListener");
	MID_abortfunc = env->GetMethodID(abort_listener_cls, "abortfunc", "(Llpsolve/LpSolve;Ljava/lang/Object;)Z");
	env->DeleteLocalRef(abort_listener_cls);

	FID_logListener = env->GetFieldID(cls, "logListener", "Llpsolve/LogListener;");
	FID_logHandle = env->GetFieldID(cls, "logUserhandle", "Ljava/lang/Object;");
	jclass log_listener_cls = env->FindClass("lpsolve/LogListener");
	MID_logfunc = env->GetMethodID(log_listener_cls, "logfunc", "(Llpsolve/LpSolve;Ljava/lang/Object;Ljava/lang/String;)V");
	env->DeleteLocalRef(log_listener_cls);

	FID_msgListener = env->GetFieldID(cls, "msgListener", "Llpsolve/MsgListener;");
	FID_msgHandle = env->GetFieldID(cls, "msgUserhandle", "Ljava/lang/Object;");
	jclass msg_listener_cls = env->FindClass("lpsolve/MsgListener");
	MID_msgfunc = env->GetMethodID(msg_listener_cls, "msgfunc", "(Llpsolve/LpSolve;Ljava/lang/Object;I)V");
	env->DeleteLocalRef(msg_listener_cls);

	FID_bbBranchListener = env->GetFieldID(cls, "bbBranchListener", "Llpsolve/BbListener;");
	FID_bbBranchHandle = env->GetFieldID(cls, "bbBranchUserhandle", "Ljava/lang/Object;");
	FID_bbNodeListener = env->GetFieldID(cls, "bbNodeListener", "Llpsolve/BbListener;");
	FID_bbNodeHandle = env->GetFieldID(cls, "bbNodeUserhandle", "Ljava/lang/Object;");
	jclass bb_listener_cls = env->FindClass("lpsolve/BbListener");
	MID_bbfunc = env->GetMethodID(bb_listener_cls, "bbfunc", "(Llpsolve/LpSolve;Ljava/lang/Object;I)I");
	env->DeleteLocalRef(bb_listener_cls);

	jclass string_cls = env->FindClass("java/lang/String");
	MID_String_getBytes = env->GetMethodID(string_cls, "getBytes", "()[B");
	MID_String_init = env->GetMethodID(string_cls, "<init>", "([B)V");
	env->DeleteLocalRef(string_cls);
}


// =======================================================================
// -----------------  helper methods  ------------------------------------
// =======================================================================

void put_lp(JNIEnv *env, jobject obj, lprec *lp) {
	env->SetLongField(obj, FID_lp, reinterpret_cast<jlong>(lp));
}

lprec* get_lp(JNIEnv *env, jobject obj) {
	return (reinterpret_cast<lprec*>(env->GetLongField(obj, FID_lp)));
}

void JNU_ThrowByName(JNIEnv* env, const char* name, const char* msg)
{
	jclass cls = env->FindClass(name);
	/* if cls is NULL, an exception has already been thrown */
	if (cls != NULL) {
		env->ThrowNew(cls, msg);
	}
	/* free local ref */
	env->DeleteLocalRef(cls);
}

void throw_exception(JNIEnv* env, const char* msg) {
	JNU_ThrowByName(env, "lpsolve/LpSolveException", msg);
}

void throw_exception_with_status(JNIEnv* env, lprec* lp, const char* funcname) {
	int statuscode = get_status(lp);
	char *statustext = get_statustext(lp, statuscode);
	char msg[200];
	sprintf(msg, "ERROR in %s: status = %d (%s)", funcname, statuscode, statustext);
	JNU_ThrowByName(env, "lpsolve/LpSolveException", msg);
}

/*
 * Utility function that creates a jstring from a locale-specific
 * native C string.
 * See Liang, The Java Native Interface, p. 99
 */
/*
jstring JNU_NewStringNative(JNIEnv* env, const char *str) {
	jstring result = NULL;
	jbyteArray bytes = 0;
	jsize len;

	if (env->EnsureLocalCapacity(2) < 0) {
		return NULL;
	}
	len = (jsize)strlen(str);
	bytes = env->NewByteArray(len);
	if (bytes != NULL) {
		env->SetByteArrayRegion(bytes, 0, len, (jbyte *)str);
		jclass string_cls = env->FindClass("java/lang/String");
		result = (jstring)env->NewObject(string_cls, MID_String_init, bytes);
		env->DeleteLocalRef(string_cls);
		env->DeleteLocalRef(bytes);
	}
	return result;
}
*/

/*
 * Utility function that translates a jstring to a locale-specific
 * native C string.
 * See Liang, The Java Native Interface, p. 100
 */
char* JNU_GetStringNativeChars(JNIEnv* env, jstring jstr) {
	char* result = 0;
	jbyteArray bytes = 0;
	jthrowable ex = 0;

	if (env->EnsureLocalCapacity(2) < 0) {
		return 0;	/* exception thrown */
	}
	bytes = (jbyteArray)env->CallObjectMethod(jstr, MID_String_getBytes);
	ex = env->ExceptionOccurred();
	if (!ex) {
		jint len = env->GetArrayLength(bytes);
		result = new char[len + 1];
		if (result == 0) {
			throw_exception(env, "out of memory");
			env->DeleteLocalRef(bytes);
			return 0;
		}
		env->GetByteArrayRegion(bytes, 0, len, (jbyte *)result);
		result[len] = 0;
	}
	else {
		env->DeleteLocalRef(ex);
	}

	env->DeleteLocalRef(bytes);
	return result;
}

// =======================================================================
// -----------------  JNI interface methods  -----------------------------
// =======================================================================

/*
 * Class:     lpsolve_LpSolve
 * Method:    makeLp
 * Signature: (II)Llpsolve/LpSolve;
 */
JNIEXPORT jobject JNICALL Java_lpsolve_LpSolve_makeLp
  (JNIEnv *env, jclass cls, jint rows, jint columns)
{
	jobject result = NULL;
	jclass lpsolve_cls = NULL;

	lprec* lp = make_lp(rows, columns);
	if (lp == NULL) {
		throw_exception(env, "make_lp returned NULL");
		goto cleanup;
	}

	lpsolve_cls = env->FindClass("lpsolve/LpSolve");
	if (lpsolve_cls == NULL) {
		goto cleanup;	/* exception thrown */
	}

	result = env->NewObject(lpsolve_cls, MID_lpsolve_constr, reinterpret_cast<jlong>(lp));
	/* no error checking because we're terminating anyway */

cleanup:
	env->DeleteLocalRef(lpsolve_cls);
	return result;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    copyLp
 * Signature: ()Llpsolve/LpSolve;
 */
JNIEXPORT jobject JNICALL Java_lpsolve_LpSolve_copyLp
  (JNIEnv *env, jobject obj)
{
	jobject result = NULL;
	jclass lpsolve_cls = NULL;

	lprec* lp = get_lp(env, obj);
	lprec* newLp = copy_lp(lp);
	if (newLp == NULL) {
		throw_exception(env, "copy_lp returned NULL");
		goto cleanup;
	}

	lpsolve_cls = env->FindClass("lpsolve/LpSolve");
	if (lpsolve_cls == NULL) {
		goto cleanup;	/* exception thrown */
	}

	result = env->NewObject(lpsolve_cls, MID_lpsolve_constr, reinterpret_cast<jlong>(newLp));
	/* no error checking because we're terminating anyway */

cleanup:
	env->DeleteLocalRef(lpsolve_cls);
	return result;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    readLp
 * Signature: (Ljava/lang/String;ILjava/lang/String;)Llpsolve/LpSolve;
 */
JNIEXPORT jobject JNICALL Java_lpsolve_LpSolve_readLp
  (JNIEnv *env, jclass cls, jstring filename, jint verbose, jstring lpName)
{
	lprec* lp = NULL;
	jobject result = NULL;
	jclass lpsolve_cls = NULL;
	const char* c_filename = NULL;
	const char* c_lpName = NULL;

	if (filename != NULL) {
		c_filename = JNU_GetStringNativeChars(env, filename);
		if (c_filename == 0 || env->ExceptionCheck()) {
			goto cleanup;	/* exception thrown */
		}
	}
	if (lpName != NULL) {
		if ((c_lpName = env->GetStringUTFChars(lpName, NULL)) == NULL) {
			goto cleanup;	/* exception thrown */
		}
	}

	lp = read_LP((char *)c_filename, verbose, (char *)c_lpName);
	if (lp == NULL) {
		throw_exception(env, "read_LP returned NULL");
		goto cleanup;
	}

	lpsolve_cls = env->FindClass("lpsolve/LpSolve");
	if (lpsolve_cls == NULL) {
		goto cleanup;	/* exception thrown */
	}

	result = env->NewObject(lpsolve_cls, MID_lpsolve_constr, reinterpret_cast<jlong>(lp));
	/* no error checking because we're terminating anyway */

cleanup:
	env->DeleteLocalRef(lpsolve_cls);
	delete (char *)c_filename;
	env->ReleaseStringUTFChars(lpName, c_lpName);
	return result;
}


/*
 * Class:     lpsolve_LpSolve
 * Method:    readMps
 * Signature: (Ljava/lang/String;I)Llpsolve/LpSolve;
 */
JNIEXPORT jobject JNICALL Java_lpsolve_LpSolve_readMps
  (JNIEnv *env, jclass cls, jstring filename, jint options)
{
	lprec* lp = NULL;
	jobject result = NULL;
	jclass lpsolve_cls = NULL;
	const char* c_filename = NULL;

	if (filename != NULL) {
		c_filename = JNU_GetStringNativeChars(env, filename);
		if (c_filename == 0 || env->ExceptionCheck()) {
			goto cleanup;	/* exception thrown */
		}
	}

	lp = read_MPS((char *)c_filename, options);
	if (lp == NULL) {
		throw_exception(env, "read_MPS returned NULL");
		goto cleanup;
	}

	lpsolve_cls = env->FindClass("lpsolve/LpSolve");
	if (lpsolve_cls == NULL) {
		goto cleanup;	/* exception thrown */
	}

	result = env->NewObject(lpsolve_cls, MID_lpsolve_constr, reinterpret_cast<jlong>(lp));
	/* no error checking because we're terminating anyway */

cleanup:
	env->DeleteLocalRef(lpsolve_cls);
	delete (char *)c_filename;
	return result;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    readFreeMps
 * Signature: (Ljava/lang/String;I)Llpsolve/LpSolve;
 */
JNIEXPORT jobject JNICALL Java_lpsolve_LpSolve_readFreeMps
  (JNIEnv *env, jclass cls, jstring filename, jint options)
{
	lprec* lp = NULL;
	jobject result = NULL;
	jclass lpsolve_cls = NULL;
	const char* c_filename = NULL;

	if (filename != NULL) {
		c_filename = JNU_GetStringNativeChars(env, filename);
		if (c_filename == 0 || env->ExceptionCheck()) {
			goto cleanup;	/* exception thrown */
		}
	}

	lp = read_freeMPS((char *)c_filename, options);
	if (lp == NULL) {
		throw_exception(env, "read_freeMPS returned NULL");
		goto cleanup;
	}

	lpsolve_cls = env->FindClass("lpsolve/LpSolve");
	if (lpsolve_cls == NULL) {
		goto cleanup;	/* exception thrown */
	}

	result = env->NewObject(lpsolve_cls, MID_lpsolve_constr, reinterpret_cast<jlong>(lp));
	/* no error checking because we're terminating anyway */

cleanup:
	env->DeleteLocalRef(lpsolve_cls);
	delete (char *)c_filename;
	return result;
}


/*
 * Class:     lpsolve_LpSolve
 * Method:    readXLI
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)Llpsolve/LpSolve;
 */
JNIEXPORT jobject JNICALL Java_lpsolve_LpSolve_readXLI
  (JNIEnv *env, jclass cls, jstring xliname, jstring modelname, jstring dataname, jstring options, jint verbose)
{
	lprec* lp = NULL;
	jobject result = NULL;
	jclass lpsolve_cls = NULL;
	const char* c_xliname = NULL;
	const char* c_modelname = NULL;
	const char* c_dataname = NULL;
	const char* c_options = NULL;

	if (xliname != NULL) {
		if ((c_xliname = env->GetStringUTFChars(xliname, NULL)) == NULL) {
			goto cleanup;	/* exception thrown */
		}
	}

	if (modelname != NULL) {
		c_modelname = JNU_GetStringNativeChars(env, modelname);
		if (c_modelname == 0 || env->ExceptionCheck()) {
			goto cleanup;	/* exception thrown */
		}
	}

	if (dataname != NULL) {
		c_dataname = JNU_GetStringNativeChars(env, dataname);
		if (c_dataname == 0 || env->ExceptionCheck()) {
			goto cleanup;	/* exception thrown */
		}
	}

	if (options != NULL) {
		if ((c_options = env->GetStringUTFChars(options, NULL)) == NULL) {
			goto cleanup;	/* exception thrown */
		}
	}

	lp = read_XLI((char *)c_xliname, (char *)c_modelname, (char *)c_dataname, (char *)c_options, verbose);
	if (lp == NULL) {
		throw_exception(env, "read_XLI returned NULL");
		goto cleanup;
	}

	lpsolve_cls = env->FindClass("lpsolve/LpSolve");
	if (lpsolve_cls == NULL) {
		goto cleanup;	/* exception thrown */
	}

	result = env->NewObject(lpsolve_cls, MID_lpsolve_constr, reinterpret_cast<jlong>(lp));
	/* no error checking because we're terminating anyway */

cleanup:
	env->DeleteLocalRef(lpsolve_cls);
	env->ReleaseStringUTFChars(xliname, c_xliname);
	delete (char *)c_modelname;
	delete (char *)c_dataname;
	env->ReleaseStringUTFChars(options, c_options);
	return result;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    resizeLp
 * Signature:
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_resizeLp
  (JNIEnv *env, jobject obj, jint rows, jint columns)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = resize_lp(lp, rows, columns);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "resize_lp");
        }
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setLpName
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setLpName
  (JNIEnv *env, jobject obj, jstring name)
{
	const char* c_name = NULL;

	if (name != NULL) {
		if ((c_name = env->GetStringUTFChars(name, NULL)) == NULL) {
			return;	/* exception thrown */
		}
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_lp_name(lp, (char *)c_name);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_lp_name");
		/* fall through and cleanup */
	}

	env->ReleaseStringUTFChars(name, c_name);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getLpName
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_lpsolve_LpSolve_getLpName
  (JNIEnv *env, jobject obj)
{
	jstring name = NULL;
	lprec* lp = get_lp(env, obj);
	char *c_name = get_lp_name(lp);
	if (c_name != NULL) {
		name = env->NewStringUTF(c_name);
		/* no error checking because we're terminating anyway */
	}
	return name;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    addConstraint
 * Signature: ([DID)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_addConstraint
  (JNIEnv *env, jobject obj, jdoubleArray row, jint constrType, jdouble rh)
{
	jdouble* c_row = env->GetDoubleArrayElements(row, NULL);
	if (c_row == NULL) {
		return;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = add_constraint(lp, c_row, constrType, rh);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "add_constraint");
		/* fall through and cleanup */
	}

	env->ReleaseDoubleArrayElements(row, c_row, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    addConstraintex
 * Signature: (I[D[IID)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_addConstraintex
  (JNIEnv *env, jobject obj, jint count, jdoubleArray row, jintArray colno, jint constrType, jdouble rh)
{
	unsigned char rc;
	jdouble* c_row = NULL;
	jint* c_colno = NULL;

	lprec* lp = get_lp(env, obj);

	c_row = env->GetDoubleArrayElements(row, NULL);
	if (c_row == NULL) {
		goto cleanup;	/* exception thrown */
	}

	c_colno = env->GetIntArrayElements(colno, NULL);
	if (c_colno == NULL) {
		goto cleanup;	/* exception thrown */
	}

	rc = add_constraintex(lp, count, (REAL *)c_row, (int *)c_colno, constrType, rh);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "add_constraintex");
		/* fall through and cleanup */
	}

cleanup:
	env->ReleaseDoubleArrayElements(row, c_row, 0);
	env->ReleaseIntArrayElements(colno, c_colno, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    strAddConstraint
 * Signature: (Ljava/lang/String;ID)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_strAddConstraint
  (JNIEnv *env, jobject obj, jstring row, jint constrType, jdouble rh)
{
	const char* c_row = env->GetStringUTFChars(row, NULL);
	if (c_row == NULL) {
		return;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = str_add_constraint(lp, (char*)c_row, constrType, rh);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "str_add_constraint");
		/* fall through and cleanup */
	}

	env->ReleaseStringUTFChars(row, c_row);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    delConstraint
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_delConstraint
  (JNIEnv *env, jobject obj, jint rownr)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = del_constraint(lp, rownr);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "del_constraint");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isConstrType
 * Signature: (II)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isConstrType
  (JNIEnv *env, jobject obj, jint row, jint mask)
{
	lprec* lp = get_lp(env, obj);
	return ((is_constr_type(lp, row, mask) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    addLagCon
 * Signature: ([DID)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_addLagCon
  (JNIEnv *env, jobject obj, jdoubleArray row, jint constrType, jdouble rh)
{
	jdouble* c_row = env->GetDoubleArrayElements(row, NULL);
	if (c_row == NULL) {
		return;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = add_lag_con(lp, c_row, constrType, rh);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "add_lag_con");
		/* fall through and cleanup */
	}

	env->ReleaseDoubleArrayElements(row, c_row, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    strAddLagCon
 * Signature: (Ljava/lang/String;ID)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_strAddLagCon
  (JNIEnv *env, jobject obj, jstring row, jint constrType, jdouble rh)
{
	const char* c_row = env->GetStringUTFChars(row, NULL);
	if (c_row == NULL) {
		return;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = str_add_lag_con(lp, (char*)c_row, constrType, rh);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "str_add_lag_con");
		/* fall through and cleanup */
	}

	env->ReleaseStringUTFChars(row, c_row);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    addColumn
 * Signature: ([D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_addColumn
  (JNIEnv *env, jobject obj, jdoubleArray column)
{
	jdouble* c_column = env->GetDoubleArrayElements(column, NULL);
	if (c_column == NULL) {
		return;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = add_column(lp, c_column);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "add_column");
		/* fall through and cleanup */
	}

	env->ReleaseDoubleArrayElements(column, c_column, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    addColumnex
 * Signature: (I[D[I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_addColumnex
  (JNIEnv *env, jobject obj, jint count, jdoubleArray column, jintArray rowno)
{
	unsigned char rc;
	jdouble* c_column = NULL;
	jint* c_rowno = NULL;

	lprec* lp = get_lp(env, obj);

	c_column = env->GetDoubleArrayElements(column, NULL);
	if (c_column == NULL) {
		goto cleanup;	/* exception thrown */
	}

	c_rowno = env->GetIntArrayElements(rowno, NULL);
	if (c_rowno == NULL) {
		goto cleanup;	/* exception thrown */
	}

	rc = add_columnex(lp, count, c_column, (int *)c_rowno);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "add_columnex");
		/* fall through and cleanup */
	}

cleanup:
	env->ReleaseDoubleArrayElements(column, c_column, 0);
	env->ReleaseIntArrayElements(rowno, c_rowno, 0);
}


/*
 * Class:     lpsolve_LpSolve
 * Method:    strAddColumn
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_strAddColumn
  (JNIEnv *env, jobject obj, jstring column)
{
	const char* c_column = env->GetStringUTFChars(column, NULL);
	if (c_column == NULL) {
		return;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = str_add_column(lp, (char*)c_column);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "str_add_column");
		/* fall through and cleanup */
	}

	env->ReleaseStringUTFChars(column, c_column);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    delColumn
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_delColumn
  (JNIEnv *env, jobject obj, jint columnnr)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = del_column(lp, columnnr);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "del_column");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setRow
 * Signature: (I[D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setRow
  (JNIEnv *env, jobject obj, jint rowno, jdoubleArray row)
{
	jdouble* c_row = env->GetDoubleArrayElements(row, NULL);
	if (c_row == NULL) {
		return;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_row(lp, rowno, c_row);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_row");
		/* fall through and cleanup */
	}

	env->ReleaseDoubleArrayElements(row, c_row, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setRowex
 * Signature: (II[D[I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setRowex
  (JNIEnv *env, jobject obj, jint rowno, jint count, jdoubleArray row, jintArray colno)
{
	unsigned char rc;
	jdouble* c_row = NULL;
	jint* c_colno = NULL;

	lprec* lp = get_lp(env, obj);

	c_row = env->GetDoubleArrayElements(row, NULL);
	if (c_row == NULL) {
		goto cleanup;	/* exception thrown */
	}

	c_colno = env->GetIntArrayElements(colno, NULL);
	if (c_colno == NULL) {
		goto cleanup;	/* exception thrown */
	}

	rc = set_rowex(lp, rowno, count, c_row, (int *)c_colno);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_rowex");
		/* fall through and cleanup */
	}

cleanup:
	env->ReleaseDoubleArrayElements(row, c_row, 0);
	env->ReleaseIntArrayElements(colno, c_colno, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setColumn
 * Signature: (I[D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setColumn
  (JNIEnv *env, jobject obj, jint colno, jdoubleArray column)
{
	jdouble* c_column = env->GetDoubleArrayElements(column, NULL);
	if (c_column == NULL) {
		return;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_column(lp, colno, c_column);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_column");
		/* fall through and cleanup */
	}

	env->ReleaseDoubleArrayElements(column, c_column, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setColumnex
 * Signature: (II[D[I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setColumnex
  (JNIEnv *env, jobject obj, jint colno, jint count, jdoubleArray column, jintArray rowno)
{
	unsigned char rc;
	jdouble* c_column = NULL;
	jint* c_rowno = NULL;

	lprec* lp = get_lp(env, obj);

	c_column = env->GetDoubleArrayElements(column, NULL);
	if (c_column == NULL) {
		goto cleanup;	/* exception thrown */
	}

	c_rowno = env->GetIntArrayElements(rowno, NULL);
	if (c_rowno == NULL) {
		goto cleanup;	/* exception thrown */
	}

	rc = set_columnex(lp, colno, count, c_column, (int *)c_rowno);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_columnex");
		/* fall through and cleanup */
	}

cleanup:
	env->ReleaseDoubleArrayElements(column, c_column, 0);
	env->ReleaseIntArrayElements(rowno, c_rowno, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    columnInLp
 * Signature: ([D)I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_columnInLp
  (JNIEnv *env, jobject obj, jdoubleArray column)
{
	jdouble* c_column = env->GetDoubleArrayElements(column, NULL);
	if (c_column == NULL) {
		return FALSE;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	jint result = column_in_lp(lp, c_column);

	env->ReleaseDoubleArrayElements(column, c_column, 0);
	return result;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setRowName
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setRowName
  (JNIEnv *env, jobject obj, jint rownr, jstring name)
{
	const char* c_name = NULL;

	if (name != NULL) {
		if ((c_name = env->GetStringUTFChars(name, NULL)) == NULL) {
			return;	/* exception thrown */
		}
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_row_name(lp, rownr, (char *)c_name);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_row_name");
		/* fall through and cleanup */
	}

	env->ReleaseStringUTFChars(name, c_name);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getRowName
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_lpsolve_LpSolve_getRowName
  (JNIEnv *env, jobject obj, jint rownr)
{
	jstring name = NULL;
	lprec* lp = get_lp(env, obj);
	char *c_name = get_row_name(lp, rownr);
	if (c_name != NULL) {
		name = env->NewStringUTF(c_name);
		/* no error checking because we're terminating anyway */
	}
	return name;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getOrigrowName
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_lpsolve_LpSolve_getOrigrowName
  (JNIEnv *env, jobject obj, jint rownr)
{
	jstring name = NULL;
	lprec* lp = get_lp(env, obj);
	char *c_name = get_origrow_name(lp, rownr);
	if (c_name != NULL) {
		name = env->NewStringUTF(c_name);
		/* no error checking because we're terminating anyway */
	}
	return name;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setColName
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setColName
  (JNIEnv *env, jobject obj, jint colnr, jstring name)
{
	const char* c_name = NULL;

	if (name != NULL) {
		if ((c_name = env->GetStringUTFChars(name, NULL)) == NULL) {
			return;	/* exception thrown */
		}
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_col_name(lp, colnr, (char *)c_name);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_col_name");
		/* fall through and cleanup */
	}

	env->ReleaseStringUTFChars(name, c_name);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getColName
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_lpsolve_LpSolve_getColName
  (JNIEnv *env, jobject obj, jint colnr)
{
	jstring name = NULL;
	lprec* lp = get_lp(env, obj);
	char *c_name = get_col_name(lp, colnr);
	if (c_name != NULL) {
		name = env->NewStringUTF(c_name);
		/* no error checking because we're terminating anyway */
	}
	return name;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getOrigcolName
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_lpsolve_LpSolve_getOrigcolName
  (JNIEnv *env, jobject obj, jint colnr)
{
	jstring name = NULL;
	lprec* lp = get_lp(env, obj);
	char *c_name = get_origcol_name(lp, colnr);
	if (c_name != NULL) {
		name = env->NewStringUTF(c_name);
		/* no error checking because we're terminating anyway */
	}
	return name;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setRhVec
 * Signature: ([D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setRhVec
  (JNIEnv *env, jobject obj, jdoubleArray rh)
{
	jdouble* c_rh = env->GetDoubleArrayElements(rh, NULL);
	if (c_rh == NULL) {
		return;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	/* Strange enough, set_rh_vec has no return value */
	set_rh_vec(lp, c_rh);

	env->ReleaseDoubleArrayElements(rh, c_rh, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    strSetRhVec
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_strSetRhVec
  (JNIEnv *env, jobject obj, jstring rh)
{
	const char* c_rh = env->GetStringUTFChars(rh, NULL);
	if (c_rh == NULL) {
		return;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = str_set_rh_vec(lp, (char*)c_rh);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "str_set_rh_vec");
		/* fall through and cleanup */
	}

	env->ReleaseStringUTFChars(rh, c_rh);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setConstrType
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setConstrType
  (JNIEnv *env, jobject obj, jint rownr, jint constrType)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_constr_type(lp, rownr, constrType);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_constr_type");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getConstrType
 * Signature: (I)S
 */
JNIEXPORT jshort JNICALL Java_lpsolve_LpSolve_getConstrType
  (JNIEnv *env, jobject obj, jint rownr)
{
	lprec* lp = get_lp(env, obj);
	int constrType = get_constr_type(lp, rownr);
	if (constrType == -1) {
		throw_exception_with_status(env, lp, "get_constr_type");
	}
	return (jshort)constrType;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    addSOS
 * Signature: (Ljava/lang/String;III[I[D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_addSOS
  (JNIEnv *env, jobject obj, jstring name, jint sostype, jint priority, jint count, jintArray sosvars, jdoubleArray weights)
{
	int rc;
	const char* c_name = NULL;
	jint* c_sosvars = NULL;
	jdouble* c_weights = NULL;

	lprec* lp = get_lp(env, obj);

	if (name != NULL) {
		if ((c_name = env->GetStringUTFChars(name, NULL)) == NULL) {
			goto cleanup;	/* exception thrown */
		}
	}
	c_sosvars = env->GetIntArrayElements(sosvars, NULL);
	if (c_sosvars == NULL) {
		goto cleanup;	/* exception thrown */
	}
	c_weights = env->GetDoubleArrayElements(weights, NULL);
	if (c_weights == NULL) {
		goto cleanup;	/* exception thrown */
	}

	rc = add_SOS(lp, (char *)c_name, (short)sostype, priority, count, (int *)c_sosvars, c_weights);
	if (rc == 0) {
		throw_exception_with_status(env, lp, "add_SOS");
		/* fall through and cleanup */
	}

cleanup:
	env->ReleaseStringUTFChars(name, c_name);
	env->ReleaseIntArrayElements(sosvars, c_sosvars, 0);
	env->ReleaseDoubleArrayElements(weights, c_weights, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isSOSVar
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isSOSVar
  (JNIEnv *env, jobject obj, jint colnr)
{
	lprec* lp = get_lp(env, obj);
	return ((is_SOS_var(lp, colnr) == TRUE) ? JNI_TRUE : JNI_FALSE);
}


/*
 * Class:     lpsolve_LpSolve
 * Method:    setObjFn
 * Signature: ([D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setObjFn
  (JNIEnv *env, jobject obj, jdoubleArray row)
{
	jdouble* c_row = env->GetDoubleArrayElements(row, NULL);
	if (c_row == NULL) {
		return;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_obj_fn(lp, c_row);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_obj_fn");
		/* fall through and cleanup */
	}

	env->ReleaseDoubleArrayElements(row, c_row, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    strSetObjFn
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_strSetObjFn
  (JNIEnv *env, jobject obj, jstring row)
{
	const char* c_row = env->GetStringUTFChars(row, NULL);
	if (c_row == NULL) {
		return;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = str_set_obj_fn(lp, (char*)c_row);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "str_set_obj_fn");
		/* fall through and cleanup */
	}

	env->ReleaseStringUTFChars(row, c_row);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setObjFnex
 * Signature: (I[D[I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setObjFnex
  (JNIEnv *env, jobject obj, jint count, jdoubleArray row, jintArray colno)
{
	unsigned char rc;
	jdouble* c_row = NULL;
	jint* c_colno = NULL;

	lprec* lp = get_lp(env, obj);

	c_row = env->GetDoubleArrayElements(row, NULL);
	if (c_row == NULL) {
		goto cleanup;	/* exception thrown */
	}

	c_colno = env->GetIntArrayElements(colno, NULL);
	if (c_colno == NULL) {
		goto cleanup;	/* exception thrown */
	}

	rc = set_obj_fnex(lp, count, c_row, (int *)c_colno);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_obj_fnex");
		/* fall through and cleanup */
	}

cleanup:
	env->ReleaseDoubleArrayElements(row, c_row, 0);
	env->ReleaseIntArrayElements(colno, c_colno, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setObj
 * Signature: (ID)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setObj
  (JNIEnv *env, jobject obj, jint column, jdouble value)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_obj(lp, column, value);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_obj");
	}
}


/*
 * Class:     lpsolve_LpSolve
 * Method:    setMat
 * Signature: (IID)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setMat
  (JNIEnv *env, jobject obj, jint row, jint column, jdouble value)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_mat(lp, row, column, value);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_mat");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getMat
 * Signature: (II)D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getMat
  (JNIEnv *env, jobject obj, jint row, jint column)
{
	lprec* lp = get_lp(env, obj);
	return get_mat(lp, row, column);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getRow
 * Signature: (I[D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_getRow
  (JNIEnv *env, jobject obj, jint rownr, jdoubleArray row)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc;
	int size = (1 + get_Ncolumns(lp));
	double* c_row = new double[size];
	if (c_row == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}

	/* We do not use the _ptr_variants because we want to control memory allocation */
	rc = get_row(lp, rownr, c_row);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_row");
		goto cleanup;
	}

	if (env->GetArrayLength(row) < size) {
		throw_exception(env, "Target array is too short to hold values");
		goto cleanup;
	}

	env->SetDoubleArrayRegion(row, 0, size, c_row);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_row;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getPtrRow
 * Signature: (I)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_lpsolve_LpSolve_getPtrRow
  (JNIEnv *env, jobject obj, jint rownr)
{
	unsigned char rc;
	jdoubleArray row = NULL;

	lprec* lp = get_lp(env, obj);
	int size = (1 + get_Ncolumns(lp));
	double* c_row = new double[size];
	if (c_row == NULL) {
		throw_exception(env, "getRow: cannot allocate memory");
		goto cleanup;
	}

	rc = get_row(lp, rownr, c_row);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_row");
		goto cleanup;
	}

	row = env->NewDoubleArray(size);
	if (row == NULL) {
		goto cleanup;	/* exception thrown */
	}

	env->SetDoubleArrayRegion(row, 0, size, c_row);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_row;
	return row;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getRowex
 * Signature: (I[D[I)I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getRowex
  (JNIEnv *env, jobject obj, jint rownr, jdoubleArray row, jintArray nzcols)
{
	int nzcount;
	jdouble* c_row = NULL;
	jint* c_nzcols = NULL;
	lprec* lp;

	c_row = env->GetDoubleArrayElements(row, NULL);
	if (c_row == NULL) {
		goto cleanup;	/* exception thrown */
	}

	c_nzcols = env->GetIntArrayElements(nzcols, NULL);
	if (c_nzcols == NULL) {
		goto cleanup;	/* exception thrown */
	}

	lp = get_lp(env, obj);
	nzcount = get_rowex(lp, rownr, c_row, (int *)c_nzcols);
	if (nzcount == -1) {
		throw_exception_with_status(env, lp, "get_rowex");
		goto cleanup;
	}

cleanup:
	env->ReleaseDoubleArrayElements(row, c_row, 0);
	env->ReleaseIntArrayElements(nzcols, c_nzcols, 0);

	return nzcount;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getColumn
 * Signature: (I[D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_getColumn
  (JNIEnv *env, jobject obj, jint columnnr, jdoubleArray column)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc;
	int size = (1 + get_Nrows(lp));
	double* c_column = new double[size];
	if (c_column == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}

	/* We do not use the _ptr_variants because we want to control memory allocation */
	rc = get_column(lp, columnnr, c_column);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_column");
		goto cleanup;
	}

	if (env->GetArrayLength(column) < size) {
		throw_exception(env, "Target array is too short to hold values");
		goto cleanup;
	}

	env->SetDoubleArrayRegion(column, 0, size, c_column);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_column;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getPtrColumn
 * Signature: (I)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_lpsolve_LpSolve_getPtrColumn
  (JNIEnv *env, jobject obj, jint columnnr)
{
	unsigned char rc;
	jdoubleArray column = NULL;

	lprec* lp = get_lp(env, obj);
	int size = (1 + get_Nrows(lp));
	double* c_column = new double[size];
	if (c_column == NULL) {
		throw_exception(env, "getColumn: cannot allocate memory");
		goto cleanup;
	}

	rc = get_column(lp, columnnr, c_column);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_column");
		goto cleanup;
	}

	column = env->NewDoubleArray(size);
	if (column == NULL) {
		goto cleanup;	/* exception thrown */
	}

	env->SetDoubleArrayRegion(column, 0, size, c_column);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_column;
	return column;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getColumnex
 * Signature: (I[D[I)I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getColumnex
  (JNIEnv *env, jobject obj, jint colnr, jdoubleArray column, jintArray nzrows)
{
	int nzcount;
	jdouble* c_column = NULL;
	jint* c_nzrows = NULL;
	lprec* lp;

	c_column = env->GetDoubleArrayElements(column, NULL);
	if (c_column == NULL) {
		goto cleanup;	/* exception thrown */
	}

	c_nzrows = env->GetIntArrayElements(nzrows, NULL);
	if (c_nzrows == NULL) {
		goto cleanup;	/* exception thrown */
	}

	lp = get_lp(env, obj);
	nzcount = get_columnex(lp, colnr, c_column, (int *)c_nzrows);
	if (nzcount == -1) {
		throw_exception_with_status(env, lp, "get_columnex");
		goto cleanup;
	}

cleanup:
	env->ReleaseDoubleArrayElements(column, c_column, 0);
	env->ReleaseIntArrayElements(nzrows, c_nzrows, 0);

	return nzcount;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setMaxim
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setMaxim
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	set_maxim(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setMinim
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setMinim
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	set_minim(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setSense
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setSense
  (JNIEnv *env, jobject obj, jboolean maximize)
{
	lprec* lp = get_lp(env, obj);
	set_sense(lp, (maximize == JNI_TRUE) ? TRUE : FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isMaxim
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isMaxim
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return ((is_maxim(lp) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setLowbo
 * Signature: (ID)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setLowbo
  (JNIEnv *env, jobject obj, jint colnr, jdouble value)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_lowbo(lp, colnr, value);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_lowbo");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getLowbo
 * Signature: (I)D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getLowbo
  (JNIEnv *env, jobject obj, jint colnr)
{
	lprec* lp = get_lp(env, obj);
	return get_lowbo(lp, colnr);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setUpbo
 * Signature: (ID)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setUpbo
  (JNIEnv *env, jobject obj, jint colnr, jdouble value)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_upbo(lp, colnr, value);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_upbo");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getUpbo
 * Signature: (I)D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getUpbo
  (JNIEnv *env, jobject obj, jint colnr)
{
	lprec* lp = get_lp(env, obj);
	return get_upbo(lp, colnr);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setUnbounded
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setUnbounded
  (JNIEnv *env, jobject obj, jint colnr)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_unbounded(lp, colnr);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_unbounded");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isUnbounded
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isUnbounded
  (JNIEnv *env, jobject obj, jint colnr)
{
	lprec* lp = get_lp(env, obj);
	return ((is_unbounded(lp, colnr) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isNegative
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isNegative
  (JNIEnv *env, jobject obj, jint colnr)
{
	lprec* lp = get_lp(env, obj);
	return ((is_negative(lp, colnr) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setBounds
 * Signature: (IDD)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setBounds
  (JNIEnv *env, jobject obj, jint colnr, jdouble lower, jdouble upper)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_bounds(lp, colnr, lower, upper);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_bounds");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setBoundsTighter
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setBoundsTighter
  (JNIEnv *env, jobject obj, jboolean tighten)
{
	lprec* lp = get_lp(env, obj);
	set_bounds_tighter(lp, (tighten == JNI_TRUE) ? TRUE : FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getBoundsTighter
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_getBoundsTighter
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return ((get_bounds_tighter(lp) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setRhRange
 * Signature: (ID)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setRhRange
  (JNIEnv *env, jobject obj, jint rownr, jdouble range)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_rh_range(lp, rownr, range);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_rh_range");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getRhRange
 * Signature: (I)D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getRhRange
  (JNIEnv *env, jobject obj, jint rownr)
{
	lprec* lp = get_lp(env, obj);
	return get_rh_range(lp, rownr);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setInt
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setInt
  (JNIEnv *env, jobject obj, jint colnr, jboolean mustBeInteger)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_int(lp, colnr, (mustBeInteger == JNI_TRUE) ? TRUE : FALSE);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_int");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isInt
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isInt
  (JNIEnv *env, jobject obj, jint colnr)
{
	lprec* lp = get_lp(env, obj);
	return ((is_int(lp, colnr) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setBinary
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setBinary
  (JNIEnv *env, jobject obj, jint colnr, jboolean mustBeBin)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_binary(lp, colnr, (mustBeBin == JNI_TRUE) ? TRUE : FALSE);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_binary");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isBinary
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isBinary
  (JNIEnv *env, jobject obj, jint colnr)
{
	lprec* lp = get_lp(env, obj);
	return ((is_binary(lp, colnr) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setSemicont
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setSemicont
  (JNIEnv *env, jobject obj, jint colnr, jboolean mustBeSc)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_semicont(lp, colnr, (mustBeSc == JNI_TRUE) ? TRUE : FALSE);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_semicont");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isSemicont
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isSemicont
  (JNIEnv *env, jobject obj, jint colnr)
{
	lprec* lp = get_lp(env, obj);
	return ((is_semicont(lp, colnr) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setInfinite
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setInfinite
  (JNIEnv *env, jobject obj, jdouble value)
{
	lprec* lp = get_lp(env, obj);
	set_infinite(lp, value);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getInfinite
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getInfinite
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_infinite(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isInfinite
 * Signature: (D)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isInfinite
  (JNIEnv *env, jobject obj, jdouble value)
{
	lprec* lp = get_lp(env, obj);
	return ((is_infinite(lp, value) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setEpsint
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setEpsint
  (JNIEnv *env, jobject obj, jdouble value)
{
	lprec* lp = get_lp(env, obj);
	set_epsint(lp, value);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getEpsint
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getEpsint
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_epsint(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setEpsb
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setEpsb
  (JNIEnv *env, jobject obj, jdouble value)
{
	lprec* lp = get_lp(env, obj);
	set_epsb(lp, value);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getEpsb
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getEpsb
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_epsb(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setEpsd
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setEpsd
  (JNIEnv *env, jobject obj, jdouble value)
{
	lprec* lp = get_lp(env, obj);
	set_epsd(lp, value);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getEpsd
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getEpsd
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_epsd(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setEpsel
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setEpsel
  (JNIEnv *env, jobject obj, jdouble value)
{
	lprec* lp = get_lp(env, obj);
	set_epsel(lp, value);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getEpsel
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getEpsel
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_epsel(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setEpspivot
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setEpspivot
  (JNIEnv *env, jobject obj, jdouble value)
{
	lprec* lp = get_lp(env, obj);
	set_epspivot(lp, value);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getEpspivot
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getEpspivot
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_epspivot(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setEpsperturb
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setEpsperturb
  (JNIEnv *env, jobject obj, jdouble value)
{
	lprec* lp = get_lp(env, obj);
	set_epsperturb(lp, value);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getEpsperturb
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getEpsperturb
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_epsperturb(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setEpslevel
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setEpslevel
  (JNIEnv *env, jobject obj, jint epslevel)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_epslevel(lp, epslevel);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_epslevel");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getStatus
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getStatus
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_status(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setMipGap
 * Signature: (ZD)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setMipGap
  (JNIEnv *env, jobject obj, jboolean absolute, jdouble value)
{
	lprec* lp = get_lp(env, obj);
	set_mip_gap(lp, (absolute == JNI_TRUE) ? TRUE : FALSE, value);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getMipGap
 * Signature: (Z)D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getMipGap
  (JNIEnv *env, jobject obj, jboolean absolute)
{
	lprec* lp = get_lp(env, obj);
	return get_mip_gap(lp, (absolute == JNI_TRUE) ? TRUE : FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setRh
 * Signature: (ID)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setRh
  (JNIEnv *env, jobject obj, jint row, jdouble value)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_rh(lp, row, value);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_rh");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getRh
 * Signature: (I)D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getRh
  (JNIEnv *env, jobject obj, jint row)
{
	lprec* lp = get_lp(env, obj);
	return get_rh(lp, row);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setVerbose
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setVerbose
  (JNIEnv *env, jobject obj, jint verbose)
{
	lprec* lp = get_lp(env, obj);
	set_verbose(lp, verbose);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getVerbose
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getVerbose
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_verbose(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setTimeout
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setTimeout
  (JNIEnv *env, jobject obj, jlong sectimeout)
{
	lprec* lp = get_lp(env, obj);
	set_timeout(lp, (long)sectimeout);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getTimeout
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_lpsolve_LpSolve_getTimeout
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_timeout(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    timeElapsed
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_timeElapsed
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return time_elapsed(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setPrintSol
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setPrintSol
  (JNIEnv *env, jobject obj, jint printSol)
{
	lprec* lp = get_lp(env, obj);
	set_print_sol(lp, printSol);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getPrintSol
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getPrintSol
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_print_sol(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setDebug
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setDebug
  (JNIEnv *env, jobject obj, jboolean debug)
{
	lprec* lp = get_lp(env, obj);
	set_debug(lp, (debug == JNI_TRUE) ? TRUE : FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isDebug
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isDebug
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return ((is_debug(lp) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setTrace
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setTrace
  (JNIEnv *env, jobject obj, jboolean trace)
{
	lprec* lp = get_lp(env, obj);
	set_trace(lp, (trace == JNI_TRUE) ? TRUE : FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isTrace
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isTrace
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return ((is_trace(lp) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setLagTrace
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setLagTrace
  (JNIEnv *env, jobject obj, jboolean lagTrace)
{
	lprec* lp = get_lp(env, obj);
	set_lag_trace(lp, (lagTrace == JNI_TRUE) ? TRUE : FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isLagTrace
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isLagTrace
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return ((is_lag_trace(lp) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setAddRowmode
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_setAddRowmode
  (JNIEnv *env, jobject obj, jboolean turnon)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_add_rowmode(lp, (turnon == JNI_TRUE) ? TRUE : FALSE);
	return (rc == TRUE) ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isAddRowmode
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isAddRowmode
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return ((is_add_rowmode(lp) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setAntiDegen
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setAntiDegen
  (JNIEnv *env, jobject obj, jint antiDegen)
{
	lprec* lp = get_lp(env, obj);
	set_anti_degen(lp, antiDegen);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isAntiDegen
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isAntiDegen
  (JNIEnv *env, jobject obj, jint testmask)
{
	lprec* lp = get_lp(env, obj);
	return ((is_anti_degen(lp, testmask) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getAntiDegen
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getAntiDegen
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_anti_degen(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setPresolve
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setPresolve
  (JNIEnv *env, jobject obj, jint doPresolve, jint maxloops)
{
	lprec* lp = get_lp(env, obj);
	set_presolve(lp, doPresolve, maxloops);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isPresolve
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isPresolve
  (JNIEnv *env, jobject obj, jint testmask)
{
	lprec* lp = get_lp(env, obj);
	return ((is_presolve(lp, testmask) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getPresolve
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getPresolve
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_presolve(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getPresolveloops
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getPresolveloops
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_presolveloops(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setMaxpivot
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setMaxpivot
  (JNIEnv *env, jobject obj, jint maxNumInv)
{
	lprec* lp = get_lp(env, obj);
	set_maxpivot(lp, maxNumInv);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getMaxpivot
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getMaxpivot
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_maxpivot(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setBbRule
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setBbRule
  (JNIEnv *env, jobject obj, jint bbRule)
{
	lprec* lp = get_lp(env, obj);
	set_bb_rule(lp, bbRule);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getBbRule
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getBbRule
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_bb_rule(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setBbDepthlimit
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setBbDepthlimit
  (JNIEnv *env, jobject obj, jint bbMaxlevel)
{
	lprec* lp = get_lp(env, obj);
	set_bb_depthlimit(lp, bbMaxlevel);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getBbDepthlimit
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getBbDepthlimit
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_bb_depthlimit(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getSolutioncount
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getSolutioncount
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_solutioncount(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setSolutionlimit
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setSolutionlimit
  (JNIEnv *env, jobject obj, jint limit)
{
	lprec* lp = get_lp(env, obj);
	set_solutionlimit(lp, limit);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getSolutionlimit
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getSolutionlimit
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_solutionlimit(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setObjBound
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setObjBound
  (JNIEnv *env, jobject obj, jdouble objBound)
{
	lprec* lp = get_lp(env, obj);
	set_obj_bound(lp, objBound);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getObjBound
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getObjBound
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_obj_bound(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setBbFloorfirst
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setBbFloorfirst
  (JNIEnv *env, jobject obj, jint floorFirst)
{
	lprec* lp = get_lp(env, obj);
	set_bb_floorfirst(lp, floorFirst);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getBbFloorfirst
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getBbFloorfirst
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_bb_floorfirst(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setVarBranch
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setVarBranch
  (JNIEnv *env, jobject obj, jint colnr, jint branchMode)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_var_branch(lp, colnr, branchMode);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_var_branch");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getVarBranch
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getVarBranch
  (JNIEnv *env, jobject obj, jint colnr)
{
	lprec* lp = get_lp(env, obj);
	return get_var_branch(lp, colnr);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setVarWeights
 * Signature: ([D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setVarWeights
  (JNIEnv *env, jobject obj, jdoubleArray weights)
{
	jdouble* c_weights = env->GetDoubleArrayElements(weights, NULL);
	if (c_weights == NULL) {
		return;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_var_weights(lp, c_weights);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_var_weights");
		/* fall through and cleanup */
	}

	env->ReleaseDoubleArrayElements(weights, c_weights, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getVarPriority
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getVarPriority
  (JNIEnv *env, jobject obj, jint colnr)
{
	lprec* lp = get_lp(env, obj);
	return get_var_priority(lp, colnr);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setBreakAtFirst
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setBreakAtFirst
  (JNIEnv *env, jobject obj, jboolean breakAtFirst)
{
	lprec* lp = get_lp(env, obj);
	set_break_at_first(lp, (breakAtFirst == JNI_TRUE) ? TRUE : FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isBreakAtFirst
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isBreakAtFirst
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return ((is_break_at_first(lp) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setBreakAtValue
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setBreakAtValue
  (JNIEnv *env, jobject obj, jdouble breakAtValue)
{
	lprec* lp = get_lp(env, obj);
	set_break_at_value(lp, breakAtValue);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getBreakAtValue
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getBreakAtValue
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_break_at_value(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setScaling
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setScaling
  (JNIEnv *env, jobject obj, jint scalemode)
{
	lprec* lp = get_lp(env, obj);
	set_scaling(lp, scalemode);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getScaling
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getScaling
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_scaling(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isScalemode
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isScalemode
  (JNIEnv *env, jobject obj, jint testmask)
{
	lprec* lp = get_lp(env, obj);
	return ((is_scalemode(lp, testmask) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isScaletype
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isScaletype
  (JNIEnv *env, jobject obj, jint scaletype)
{
	lprec* lp = get_lp(env, obj);
	return ((is_scaletype(lp, scaletype) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isIntegerscaling
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isIntegerscaling
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return ((is_integerscaling(lp) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setScalelimit
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setScalelimit
  (JNIEnv *env, jobject obj, jdouble scalelimit)
{
	lprec* lp = get_lp(env, obj);
	set_scalelimit(lp, scalelimit);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getScalelimit
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getScalelimit
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_scalelimit(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setImprove
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setImprove
  (JNIEnv *env, jobject obj, jint improve)
{
	lprec* lp = get_lp(env, obj);
	set_improve(lp, improve);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getImprove
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getImprove
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_improve(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setPivoting
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setPivoting
  (JNIEnv *env, jobject obj, jint pivRule)
{
	lprec* lp = get_lp(env, obj);
	set_pivoting(lp, pivRule);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getPivoting
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getPivoting
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_pivoting(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isPivMode
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isPivMode
  (JNIEnv *env, jobject obj, jint testmask)
{
	lprec* lp = get_lp(env, obj);
	return ((is_piv_mode(lp, testmask) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isPivRule
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isPivRule
  (JNIEnv *env, jobject obj, jint rule)
{
	lprec* lp = get_lp(env, obj);
	return ((is_piv_rule(lp, rule) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setPreferdual
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setPreferdual
  (JNIEnv *env, jobject obj, jboolean dodual)
{
	lprec* lp = get_lp(env, obj);
	set_preferdual(lp, (dodual == JNI_TRUE) ? TRUE : FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setSimplextype
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setSimplextype
  (JNIEnv *env, jobject obj, jint simplextype)
{
	lprec* lp = get_lp(env, obj);
	set_simplextype(lp, simplextype);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getSimplextype
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getSimplextype
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_simplextype(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setNegrange
 * Signature: (D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setNegrange
  (JNIEnv *env, jobject obj, jdouble negrange)
{
	lprec* lp = get_lp(env, obj);
	set_negrange(lp, negrange);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getNegrange
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getNegrange
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_negrange(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getTotalIter
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_lpsolve_LpSolve_getTotalIter
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_total_iter(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getMaxLevel
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getMaxLevel
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_max_level(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getTotalNodes
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_lpsolve_LpSolve_getTotalNodes
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_total_nodes(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    solve
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_solve
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return solve(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getStatustext
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_lpsolve_LpSolve_getStatustext
  (JNIEnv *env, jobject obj, jint statuscode)
{
	jstring name = NULL;
	lprec* lp = get_lp(env, obj);
	char *c_name = get_statustext(lp, statuscode);
	if (c_name != NULL) {
		name = env->NewStringUTF(c_name);
		/* no error checking because we're terminating anyway */
	}
	return name;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    lagSolve
 * Signature: (DI)I
 */
/*
 * TODO: According to Peter Notebaert, this method is temporarily unavailable
 * but might be added again in later versions.
 *
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_lagSolve
  (JNIEnv *env, jobject obj, jdouble startBound, jint numIter)
{
	lprec* lp = get_lp(env, obj);
	return lag_solve(lp, startBound, numIter);
}
*/

/*
 * Class:     lpsolve_LpSolve
 * Method:    isFeasible
 * Signature: ([DD)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isFeasible
  (JNIEnv *env, jobject obj, jdoubleArray values, jdouble threshold)
{
	jdouble* c_values = env->GetDoubleArrayElements(values, NULL);
	if (c_values == NULL) {
		return JNI_FALSE;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = is_feasible(lp, c_values, threshold);
	jboolean result =  ((rc == TRUE) ? JNI_TRUE : JNI_FALSE);

	env->ReleaseDoubleArrayElements(values, c_values, 0);
	return result;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getObjective
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getObjective
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_objective(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getWorkingObjective
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getWorkingObjective
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_working_objective(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getVariables
 * Signature: ([D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_getVariables
  (JNIEnv *env, jobject obj, jdoubleArray var)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc;
	int size = get_Ncolumns(lp);
	double* c_var = new double[size];
	if (c_var == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}

	/* We do not use the _ptr_variants because we want to control memory allocation */
	rc = get_variables(lp, c_var);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_variables");
		goto cleanup;
	}

	if (env->GetArrayLength(var) < size) {
		throw_exception(env, "Target array is too short to hold values");
		goto cleanup;
	}

	env->SetDoubleArrayRegion(var, 0, size, c_var);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_var;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getPtrVariables
 * Signature: ()[D
 */
JNIEXPORT jdoubleArray JNICALL Java_lpsolve_LpSolve_getPtrVariables
  (JNIEnv *env, jobject obj)
{
	unsigned char rc;
	jdoubleArray var = NULL;

	lprec* lp = get_lp(env, obj);
	int size = get_Ncolumns(lp);
	double* c_var = new double[size];
	if (c_var == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}

	rc = get_variables(lp, c_var);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_variables");
		goto cleanup;
	}

	var = env->NewDoubleArray(size);
	if (var == NULL) {
		goto cleanup;	/* exception thrown */
	}

	env->SetDoubleArrayRegion(var, 0, size, c_var);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_var;
	return var;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getConstraints
 * Signature: ([D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_getConstraints
  (JNIEnv *env, jobject obj, jdoubleArray var)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc;
	int size = get_Nrows(lp);
	double* c_var = new double[size];
	if (c_var == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}

	/* We do not use the _ptr_variants because we want to control memory allocation */
	rc = get_constraints(lp, c_var);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_constraints");
		goto cleanup;
	}

	if (env->GetArrayLength(var) < size) {
		throw_exception(env, "Target array is too short to hold values");
		goto cleanup;
	}

	env->SetDoubleArrayRegion(var, 0, size, c_var);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_var;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getPtrConstraints
 * Signature: ()[D
 */
JNIEXPORT jdoubleArray JNICALL Java_lpsolve_LpSolve_getPtrConstraints
  (JNIEnv *env, jobject obj)
{
	unsigned char rc;
	jdoubleArray var = NULL;

	lprec* lp = get_lp(env, obj);
	int size = get_Nrows(lp);
	double* c_var = new double[size];
	if (c_var == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}

	rc = get_constraints(lp, c_var);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_constraints");
		goto cleanup;
	}

	var = env->NewDoubleArray(size);
	if (var == NULL) {
		goto cleanup;	/* exception thrown */
	}

	env->SetDoubleArrayRegion(var, 0, size, c_var);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_var;
	return var;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getPrimalSolution
 * Signature: ([D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_getPrimalSolution
  (JNIEnv *env, jobject obj, jdoubleArray pv)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc;
	int nRows = get_Nrows(lp);
	int nCols = get_Ncolumns(lp);
	int size = 1 + nRows + nCols;
	double* c_pv = new double[size];
	if (c_pv == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}

	/* We do not use the _ptr_variants because we want to control memory allocation */
	rc = get_primal_solution(lp, c_pv);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_primal_solution");
		goto cleanup;
	}

	if (env->GetArrayLength(pv) < size) {
		throw_exception(env, "Target array is too short to hold values");
		goto cleanup;
	}

	env->SetDoubleArrayRegion(pv, 0, size, c_pv);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_pv;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getPtrPrimalSolution
 * Signature: ()[D
 */
JNIEXPORT jdoubleArray JNICALL Java_lpsolve_LpSolve_getPtrPrimalSolution
  (JNIEnv *env, jobject obj)
{
	unsigned char rc;
	jdoubleArray solution = NULL;

	lprec* lp = get_lp(env, obj);
	int nRows = get_Nrows(lp);
	int nCols = get_Ncolumns(lp);
	int size = 1 + nRows + nCols;
	double* c_solution = new double[size];
	if (c_solution == NULL) {
		throw_exception(env, "getPrimalSolution: cannot allocate memory");
		goto cleanup;
	}

	rc = get_primal_solution(lp, c_solution);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_primal_solution");
		goto cleanup;
	}

	solution = env->NewDoubleArray(size);
	if (solution == NULL) {
		goto cleanup;	/* exception thrown */
	}

	env->SetDoubleArrayRegion(solution, 0, size, c_solution);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_solution;
	return solution;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getVarPrimalresult
 * Signature: (I)D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getVarPrimalresult
  (JNIEnv *env, jobject obj, jint index)
{
	lprec* lp = get_lp(env, obj);
	return get_var_primalresult(lp, index);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getSensitivityRhs
 * Signature: ([D[D[D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_getSensitivityRhs
  (JNIEnv *env, jobject obj, jdoubleArray duals, jdoubleArray dualsfrom, jdoubleArray dualstill)
{
	unsigned char rc;
	double *c_duals = NULL, *c_dualsfrom = NULL, *c_dualstill = NULL;
	lprec* lp = get_lp(env, obj);
	int size = get_Nrows(lp) + get_Ncolumns(lp);
	if ((c_duals = new double[size]) == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}
	if ((c_dualsfrom = new double[size]) == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}
	if ((c_dualstill = new double[size]) == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}

	/* We do not use the _ptr_variants because we want to control memory allocation */
	rc = get_sensitivity_rhs(lp, c_duals, c_dualsfrom, c_dualstill);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_sensitivity_rhs");
		goto cleanup;
	}

	if (env->GetArrayLength(duals) < size || env->GetArrayLength(dualsfrom) < size || env->GetArrayLength(dualstill) < size) {
		throw_exception(env, "Target array is too short to hold values");
		goto cleanup;
	}

	env->SetDoubleArrayRegion(duals, 0, size, c_duals);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	env->SetDoubleArrayRegion(dualsfrom, 0, size, c_dualsfrom);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	env->SetDoubleArrayRegion(dualstill, 0, size, c_dualstill);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_duals, c_dualsfrom, c_dualstill;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getPtrSensitivityRhs
 * Signature: ()[[D
 */
JNIEXPORT jobjectArray JNICALL Java_lpsolve_LpSolve_getPtrSensitivityRhs
  (JNIEnv *env, jobject obj)
{
	// See also Liang, p. 39 for an example with two-dimensional arrays.
	unsigned char rc;
	jobjectArray result = NULL;
	jdoubleArray duals = NULL, dualsfrom = NULL, dualstill = NULL;
	double *c_duals = NULL, *c_dualsfrom = NULL, *c_dualstill = NULL;

	jclass doubleArr_cls = env->FindClass("[D");
	if (doubleArr_cls == NULL) {
		return NULL;	/* exception thrown */
	}

	result = env->NewObjectArray(3, doubleArr_cls, NULL);
	if (result == NULL) {
		return NULL;	/* out of memory error thrown */
	}

	// Now allocate the arrays for the lp_solve method
	lprec* lp = get_lp(env, obj);
	int size = get_Nrows(lp) + get_Ncolumns(lp);

	if ((duals = env->NewDoubleArray(size)) == NULL) {
		goto cleanup;	/* exception thrown */
	}
	if ((dualsfrom = env->NewDoubleArray(size)) == NULL) {
		goto cleanup;	/* exception thrown */
	}
	if ((dualstill = env->NewDoubleArray(size)) == NULL) {
		goto cleanup;	/* exception thrown */
	}
	if ((c_duals = new double[size]) == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}
	if ((c_dualsfrom = new double[size]) == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}
	if ((c_dualstill = new double[size]) == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}

	/* We do not use the _ptr_variants because we want to control memory allocation */
	rc = get_sensitivity_rhs(lp, c_duals, c_dualsfrom, c_dualstill);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_sensitivity_rhs");
		goto cleanup;
	}

	env->SetDoubleArrayRegion(duals, 0, size, c_duals);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	env->SetDoubleArrayRegion(dualsfrom, 0, size, c_dualsfrom);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	env->SetDoubleArrayRegion(dualstill, 0, size, c_dualstill);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}

	// Set the lements of the resulting array
	env->SetObjectArrayElement(result, 0, duals);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	env->SetObjectArrayElement(result, 1, dualsfrom);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	env->SetObjectArrayElement(result, 2, dualstill);
	/* no exception check; we're terminating anyway */


cleanup:
	delete c_duals, c_dualsfrom, c_dualstill;
	return result;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getDualSolution
 * Signature: ([D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_getDualSolution
  (JNIEnv *env, jobject obj, jdoubleArray duals)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc;
	int nRows = get_Nrows(lp);
	int nCols = get_Ncolumns(lp);
	int size = 1 + nRows + nCols;
	double* c_duals = new double[size];
	if (c_duals == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}

	/* We do not use the _ptr_variants because we want to control memory allocation */
	rc = get_dual_solution(lp, c_duals);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_dual_solution");
		goto cleanup;
	}

	if (env->GetArrayLength(duals) < size) {
		throw_exception(env, "Target array is too short to hold values");
		goto cleanup;
	}

	env->SetDoubleArrayRegion(duals, 0, size, c_duals);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_duals;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getPtrDualSolution
 * Signature: ()[D
 */
JNIEXPORT jdoubleArray JNICALL Java_lpsolve_LpSolve_getPtrDualSolution
  (JNIEnv *env, jobject obj)
{
	unsigned char rc;
	jdoubleArray duals = NULL;

	lprec* lp = get_lp(env, obj);
	int nRows = get_Nrows(lp);
	int nCols = get_Ncolumns(lp);
	int size = 1 + nRows + nCols;
	double* c_duals = new double[size];
	if (c_duals == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}

	/* We do not use the _ptr_variants because we want to control memory allocation */
	rc = get_dual_solution(lp, c_duals);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_dual_solution");
		goto cleanup;
	}

	duals = env->NewDoubleArray(size);
	if (duals == NULL) {
		goto cleanup;	/* exception thrown */
	}

	env->SetDoubleArrayRegion(duals, 0, size, c_duals);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_duals;
	return duals;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getVarDualresult
 * Signature: (I)D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getVarDualresult
  (JNIEnv *env, jobject obj, jint index)
{
	lprec* lp = get_lp(env, obj);
	return get_var_dualresult(lp, index);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getSensitivityObj
 * Signature: ([D[D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_getSensitivityObj
  (JNIEnv *env, jobject obj, jdoubleArray objfrom, jdoubleArray objtill)
{
	unsigned char rc;
	double *c_objfrom = NULL, *c_objtill = NULL;
	lprec* lp = get_lp(env, obj);
	int size = get_Ncolumns(lp);
	if ((c_objfrom = new double[size]) == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}
	if ((c_objtill = new double[size]) == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}

	/* We do not use the _ptr_variants because we want to control memory allocation */
	rc = get_sensitivity_obj(lp, c_objfrom, c_objtill);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_sensitivity_obj");
		goto cleanup;
	}

	if (env->GetArrayLength(objfrom) < size || env->GetArrayLength(objtill) < size) {
		throw_exception(env, "Target array is too short to hold values");
		goto cleanup;
	}

	env->SetDoubleArrayRegion(objfrom, 0, size, c_objfrom);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	env->SetDoubleArrayRegion(objtill, 0, size, c_objtill);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_objfrom, c_objtill;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getPtrSensitivityObj
 * Signature: ()[[D
 */
JNIEXPORT jobjectArray JNICALL Java_lpsolve_LpSolve_getPtrSensitivityObj
  (JNIEnv *env, jobject obj)
{
	// See also Liang, p. 39 for an example with two-dimensional arrays.
	unsigned char rc;
	jobjectArray result = NULL;
	jdoubleArray objfrom = NULL, objtill = NULL;
	double *c_objfrom = NULL, *c_objtill = NULL;

	jclass doubleArr_cls = env->FindClass("[D");
	if (doubleArr_cls == NULL) {
		return NULL;	/* exception thrown */
	}

	// the resulting object array will contain two double[] arrays
	result = env->NewObjectArray(2, doubleArr_cls, NULL);
	if (result == NULL) {
		return NULL;	/* out of memory error thrown */
	}

	// Now allocate the arrays for the lp_solve method
	lprec* lp = get_lp(env, obj);
	int size = get_Ncolumns(lp);

	if ((objfrom = env->NewDoubleArray(size)) == NULL) {
		goto cleanup;	/* exception thrown */
	}
	if ((objtill = env->NewDoubleArray(size)) == NULL) {
		goto cleanup;	/* exception thrown */
	}
	if ((c_objfrom = new double[size]) == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}
	if ((c_objtill = new double[size]) == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}

	/* We do not use the _ptr_variants because we want to control memory allocation */
	rc = get_sensitivity_obj(lp, c_objfrom, c_objtill);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_sensitivity_obj");
		goto cleanup;
	}

	env->SetDoubleArrayRegion(objfrom, 0, size, c_objfrom);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	env->SetDoubleArrayRegion(objtill, 0, size, c_objtill);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}

	// Set the lements of the resulting array
	env->SetObjectArrayElement(result, 0, objfrom);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	env->SetObjectArrayElement(result, 1, objtill);
	/* no exception check; we're terminating anyway */


cleanup:
	delete c_objfrom, c_objtill;
	return result;
}

/*
 * Common code from getSensitivityObjex and getPtrSensitivityObjex implementation
 */
void getSensitivityObjex
  (JNIEnv *env, jobject obj, jdoubleArray objfrom, jdoubleArray objtill, jdoubleArray objfromvalue, jdoubleArray objtillvalue)
{
	double *c_objfrom = NULL, *c_objtill = NULL, *c_objfromvalue = NULL, *c_objtillvalue = NULL;
	unsigned char rc;
	lprec* lp = get_lp(env, obj);
	int size = get_Ncolumns(lp);

	if ((c_objfrom = new double[size]) == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}
	if ((c_objtill = new double[size]) == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}
	if ((c_objfromvalue = new double[size]) == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}
	if ((c_objtillvalue = new double[size]) == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}

	rc = get_sensitivity_objex(lp, c_objfrom, c_objtill, c_objfromvalue, c_objtillvalue);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_sensitivity_objex");
		goto cleanup;
	}

	if (env->GetArrayLength(objfrom) < size || env->GetArrayLength(objtill) < size
		|| env->GetArrayLength(objfromvalue) < size || env->GetArrayLength(objtillvalue) < size)
	{
		throw_exception(env, "Target array is too short to hold values");
		goto cleanup;
	}

	// copy results to the Java arrays
	env->SetDoubleArrayRegion(objfrom, 0, size, c_objfrom);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	env->SetDoubleArrayRegion(objtill, 0, size, c_objtill);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	env->SetDoubleArrayRegion(objfromvalue, 0, size, c_objfromvalue);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	env->SetDoubleArrayRegion(objtillvalue, 0, size, c_objtillvalue);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_objfrom, c_objtill, c_objfromvalue, c_objtillvalue;
}


/*
 * Class:     lpsolve_LpSolve
 * Method:    getSensitivityObjex
 * Signature: ([D[D[D[D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_getSensitivityObjex
  (JNIEnv *env, jobject obj, jdoubleArray objfrom, jdoubleArray objtill, jdoubleArray objfromvalue, jdoubleArray objtillvalue)
{
	getSensitivityObjex(env, obj, objfrom, objtill, objfromvalue, objtillvalue);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getPtrSensitivityObjex
 * Signature: ()[[D
 */
JNIEXPORT jobjectArray JNICALL Java_lpsolve_LpSolve_getPtrSensitivityObjex
  (JNIEnv *env, jobject obj)
{
	// See also Liang, p. 39 for an example with two-dimensional arrays.
	jobjectArray result = NULL;
	jdoubleArray objfrom = NULL, objtill = NULL, objfromvalue = NULL, objtillvalue = NULL;
	double *c_objfrom = NULL, *c_objtill = NULL, *c_objfromvalue = NULL, *c_objtillvalue = NULL;

	jclass doubleArr_cls = env->FindClass("[D");
	if (doubleArr_cls == NULL) {
		return NULL;	/* exception thrown */
	}

	// the resulting object array will contain four double[] arrays
	result = env->NewObjectArray(4, doubleArr_cls, NULL);
	if (result == NULL) {
		return NULL;	/* out of memory error thrown */
	}

	// Now allocate the arrays for the lp_solve method
	lprec* lp = get_lp(env, obj);
	int size = get_Ncolumns(lp);

	if ((objfrom = env->NewDoubleArray(size)) == NULL) {
		goto cleanup;	/* exception thrown */
	}
	if ((objtill = env->NewDoubleArray(size)) == NULL) {
		goto cleanup;	/* exception thrown */
	}
	if ((objfromvalue = env->NewDoubleArray(size)) == NULL) {
		goto cleanup;	/* exception thrown */
	}
	if ((objtillvalue = env->NewDoubleArray(size)) == NULL) {
		goto cleanup;	/* exception thrown */
	}

	getSensitivityObjex(env, obj, objfrom, objtill, objfromvalue, objtillvalue);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}

	// Set the lements of the resulting array
	env->SetObjectArrayElement(result, 0, objfrom);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	env->SetObjectArrayElement(result, 1, objtill);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	env->SetObjectArrayElement(result, 2, objfromvalue);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	env->SetObjectArrayElement(result, 3, objtillvalue);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_objfrom, c_objtill, c_objfromvalue, c_objtillvalue;
	return result;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getPtrLambda
 * Signature: ()[D
 */
JNIEXPORT jdoubleArray JNICALL Java_lpsolve_LpSolve_getPtrLambda
  (JNIEnv *env, jobject obj)
{
	unsigned char rc;
	jdoubleArray lambda = NULL;

	lprec* lp = get_lp(env, obj);
	int size = get_Lrows(lp);
	double* c_lambda = new double[size];
	if (c_lambda == NULL) {
		throw_exception(env, "getPrimalSolution: cannot allocate memory");
		goto cleanup;
	}

	rc = get_lambda(lp, c_lambda);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_lambda");
		goto cleanup;
	}

	lambda = env->NewDoubleArray(size);
	if (lambda == NULL) {
		goto cleanup;	/* exception thrown */
	}

	env->SetDoubleArrayRegion(lambda, 0, size, c_lambda);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_lambda;
	return lambda;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getLambda
 * Signature: ([D)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_getLambda
  (JNIEnv *env, jobject obj, jdoubleArray lambda)
{
	lprec* lp = get_lp(env, obj);
	unsigned char rc;
	int size = get_Lrows(lp);
	double* c_lambda = new double[size];
	if (c_lambda == NULL) {
		throw_exception(env, "cannot allocate memory");
		goto cleanup;
	}

	/* We do not use the _ptr_variants because we want to control memory allocation */
	rc = get_lambda(lp, c_lambda);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_lambda");
		goto cleanup;
	}

	if (env->GetArrayLength(lambda) < size) {
		throw_exception(env, "Target array is too short to hold values");
		goto cleanup;
	}

	env->SetDoubleArrayRegion(lambda, 0, size, c_lambda);
	/* no exception check; we're terminating anyway */

cleanup:
	delete c_lambda;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    deleteLp
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_deleteLp
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	delete_lp(lp);
	put_lp(env, obj, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getNrows
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getNrows
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_Nrows(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getNcolumns
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getNcolumns
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_Ncolumns(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getNorigRows
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getNorigRows
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_Norig_rows(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getNorigColumns
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getNorigColumns
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_Norig_columns(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getNonzeros
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getNonzeros
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_nonzeros(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getLrows
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getLrows
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_Lrows(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getOrigIndex
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getOrigIndex
  (JNIEnv *env, jobject obj, jint index)
{
	lprec* lp = get_lp(env, obj);
	return get_orig_index(lp, index);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getLpIndex
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getLpIndex
  (JNIEnv *env, jobject obj, jint index)
{
	lprec* lp = get_lp(env, obj);
	return get_lp_index(lp, index);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    guessBasis
 * Signature: ([IZ)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_guessBasis
  (JNIEnv *env, jobject obj, jdoubleArray guessvector, jintArray basisvector)
{
	unsigned char rc;
	jdouble* c_guessvector;
	jint* c_basisvector;
        lprec* lp = get_lp(env, obj);

	c_guessvector = env->GetDoubleArrayElements(guessvector, NULL);
	if (c_guessvector == NULL) {
                goto cleanup;	/* exception thrown */
	}

        c_basisvector = env->GetIntArrayElements(basisvector, NULL);
	if (c_basisvector == NULL) {
                goto cleanup;	/* exception thrown */
	}

	rc = guess_basis(lp, c_guessvector, (int *)c_basisvector);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "guess_basis");
		/* Fall through and cleanup */
	}

cleanup:
	env->ReleaseDoubleArrayElements(guessvector, c_guessvector, 0);
        env->ReleaseIntArrayElements(basisvector, c_basisvector, 0);
}


/*
 * Class:     lpsolve_LpSolve
 * Method:    setBasis
 * Signature: ([IZ)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setBasis
  (JNIEnv *env, jobject obj, jintArray bascolumn, jboolean nonbasic)
{
	unsigned char rc;
	jint* c_bascolumn = env->GetIntArrayElements(bascolumn, NULL);
	if (c_bascolumn == NULL) {
		return;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	rc = set_basis(lp, (int *)c_bascolumn, (nonbasic == JNI_TRUE) ? TRUE : FALSE);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_basis");
		/* Fall through and cleanup */
	}

	env->ReleaseIntArrayElements(bascolumn, c_bascolumn, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getBasis
 * Signature: ([IZ)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_getBasis
  (JNIEnv *env, jobject obj, jintArray bascolumn, jboolean nonbasic)
{
	unsigned char rc;
	jint* c_bascolumn = env->GetIntArrayElements(bascolumn, NULL);
	if (c_bascolumn == NULL) {
		return;	/* exception thrown */
	}

	lprec* lp = get_lp(env, obj);
	// get_basis has no return value to indicate failure !
	rc = get_basis(lp, (int *)c_bascolumn, (nonbasic == JNI_TRUE) ? TRUE : FALSE);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "get_basis");
		/* Fall through and cleanup */
	}

	env->ReleaseIntArrayElements(bascolumn, c_bascolumn, 0);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    resetBasis
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_resetBasis
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	reset_basis(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    defaultBasis
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_defaultBasis
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	default_basis(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setBasiscrash
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setBasiscrash
  (JNIEnv *env, jobject obj, jint mode)
{
	lprec* lp = get_lp(env, obj);
	set_basiscrash(lp, mode);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getBasiscrash
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getBasiscrash
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return get_basiscrash(lp);
}


/*
 * Class:     lpsolve_LpSolve
 * Method:    unscale
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_unscale
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	unscale(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setBFP
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setBFP
  (JNIEnv *env, jobject obj, jstring name)
{
	const char* c_name = NULL;

	if (name != NULL) {
		if ((c_name = env->GetStringUTFChars(name, NULL)) == NULL) {
			return;	/* exception thrown */
		}
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_BFP(lp, (char *)c_name);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_BFP");
		/* fall through and cleanup */
	}

	env->ReleaseStringUTFChars(name, c_name);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isNativeBFP
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isNativeBFP
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return ((is_nativeBFP(lp) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    hasBFP
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_hasBFP
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return ((has_BFP(lp) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    hasXLI
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_hasXLI
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return ((has_XLI(lp) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isNativeXLI
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isNativeXLI
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	return ((is_nativeXLI(lp) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getNameindex
 * Signature: (Ljava/lang/String;Z)I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_getNameindex
  (JNIEnv *env, jobject obj, jstring name, jboolean isrow)
{
	const char* c_name = NULL;

	if (name != NULL) {
		if ((c_name = env->GetStringUTFChars(name, NULL)) == NULL) {
			return -1;	/* exception thrown */
		}
	}

	lprec* lp = get_lp(env, obj);
	int rc = get_nameindex(lp, (char *)c_name, (isrow == JNI_TRUE) ? TRUE : FALSE);

	env->ReleaseStringUTFChars(name, c_name);
	return rc;
}


/*
 * Class:     lpsolve_LpSolve
 * Method:    lpSolveVersion
 * Signature: ()Llpsolve/VersionInfo;
 */
JNIEXPORT jobject JNICALL Java_lpsolve_LpSolve_lpSolveVersion
  (JNIEnv *env, jclass cls)
{
	int major, minor, release, build;
	lp_solve_version(&major, &minor, &release, &build);

	jclass info_cls = env->FindClass("lpsolve/VersionInfo");
	if (info_cls == NULL) {
		return NULL;	/* exception thrown */
	}

	jmethodID cid = env->GetMethodID(info_cls, "<init>", "(IIII)V");
	if (cid == NULL) {
		return NULL;	/* exception thrown */
	}

	jobject result = env->NewObject(info_cls, cid, major, minor, release, build);
	/* no error checking because we're terminating anyway */

	env->DeleteLocalRef(info_cls);
	return result;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    dualizeLp
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_dualizeLp
  (JNIEnv *env, jobject obj)
{
	lprec *lp = get_lp(env, obj);
	unsigned char rc = dualize_lp(lp);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "dualize_lp");
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    isUseNames
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_lpsolve_LpSolve_isUseNames
  (JNIEnv *env, jobject obj, jboolean isRow)
{
	lprec* lp = get_lp(env, obj);
	return ((is_use_names(lp, (isRow == JNI_TRUE) ? TRUE : FALSE) == TRUE) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setUseNames
 * Signature: (ZZ)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setUseNames
  (JNIEnv *env, jobject obj, jboolean isRow, jboolean useNames)
{
	lprec* lp = get_lp(env, obj);
	set_use_names(lp, (isRow == JNI_TRUE) ? TRUE : FALSE, (useNames == JNI_TRUE) ? TRUE : FALSE);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    getConstrValue
 * Signature: (II[D[I)D
 */
JNIEXPORT jdouble JNICALL Java_lpsolve_LpSolve_getConstrValue
  (JNIEnv *env, jobject obj, jint rownr, jint count, jdoubleArray primsolution, jintArray nzindex)
{
	double value;
	jdouble* c_primsolution = NULL;
	jint* c_nzindex = NULL;
	lprec* lp;

	c_primsolution = env->GetDoubleArrayElements(primsolution, NULL);
	if (c_primsolution == NULL) {
		goto cleanup;	/* exception thrown */
	}

	c_nzindex = env->GetIntArrayElements(nzindex, NULL);
	if (c_nzindex == NULL) {
		goto cleanup;	/* exception thrown */
	}

	lp = get_lp(env, obj);
	value = get_constr_value(lp, rownr, count, c_primsolution, (int *)c_nzindex);

cleanup:
	env->ReleaseDoubleArrayElements(primsolution, c_primsolution, 0);
	env->ReleaseIntArrayElements(nzindex, c_nzindex, 0);

	return value;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setBasisvar
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_lpsolve_LpSolve_setBasisvar
  (JNIEnv *env, jobject obj, jint basisPos, jint enteringCol)
{
	lprec* lp = get_lp(env, obj);
	return set_basisvar(lp, (char)basisPos, (char)enteringCol);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    writeLp
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_writeLp
  (JNIEnv *env, jobject obj, jstring filename)
{
	lprec *lp = NULL;
	unsigned char rc;
	const char *c_filename = JNU_GetStringNativeChars(env, filename);
	if (c_filename == 0 || env->ExceptionCheck()) {
		return;	/* exception thrown */
	}
	lp = get_lp(env, obj);
	rc = write_lp(lp, (char*)c_filename);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "write_lp");
		/* fall through and release pointer */
	}
	delete (char *)c_filename;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    writeMps
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_writeMps
  (JNIEnv *env, jobject obj, jstring filename)
{
	lprec *lp = NULL;
	unsigned char rc;
	const char *c_filename = JNU_GetStringNativeChars(env, filename);
	if (c_filename == 0 || env->ExceptionCheck()) {
		return;	/* exception thrown */
	}
	lp = get_lp(env, obj);
	rc = write_mps(lp, (char*)c_filename);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "write_mps");
		/* fall through and release pointer */
	}
	delete (char *)c_filename;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    writeBasis
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_writeBasis
  (JNIEnv *env, jobject obj, jstring filename)
{
	lprec *lp = NULL;
	unsigned char rc;
	const char *c_filename = JNU_GetStringNativeChars(env, filename);
	if (c_filename == 0 || env->ExceptionCheck()) {
		return;	/* exception thrown */
	}
	lp = get_lp(env, obj);
	rc = write_basis(lp, (char*)c_filename);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "write_basis");
		/* fall through and release pointer */
	}
	delete (char *)c_filename;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    readParams
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_readParams
  (JNIEnv *env, jobject obj, jstring filename, jstring options)
{
	lprec *lp = NULL;
	unsigned char rc;
	const char *c_filename = NULL;
	const char *c_options = NULL;

	c_filename = JNU_GetStringNativeChars(env, filename);
	if (c_filename == 0 || env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}

	c_options = JNU_GetStringNativeChars(env, options);
	if (c_options == 0 || env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}


	lp = get_lp(env, obj);
	rc = read_params(lp, (char*)c_filename, (char*)c_options);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "read_params");
		/* fall through and release pointer */
	}

cleanup:
	delete (char *)c_filename;
	delete (char *)c_options;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    writeParams
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_writeParams
  (JNIEnv *env, jobject obj, jstring filename, jstring options)
{
	lprec *lp = NULL;
	unsigned char rc;
	const char *c_filename = NULL;
	const char *c_options = NULL;

	c_filename = JNU_GetStringNativeChars(env, filename);
	if (c_filename == 0 || env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}

	c_options = JNU_GetStringNativeChars(env, options);
	if (c_options == 0 || env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}


	lp = get_lp(env, obj);
	rc = write_params(lp, (char*)c_filename, (char*)c_options);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "read_params");
		/* fall through and release pointer */
	}

cleanup:
	delete (char *)c_filename;
	delete (char *)c_options;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    resetParams
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_resetParams
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	reset_params(lp);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    writeFreeMps
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_writeFreeMps
  (JNIEnv *env, jobject obj, jstring filename)
{
	lprec *lp = NULL;
	unsigned char rc;
	const char *c_filename = JNU_GetStringNativeChars(env, filename);
	if (c_filename == 0 || env->ExceptionCheck()) {
		return;	/* exception thrown */
	}
	lp = get_lp(env, obj);
	rc = write_freemps(lp, (char*)c_filename);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "write_freemps");
		/* fall through and release pointer */
	}
	delete (char *)c_filename;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setXLI
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setXLI
  (JNIEnv *env, jobject obj, jstring filename)
{
	const char *c_filename = env->GetStringUTFChars(filename, NULL);
	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_XLI(lp, (char*)c_filename);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_XLI");
		/* fall through and release pointer */
	}
	env->ReleaseStringUTFChars(filename, c_filename);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    readBasis
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_lpsolve_LpSolve_readBasis
  (JNIEnv *env, jobject obj, jstring filename)
{
	jstring info = NULL;
	lprec *lp = NULL;
	unsigned char rc;
	const char *c_filename = NULL;
	char c_info[500];

	if (filename != NULL) {
		c_filename = JNU_GetStringNativeChars(env, filename);
		if (c_filename == 0 || env->ExceptionCheck()) {
			goto cleanup;	/* exception thrown */
		}
	}

	lp = get_lp(env, obj);
	rc = read_basis(lp, (char*)c_filename, c_info);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "read_basis");
		goto cleanup;
	}
	info = env->NewStringUTF(c_info);
	/* no exception check; we're terminating anyway */

cleanup:
	delete (char *)c_filename;
	return info;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    writeXLI
 * Signature: (Ljava/lang/String;Ljava/lang/String;Z)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_writeXLI
  (JNIEnv *env, jobject obj, jstring filename, jstring options, jboolean results)
{
	lprec *lp = NULL;
	unsigned char rc;
	const char *c_filename = NULL;
	const char *c_options = NULL;

	if (filename != NULL) {
		c_filename = JNU_GetStringNativeChars(env, filename);
		if (c_filename == 0 || env->ExceptionCheck()) {
			goto cleanup;	/* exception thrown */
		}
	}

	if (options != NULL) {
		c_options = JNU_GetStringNativeChars(env, options);
		if (c_options == 0 || env->ExceptionCheck()) {
			goto cleanup;	/* exception thrown */
		}
	}

	lp = get_lp(env, obj);
	rc = write_XLI(lp, (char*)c_filename, (char *)c_options, (results == JNI_TRUE) ? TRUE : FALSE);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "write_XLI");
		/* fall through and release pointer */
	}

cleanup:
	delete (char *)c_filename;
	delete (char *)c_options;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    printLp
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_printLp
  (JNIEnv *env, jobject obj)
{
	print_lp(get_lp(env, obj));
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    printConstraints
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_printConstraints
  (JNIEnv *env, jobject obj, jint columns)
{
	print_constraints(get_lp(env, obj), columns);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    printDuals
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_printDuals
  (JNIEnv *env, jobject obj)
{
	print_duals(get_lp(env, obj));
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    printScales
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_printScales
  (JNIEnv *env, jobject obj)
{
	print_scales(get_lp(env, obj));
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    printTableau
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_printTableau
  (JNIEnv *env, jobject obj)
{
	print_tableau(get_lp(env, obj));
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    printObjective
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_printObjective
  (JNIEnv *env, jobject obj)
{
	print_objective(get_lp(env, obj));
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    printSolution
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_printSolution
  (JNIEnv *env, jobject obj, jint columns)
{
	print_solution(get_lp(env, obj), columns);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    printStr
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_printStr
  (JNIEnv *env, jobject obj, jstring str)
{
	const char* c_str = NULL;

	if (str != NULL) {
		c_str = JNU_GetStringNativeChars(env, str);
		if (c_str == 0 || env->ExceptionCheck()) {
			return;	/* exception thrown */
		}

		lprec* lp = get_lp(env, obj);
		print_str(lp, (char *)c_str);

		delete (char *)c_str;
	}
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    setOutputfile
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_setOutputfile
  (JNIEnv *env, jobject obj, jstring filename)
{
	const char* c_filename = NULL;

	if (filename != NULL) {
		if ((c_filename = env->GetStringUTFChars(filename, NULL)) == NULL) {
			return;	/* exception thrown */
		}
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = set_outputfile(lp, (char *)c_filename);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "set_outputfile");
		/* fall through and cleanup */
	}

	env->ReleaseStringUTFChars(filename, c_filename);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    printDebugdump
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_printDebugdump
  (JNIEnv *env, jobject obj, jstring filename)
{
	const char* c_filename = NULL;

	if (filename != NULL) {
		if ((c_filename = env->GetStringUTFChars(filename, NULL)) == NULL) {
			return;	/* exception thrown */
		}
	}

	lprec* lp = get_lp(env, obj);
	unsigned char rc = print_debugdump(lp, (char *)c_filename);
	if (rc == FALSE) {
		throw_exception_with_status(env, lp, "print_debugdump");
		/* fall through and cleanup */
	}

	env->ReleaseStringUTFChars(filename, c_filename);
}

/*
 * Abort callback function
 */
int abortfunction(lprec* lp, void* userhandle) {
	//printf("C abortfunction called ..\n");
	int doAbort = TRUE;
	jclass lpsolve_cls = NULL;
	jobject lpsolve_obj = NULL;
	jobject listener_obj = NULL;
	jobject handle_obj = NULL;
	jboolean rc;

	/* Get a pointer to the JNI environment from the global JavaVM object */
	JNIEnv *env;
	g_jvm->AttachCurrentThread((void **)&env, NULL);

	lpsolve_cls = env->FindClass("lpsolve/LpSolve");
	if (lpsolve_cls == NULL) {
		goto cleanup;	/* exception thrown */
	}

	/* Get the LpSolve object that contains the given lp argument */
	lpsolve_obj = env->CallStaticObjectMethod(lpsolve_cls, MID_getLp, reinterpret_cast<jlong>(lp));
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	if (lpsolve_obj == NULL) {
		throw_exception(env, "abortfunction: No LpSolve object found");
		goto cleanup;
	}

	/* Get the listener object */
	listener_obj = env->GetObjectField(lpsolve_obj, FID_abortListener);
	if (listener_obj == NULL) {
		throw_exception(env, "abortfunction: No AbortListener object found");
		goto cleanup;
	}

	/* Get the userhandle object */
	handle_obj = env->GetObjectField(lpsolve_obj, FID_abortHandle);

	/* Call the callback method */
	rc = env->CallBooleanMethod(listener_obj, MID_abortfunc, lpsolve_obj, handle_obj);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	doAbort = (rc == JNI_TRUE) ? TRUE : FALSE;

cleanup:
	env->DeleteLocalRef(listener_obj);
	env->DeleteLocalRef(lpsolve_obj);
	env->DeleteLocalRef(lpsolve_cls);
	env->DeleteLocalRef(handle_obj);
	return doAbort;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    registerAbortfunc
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_registerAbortfunc
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	jobject listener_obj = env->GetObjectField(obj, FID_abortListener);
	put_abortfunc(lp, (listener_obj != NULL) ? abortfunction : NULL, NULL);
	env->DeleteLocalRef(listener_obj);
}

/*
 * Log callback function
 */
void logfunction(lprec* lp, void* userhandle, char* buf) {
	//printf("C logfunction called, buf = %s ..\n", buf);
	jclass lpsolve_cls = NULL;
	jobject lpsolve_obj = NULL;
	jobject listener_obj = NULL;
	jobject handle_obj = NULL;
	jstring j_buf = NULL;

	/* Get a pointer to the JNI environment from the global JavaVM object */
	JNIEnv *env;
	g_jvm->AttachCurrentThread((void **)&env, NULL);

	lpsolve_cls = env->FindClass("lpsolve/LpSolve");
	if (lpsolve_cls == NULL) {
		goto cleanup;	/* exception thrown */
	}

	/* Get the LpSolve object that contains the given lp argument */
	lpsolve_obj = env->CallStaticObjectMethod(lpsolve_cls, MID_getLp, reinterpret_cast<jlong>(lp));
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	if (lpsolve_obj == NULL) {
		throw_exception(env, "logfunction: No LpSolve object found");
		goto cleanup;
	}

	/* Get the listener object */
	listener_obj = env->GetObjectField(lpsolve_obj, FID_logListener);
	if (listener_obj == NULL) {
		throw_exception(env, "logfunction: No LogListener object found");
		goto cleanup;
	}

	/* Get the userhandle object */
	handle_obj = env->GetObjectField(lpsolve_obj, FID_logHandle);

	/* Construct the Java string */
	j_buf = env->NewStringUTF(buf);
	if (j_buf == NULL) {
		goto cleanup;	/* exception thrown */
	}

	/* Call the callback method */
	env->CallVoidMethod(listener_obj, MID_logfunc, lpsolve_obj, handle_obj, j_buf);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}

cleanup:
	env->DeleteLocalRef(listener_obj);
	env->DeleteLocalRef(lpsolve_obj);
	env->DeleteLocalRef(lpsolve_cls);
	env->DeleteLocalRef(handle_obj);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    registerLogfunc
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_registerLogfunc
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	jobject listener_obj = env->GetObjectField(obj, FID_logListener);
	put_logfunc(lp, (listener_obj != NULL) ? logfunction : NULL, NULL);
	env->DeleteLocalRef(listener_obj);
}

/*
 * Msg callback function
 */
void msgfunction(lprec* lp, void* userhandle, int code) {
	//printf("C msgfunction called, code = %d ..\n", code);
	jclass lpsolve_cls = NULL;
	jobject lpsolve_obj = NULL;
	jobject listener_obj = NULL;
	jobject handle_obj = NULL;

	/* Get a pointer to the JNI environment from the global JavaVM object */
	JNIEnv *env;
	g_jvm->AttachCurrentThread((void **)&env, NULL);

	lpsolve_cls = env->FindClass("lpsolve/LpSolve");
	if (lpsolve_cls == NULL) {
		goto cleanup;	/* exception thrown */
	}

	/* Get the LpSolve object that contains the given lp argument */
	lpsolve_obj = env->CallStaticObjectMethod(lpsolve_cls, MID_getLp, reinterpret_cast<jlong>(lp));
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	if (lpsolve_obj == NULL) {
		throw_exception(env, "msgfunction: No LpSolve object found");
		goto cleanup;
	}

	/* Get the listener object */
	listener_obj = env->GetObjectField(lpsolve_obj, FID_msgListener);
	if (listener_obj == NULL) {
		throw_exception(env, "msgfunction: No MsgListener object found");
		goto cleanup;
	}

	/* Get the userhandle object */
	handle_obj = env->GetObjectField(lpsolve_obj, FID_msgHandle);

	/* Call the callback method */
	env->CallVoidMethod(listener_obj, MID_msgfunc, lpsolve_obj, handle_obj, code);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}

cleanup:
	env->DeleteLocalRef(listener_obj);
	env->DeleteLocalRef(lpsolve_obj);
	env->DeleteLocalRef(lpsolve_cls);
	env->DeleteLocalRef(handle_obj);
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    registerMsgfunc
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_registerMsgfunc
  (JNIEnv *env, jobject obj, jint mask)
{
	lprec* lp = get_lp(env, obj);
	jobject listener_obj = env->GetObjectField(obj, FID_msgListener);
	put_msgfunc(lp, (listener_obj != NULL) ? msgfunction : NULL, NULL, mask);
	env->DeleteLocalRef(listener_obj);
}

/*
 * bb_branch callback function
 */
int bb_branchfunction(lprec* lp, void* userhandle, int message) {
	//printf("C msgfunction called, code = %d ..\n", code);
	jclass lpsolve_cls = NULL;
	jobject lpsolve_obj = NULL;
	jobject listener_obj = NULL;
	jobject handle_obj = NULL;
	jint rc;

	/* Get a pointer to the JNI environment from the global JavaVM object */
	JNIEnv *env;
	g_jvm->AttachCurrentThread((void **)&env, NULL);

	lpsolve_cls = env->FindClass("lpsolve/LpSolve");
	if (lpsolve_cls == NULL) {
		goto cleanup;	/* exception thrown */
	}

	/* Get the LpSolve object that contains the given lp argument */
	lpsolve_obj = env->CallStaticObjectMethod(lpsolve_cls, MID_getLp, reinterpret_cast<jlong>(lp));
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	if (lpsolve_obj == NULL) {
		throw_exception(env, "bb_branchfunction: No LpSolve object found");
		goto cleanup;
	}

	/* Get the listener object */
	listener_obj = env->GetObjectField(lpsolve_obj, FID_bbBranchListener);
	if (listener_obj == NULL) {
		throw_exception(env, "bb_branchfunction: No BbListener object found");
		goto cleanup;
	}

	/* Get the userhandle object */
	handle_obj = env->GetObjectField(lpsolve_obj, FID_bbBranchHandle);

	/* Call the callback method */
	rc = env->CallIntMethod(listener_obj, MID_bbfunc, lpsolve_obj, handle_obj, message);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}

cleanup:
	env->DeleteLocalRef(listener_obj);
	env->DeleteLocalRef(lpsolve_obj);
	env->DeleteLocalRef(lpsolve_cls);
	env->DeleteLocalRef(handle_obj);

	return rc;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    registerBbBranchfunc
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_registerBbBranchfunc
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	jobject listener_obj = env->GetObjectField(obj, FID_bbBranchListener);
	put_bb_branchfunc(lp, (listener_obj != NULL) ? bb_branchfunction : NULL, NULL);
	env->DeleteLocalRef(listener_obj);
}

/*
 * bb_node callback function
 */
int bb_nodefunction(lprec* lp, void* userhandle, int message) {
	//printf("C msgfunction called, code = %d ..\n", code);
	jclass lpsolve_cls = NULL;
	jobject lpsolve_obj = NULL;
	jobject listener_obj = NULL;
	jobject handle_obj = NULL;
	jint rc;

	/* Get a pointer to the JNI environment from the global JavaVM object */
	JNIEnv *env;
	g_jvm->AttachCurrentThread((void **)&env, NULL);

	lpsolve_cls = env->FindClass("lpsolve/LpSolve");
	if (lpsolve_cls == NULL) {
		goto cleanup;	/* exception thrown */
	}

	/* Get the LpSolve object that contains the given lp argument */
	lpsolve_obj = env->CallStaticObjectMethod(lpsolve_cls, MID_getLp, reinterpret_cast<jlong>(lp));
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}
	if (lpsolve_obj == NULL) {
		throw_exception(env, "bb_nodefunction: No LpSolve object found");
		goto cleanup;
	}

	/* Get the listener object */
	listener_obj = env->GetObjectField(lpsolve_obj, FID_bbNodeListener);
	if (listener_obj == NULL) {
		throw_exception(env, "bb_nodefunction: No BbListener object found");
		goto cleanup;
	}

	/* Get the userhandle object */
	handle_obj = env->GetObjectField(lpsolve_obj, FID_bbNodeHandle);

	/* Call the callback method */
	rc = env->CallIntMethod(listener_obj, MID_bbfunc, lpsolve_obj, handle_obj, message);
	if (env->ExceptionCheck()) {
		goto cleanup;	/* exception thrown */
	}

cleanup:
	env->DeleteLocalRef(listener_obj);
	env->DeleteLocalRef(lpsolve_obj);
	env->DeleteLocalRef(lpsolve_cls);
	env->DeleteLocalRef(handle_obj);

	return rc;
}

/*
 * Class:     lpsolve_LpSolve
 * Method:    registerBbNodefunc
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_lpsolve_LpSolve_registerBbNodefunc
  (JNIEnv *env, jobject obj)
{
	lprec* lp = get_lp(env, obj);
	jobject listener_obj = env->GetObjectField(obj, FID_bbNodeListener);
	put_bb_nodefunc(lp, (listener_obj != NULL) ? bb_nodefunction : NULL, NULL);
	env->DeleteLocalRef(listener_obj);
}
