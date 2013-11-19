//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

#include "PrismSparse.h"
#include "NDSparseMatrix.h"
#include <stdio.h>
#include <stdarg.h>
#include <limits.h>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include "sparse.h"
#include "PrismSparseGlob.h"
#include "jnipointer.h"

#define MAX_LOG_STRING_LEN 1024
#define MAX_ERR_STRING_LEN 1024

//------------------------------------------------------------------------------
// sparse engine global variables
//------------------------------------------------------------------------------

// cudd manager
DdManager *ddman;

// logs
// global refs to log classes
static jclass main_log_cls = NULL;
static jclass tech_log_cls = NULL;
// global refs to log objects
static jobject main_log_obj = NULL;
static jobject tech_log_obj = NULL;
// method ids for print method in logs
static jmethodID main_log_mid = NULL;
static jmethodID main_log_warn = NULL;
static jmethodID tech_log_mid = NULL;

// export stuff
int export_type;
FILE *export_file;
JNIEnv *export_env;

// error message
static char error_message[MAX_ERR_STRING_LEN];

//------------------------------------------------------------------------------
// cudd manager
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetCUDDManager(JNIEnv *env, jclass cls, jlong __jlongpointer ddm)
{
	ddman = jlong_to_DdManager(ddm);
}

//------------------------------------------------------------------------------
// logs
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetMainLog(JNIEnv *env, jclass cls, jobject log)
{
	// if main log has been set previously, we need to delete existing global refs first
	if (main_log_obj != NULL) {
		env->DeleteGlobalRef(main_log_cls);
		main_log_cls = NULL;
		env->DeleteGlobalRef(main_log_obj);
		main_log_obj = NULL;
	}
	
	// make a global reference to the log object
	main_log_obj = env->NewGlobalRef(log);
	// get the log class and make a global reference to it
	main_log_cls = (jclass)env->NewGlobalRef(env->GetObjectClass(main_log_obj));
	// get the method id for the print method
	main_log_mid = env->GetMethodID(main_log_cls, "print", "(Ljava/lang/String;)V");
    main_log_warn = env->GetMethodID(main_log_cls, "printWarning", "(Ljava/lang/String;)V");
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetTechLog(JNIEnv *env, jclass cls,  jobject log)
{
	// if tech log has been set previously, we need to delete existing global refs first
	if (tech_log_obj != NULL) {
		env->DeleteGlobalRef(tech_log_cls);
		tech_log_cls = NULL;
		env->DeleteGlobalRef(tech_log_obj);
		tech_log_obj = NULL;
	}
	
	// make a global reference to the log object
	tech_log_obj = env->NewGlobalRef(log);
	// get the log class and make a global reference to it
	tech_log_cls = (jclass)env->NewGlobalRef(env->GetObjectClass(tech_log_obj));
	// get the method id for the print method
	tech_log_mid = env->GetMethodID(tech_log_cls, "print", "(Ljava/lang/String;)V");
}

//------------------------------------------------------------------------------

void PS_PrintToMainLog(JNIEnv *env, const char *str, ...)
{
	va_list argptr;
	char full_string[MAX_LOG_STRING_LEN];
	
	va_start(argptr, str);
	vsnprintf(full_string, MAX_LOG_STRING_LEN, str, argptr);
	va_end(argptr);
	
	if (env)
		env->CallVoidMethod(main_log_obj, main_log_mid, env->NewStringUTF(full_string));
	else
		printf("%s", full_string);
}

//------------------------------------------------------------------------------

void PS_PrintWarningToMainLog(JNIEnv *env, const char *str, ...)
{
	va_list argptr;
	char full_string[MAX_LOG_STRING_LEN];
	
	va_start(argptr, str);
	vsnprintf(full_string, MAX_LOG_STRING_LEN, str, argptr);
	va_end(argptr);
	
	if (env)
		env->CallVoidMethod(main_log_obj, main_log_warn, env->NewStringUTF(full_string));
	else
		printf("\nWarning: %s\n", full_string);
}

//------------------------------------------------------------------------------

void PS_PrintToTechLog(JNIEnv *env, const char *str, ...)
{
	va_list argptr;
	char full_string[MAX_LOG_STRING_LEN];
	
	va_start(argptr, str);
	vsnprintf(full_string, MAX_LOG_STRING_LEN, str, argptr);
	va_end(argptr);
	
	if (env)
		env->CallVoidMethod(tech_log_obj, tech_log_mid, env->NewStringUTF(full_string));
	else
		printf("%s", full_string);
}

//------------------------------------------------------------------------------

// Print formatted memory info to main log

void PS_PrintMemoryToMainLog(JNIEnv *env, const char *before, double mem, const char *after)
{
	char full_string[MAX_LOG_STRING_LEN];
	
	if (mem > 1048576)
		snprintf(full_string, MAX_LOG_STRING_LEN, "%s%.1f GB%s", before, mem/1048576.0, after);
	else if (mem > 1024)
		snprintf(full_string, MAX_LOG_STRING_LEN, "%s%.1f MB%s", before, mem/1024.0, after);
	else
		snprintf(full_string, MAX_LOG_STRING_LEN, "%s%.1f KB%s", before, mem, after);
	
	if (env) {
		env->CallVoidMethod(main_log_obj, main_log_mid, env->NewStringUTF(full_string));
	}
	else {
		printf("%s", full_string);
	}
}

//------------------------------------------------------------------------------
// export stuff
//------------------------------------------------------------------------------

// store export info globally
// returns 0 on failure, 1 otherwise

int store_export_info(int type, jstring fn, JNIEnv *env)
{
	export_type = type;
	if (fn) {
		const char *filename = env->GetStringUTFChars(fn, 0);
		export_file = fopen(filename, "w");
		if (!export_file) {
			env->ReleaseStringUTFChars(fn, filename);
			return 0;
		}
		env->ReleaseStringUTFChars(fn, filename);
	} else {
		export_file = NULL;
	}
	export_env = env;
	return 1;
}

//------------------------------------------------------------------------------

// export string (either to file or main log)

void export_string(const char *str, ...)
{
	va_list argptr;
	char full_string[MAX_LOG_STRING_LEN];
	
	va_start(argptr, str);
	vsnprintf(full_string, MAX_LOG_STRING_LEN, str, argptr);
	va_end(argptr);
	
	if (export_file) {
		fprintf(export_file, full_string);
	} else {
		PS_PrintToMainLog(export_env, full_string);
	}
}

//------------------------------------------------------------------------------
// use steady-state detection?
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetDoSSDetect(JNIEnv *env, jclass cls, jboolean b)
{
	do_ss_detect = b;
}

//------------------------------------------------------------------------------
// error message handling
//------------------------------------------------------------------------------

void PS_SetErrorMessage(const char *str, ...)
{
	va_list argptr;
	
	va_start(argptr, str);
	vsnprintf(error_message, MAX_ERR_STRING_LEN, str, argptr);
	va_end(argptr);
}

char *PS_GetErrorMessage()
{
	return error_message;
}

JNIEXPORT jstring JNICALL Java_sparse_PrismSparse_PS_1GetErrorMessage(JNIEnv *env, jclass cls)
{
	return env->NewStringUTF(error_message);
}

//------------------------------------------------------------------------------
// tidy up
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1FreeGlobalRefs(JNIEnv *env, jclass cls)
{
	// delete all global references
	env->DeleteGlobalRef(main_log_cls);
	main_log_cls = NULL;
	env->DeleteGlobalRef(tech_log_cls);
	tech_log_cls = NULL;
	env->DeleteGlobalRef(main_log_obj);
	main_log_obj = NULL;
	env->DeleteGlobalRef(tech_log_obj);
	tech_log_obj = NULL;
}

//------------------------------------------------------------------------------
// Sparse matrix
//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_sparse_NDSparseMatrix_PS_1NDGetActionIndex
(JNIEnv *env, jclass cls,
 jlong __jlongpointer _ndsm, // NDSparseMatrix ptr
 jint __jlongpointer s, // state index
 jint __jlongpointer i // choice index
 )
{
    NDSparseMatrix * ndsm = (NDSparseMatrix *) jlong_to_NDSparseMatrix(_ndsm);
	bool use_counts = ndsm->use_counts;
	int *row_starts = (int *)ndsm->row_counts;
	unsigned char *row_counts = ndsm->row_counts;
	
	int l1 = 0;
	if (!use_counts) { l1 = row_starts[s]; }
	else { for (int i = 0; i < s; i++) l1 += row_counts[i]; }
	return ndsm->actions[l1];
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_NDSparseMatrix_PS_1BuildNDSparseMatrix
(JNIEnv *env, jclass cls,
 jlong __jlongpointer t,    // trans
 jlong __jlongpointer od, // odd
 jlong __jlongpointer rv, // row vars
 jint num_rvars,
 jlong __jlongpointer cv, // col vars
 jint num_cvars,
 jlong __jlongpointer ndv,  // nondet vars
 jint num_ndvars
 )
{
    NDSparseMatrix *ndsm = NULL;
    
    DdNode *trans = jlong_to_DdNode(t); //trans/reward matrix
    DdNode **rvars = jlong_to_DdNode_array(rv);   // row vars
    DdNode **cvars = jlong_to_DdNode_array(cv);   // col vars
    DdNode **ndvars = jlong_to_DdNode_array(ndv); // nondet vars
    ODDNode *odd = jlong_to_ODDNode(od);      // reachable states
    
	ndsm = build_nd_sparse_matrix(ddman, trans, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
    
    return ptr_to_jlong(ndsm);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_NDSparseMatrix_PS_1BuildSubNDSparseMatrix
(JNIEnv *env, jclass cls,
 jlong __jlongpointer t,    // trans
 jlong __jlongpointer od, // odd
 jlong __jlongpointer rv, // row vars
 jint num_rvars,
 jlong __jlongpointer cv, // col vars
 jint num_cvars,
 jlong __jlongpointer ndv,  // nondet vars
 jint num_ndvars,
 jlong __jlongpointer r    // reward
 )
{
    NDSparseMatrix *ndsm = NULL;
    
    DdNode *trans = jlong_to_DdNode(t); //trans/reward matrix
    DdNode **rvars = jlong_to_DdNode_array(rv);   // row vars
    DdNode **cvars = jlong_to_DdNode_array(cv);   // col vars
    DdNode **ndvars = jlong_to_DdNode_array(ndv); // nondet vars
    ODDNode *odd = jlong_to_ODDNode(od);      // reachable states
    DdNode *rewards = jlong_to_DdNode(r);
    
	ndsm = build_sub_nd_sparse_matrix(ddman, trans, rewards, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
    
    return ptr_to_jlong(ndsm);
}

//------------------------------------------------------------------------------

JNIEXPORT void __jlongpointer JNICALL Java_sparse_NDSparseMatrix_PS_1DeleteNDSparseMatrix
(JNIEnv *env, jclass cls,
 jlong __jlongpointer _ndsm)
{
    NDSparseMatrix * ndsm = (NDSparseMatrix *) jlong_to_NDSparseMatrix(_ndsm);
    if (ndsm) delete ndsm;
}

//------------------------------------------------------------------------------