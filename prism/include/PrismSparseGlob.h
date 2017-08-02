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

#include <stdarg.h>
#include <jni.h>
#include "PrismNativeGlob.h"

//------------------------------------------------------------------------------

// externs to global variables

// cudd manager
extern DdManager *ddman;

// export stuff
extern int export_type;
extern FILE *export_file;
extern JNIEnv *export_env;

//------------------------------------------------------------------------------

// macros, function prototypes

#define logtwo(X) log((double)X)/log(2.0)
void PS_PrintToMainLog(JNIEnv *env, const char *str, ...) IS_LIKE_PRINTF(2,3);
void PS_PrintWarningToMainLog(JNIEnv *env, const char *str, ...) IS_LIKE_PRINTF(2,3);
void PS_PrintToTechLog(JNIEnv *env, const char *str, ...) IS_LIKE_PRINTF(2,3);
void PS_PrintMemoryToMainLog(JNIEnv *env, const char *before, double mem, const char *after);
void PS_SetErrorMessage(const char *str, ...) IS_LIKE_PRINTF(1,2);
char *PS_GetErrorMessage();
int store_export_info(int type, jstring fn, JNIEnv *env);
void export_string(const char *str, ...) IS_LIKE_PRINTF(1,2);
bool PS_GetFlagExportIterations();

//------------------------------------------------------------------------------
