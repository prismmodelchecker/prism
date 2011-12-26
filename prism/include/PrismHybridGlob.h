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
#include <cudd.h>
#include <dd.h>
#include "PrismNativeGlob.h"

//------------------------------------------------------------------------------

// externs to global variables

// cudd manager
extern DdManager *ddman;

// details from numerical computation which may be queried
extern double last_unif;

//------------------------------------------------------------------------------

// macros, function prototypes

#define logtwo(X) log((double)X)/log(2.0)
void PH_PrintToMainLog(JNIEnv *env, const char *str, ...);
void PH_PrintWarningToMainLog(JNIEnv *env, const char *str, ...);
void PH_PrintToTechLog(JNIEnv *env, const char *str, ...);
void PH_PrintMemoryToMainLog(JNIEnv *env, const char *before, double mem, const char *after);
void PH_SetErrorMessage(const char *str, ...);
char *PH_GetErrorMessage();

//------------------------------------------------------------------------------
