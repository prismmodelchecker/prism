//==============================================================================
//	
//	File:		PrismSparse.cc
//	Author:		Dave Parker
//	Date:		10/04/01
//	Desc:		Main C++ file for sparse engine
//				(package wide global variables and JNI get/set methods for them)
//	
//------------------------------------------------------------------------------
//	
//	Copyright (c) 2002-2004, Dave Parker
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
#include <stdio.h>
#include <stdarg.h>
#include <limits.h>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include "PrismSparseGlob.h"

#define MAX_LOG_STRING_LEN 255

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
static jmethodID tech_log_mid = NULL;

// numerical method stuff
int lin_eq_method;
double lin_eq_method_param;
int term_crit;
double term_crit_param;
int max_iters;

// use "compact modified" sparse matrix storage?
bool compact;

//------------------------------------------------------------------------------
// cudd manager
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetCUDDManager(JNIEnv *env, jclass cls, jint ddm)
{
	ddman = (DdManager *)ddm;
}

//------------------------------------------------------------------------------
// logs
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetMainLog(JNIEnv *env, jclass cls, jobject log)
{
	// if main log has been set previously, we need to delete existing global refs first
	if (main_log_obj != NULL) {
		env->DeleteGlobalRef(main_log_cls);
		env->DeleteGlobalRef(main_log_obj);
	}
	
	// make a global reference to the log object
	main_log_obj = env->NewGlobalRef(log);
	// get the log class and make a global reference to it
	main_log_cls = (jclass)env->NewGlobalRef(env->GetObjectClass(main_log_obj));
	// get the method id for the print method
	main_log_mid = env->GetMethodID(main_log_cls, "print", "(Ljava/lang/String;)V");
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetTechLog(JNIEnv *env, jclass cls,  jobject log)
{
	// if tech log has been set previously, we need to delete existing global refs first
	if (tech_log_obj != NULL) {
		env->DeleteGlobalRef(tech_log_cls);
		env->DeleteGlobalRef(tech_log_obj);
	}
	
	// make a global reference to the log object
	tech_log_obj = env->NewGlobalRef(log);
	// get the log class and make a global reference to it
	tech_log_cls = (jclass)env->NewGlobalRef(env->GetObjectClass(tech_log_obj));
	// get the method id for the print method
	tech_log_mid = env->GetMethodID(tech_log_cls, "print", "(Ljava/lang/String;)V");
}

//------------------------------------------------------------------------------

void PS_PrintToMainLog(JNIEnv *env, char *str, ...)
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

void PS_PrintToTechLog(JNIEnv *env, char *str, ...)
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
// numerical method stuff
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetLinEqMethod(JNIEnv *env, jclass cls, jint i)
{
	lin_eq_method = i;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetLinEqMethodParam(JNIEnv *env, jclass cls, jdouble d)
{
	lin_eq_method_param = d;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetTermCrit(JNIEnv *env, jclass cls, jint i)
{
	term_crit = i;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetTermCritParam(JNIEnv *env, jclass cls, jdouble d)
{
	term_crit_param = d;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetMaxIters(JNIEnv *env, jclass cls, jint i)
{
	max_iters = i;
}

//------------------------------------------------------------------------------
// use "compact modified" sparse matrix storage?
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetCompact(JNIEnv *env, jclass cls, jboolean b)
{
	compact = b;
}

//------------------------------------------------------------------------------
// tidy up
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1FreeGlobalRefs(JNIEnv *env, jclass cls)
{
	// delete all global references
	env->DeleteGlobalRef(main_log_cls);
	env->DeleteGlobalRef(tech_log_cls);
	env->DeleteGlobalRef(main_log_obj);
	env->DeleteGlobalRef(tech_log_obj);
}

//------------------------------------------------------------------------------
