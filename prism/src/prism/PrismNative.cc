//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

#include "PrismNative.h"
#include <cstdio>
#include <cstring>
#include <limits>
#include <locale.h>
#include <unistd.h>
#include "PrismNativeGlob.h"
#include "jnipointer.h"

// Global ref to prism class/object
EXPORT jclass prism_cls = NULL;
EXPORT jobject prism_obj = NULL;

// Options:
// numerical method stuff
EXPORT int lin_eq_method;
EXPORT double lin_eq_method_param;
EXPORT int term_crit;
EXPORT double term_crit_param;
EXPORT int max_iters;
// use "compact modified" sparse matrix storage?
EXPORT bool compact;
// sparse bits info
EXPORT int sb_max_mem;
EXPORT int num_sb_levels;
// hybrid sor info
EXPORT int sor_max_mem;
EXPORT int num_sor_levels;
// use steady-state detection for transient computation?
EXPORT bool do_ss_detect;
// adversary export mode
EXPORT int export_adv;
// adversary export filename
EXPORT const char *export_adv_filename;
// export iterations filename
EXPORT const char *export_iterations_filename = "iterations.html";

//------------------------------------------------------------------------------
// Prism object
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetPrism(JNIEnv *env, jclass cls, jobject prism)
{
	// if prism has been set previously, we need to delete existing global refs first
	if (prism_obj != NULL) {
		env->DeleteGlobalRef(prism_cls);
		prism_cls = NULL;
		env->DeleteGlobalRef(prism_obj);
		prism_obj = NULL;
	}
	
	// make a global reference to the object
	prism_obj = env->NewGlobalRef(prism);
	// get the class and make a global reference to it
	prism_cls = (jclass)env->NewGlobalRef(env->GetObjectClass(prism_obj));
	
	// We should also set the locale, to ensure consistent display of numerical values
	// (e.g. 0.5 not 0,5). This seems as good a place as any to do it.
	setlocale(LC_NUMERIC, "C");
}

//------------------------------------------------------------------------------
// Set methods for options in native code
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetCompact(JNIEnv *env, jclass cls, jboolean b)
{
	compact = b;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetLinEqMethod(JNIEnv *env, jclass cls, jint i)
{
	lin_eq_method = i;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetLinEqMethodParam(JNIEnv *env, jclass cls, jdouble d)
{
	lin_eq_method_param = d;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetTermCrit(JNIEnv *env, jclass cls, jint i)
{
	term_crit = i;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetTermCritParam(JNIEnv *env, jclass cls, jdouble d)
{
	term_crit_param = d;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetMaxIters(JNIEnv *env, jclass cls, jint i)
{
	max_iters = i;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetSBMaxMem(JNIEnv *env, jclass cls, jint sbmm)
{
	sb_max_mem = sbmm;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetNumSBLevels(JNIEnv *env, jclass cls, jint nsbl)
{
	num_sb_levels = nsbl;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetSORMaxMem(JNIEnv *env, jclass cls, jint smm)
{
	sor_max_mem = smm;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetNumSORLevels(JNIEnv *env, jclass cls, jint nsl)
{
	num_sor_levels = nsl;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetDoSSDetect(JNIEnv *env, jclass cls, jboolean b)
{
	do_ss_detect = b;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetExportAdv(JNIEnv *env, jclass cls, jint i)
{
	export_adv = i;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetExportAdvFilename(JNIEnv *env, jclass cls, jstring fn)
{
	if (fn) {
		export_adv_filename = env->GetStringUTFChars(fn, 0);
		// This never gets released. Oops.
	} else {
		export_adv_filename = NULL;
	}
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1SetDefaultExportIterationsFilename(JNIEnv *env, jclass cls, jstring fn)
{
	if (fn) {
		export_iterations_filename = env->GetStringUTFChars(fn, 0);
		// This never gets released. Oops.
	} else {
		export_iterations_filename = NULL;
	}
}

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_prism_PrismNative_PN_1SetWorkingDirectory(JNIEnv *env, jclass cls, jstring dn) {
	int rv;
	const char* dirname = env->GetStringUTFChars(dn, 0);
	rv = chdir(dirname);
	env->ReleaseStringUTFChars(dn, dirname);
	return rv;
}

//------------------------------------------------------------------------------
// Some miscellaneous native methods
//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_prism_PrismNative_PN_1GetStdout(JNIEnv *env, jclass cls)
{
#ifdef _WIN32
	setvbuf(stdout, NULL, _IONBF, 0);
#else
	setvbuf(stdout, NULL, _IOLBF, 1024);
#endif
	return ptr_to_jlong(stdout);
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_prism_PrismNative_PN_1OpenFile(JNIEnv *env, jclass cls, jstring filename)
{
	const char *str = env->GetStringUTFChars(filename, 0);
	FILE *fp = fopen(str, "w");
	env->ReleaseStringUTFChars(filename, str);
	return ptr_to_jlong(fp);
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_prism_PrismNative_PN_1OpenFileAppend(JNIEnv *env, jclass cls, jstring filename)
{
	const char *str = env->GetStringUTFChars(filename, 0);
	FILE *fp = fopen(str, "a");
	env->ReleaseStringUTFChars(filename, str);
	return ptr_to_jlong(fp);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1PrintToFile(JNIEnv *env, jclass cls, jlong __jlongpointer fp, jstring s)
{
	const char *str = env->GetStringUTFChars(s, 0);
	// note: use fwrite not fprintf here because there is no formatting to do
	// (and in fact formatting has probably already been done so mustn't do it again,
	//  especially if we want to print % characters reliably)
	fwrite(str, sizeof(char), strlen(str), jlong_to_FILE(fp));
	//fprintf(jlong_to_FILE(fp), str);
	env->ReleaseStringUTFChars(s, str);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1FlushFile(JNIEnv *env, jclass cls, jlong __jlongpointer fp)
{
	fflush(jlong_to_FILE(fp));
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1CloseFile(JNIEnv *env, jclass cls, jlong __jlongpointer fp)
{
	fclose(jlong_to_FILE(fp));
}

//------------------------------------------------------------------------------
// tidy up
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_prism_PrismNative_PN_1FreeGlobalRefs(JNIEnv *env, jclass cls)
{
	// delete all global references
	env->DeleteGlobalRef(prism_cls);
	prism_cls = NULL;
	env->DeleteGlobalRef(prism_obj);
	prism_obj = NULL;
}

//------------------------------------------------------------------------------

