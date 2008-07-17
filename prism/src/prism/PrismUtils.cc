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

#include "PrismUtils.h"
#include <stdio.h>
#include <string.h>
#include <limits.h>
#include "jnipointer.h"

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_prism_PrismUtils_PU_1GetStdout(JNIEnv *env, jclass cls)
{
	return ptr_to_jlong(stdout);
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_prism_PrismUtils_PU_1OpenFile(JNIEnv *env, jclass cls, jstring filename)
{
	const char *str = env->GetStringUTFChars(filename, 0);
	FILE *fp = fopen(str, "w");
	env->ReleaseStringUTFChars(filename, str);
	return ptr_to_jlong(fp);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_prism_PrismUtils_PU_1PrintToFile(JNIEnv *env, jclass cls, jlong __jlongpointer fp, jstring s)
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


JNIEXPORT void JNICALL Java_prism_PrismUtils_PU_1FlushFile(JNIEnv *env, jclass cls, jlong __jlongpointer fp)
{
	fflush(jlong_to_FILE(fp));
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_prism_PrismUtils_PU_1CloseFile(JNIEnv *env, jclass cls, jlong __jlongpointer fp)
{
	fclose(jlong_to_FILE(fp));
}

//------------------------------------------------------------------------------

