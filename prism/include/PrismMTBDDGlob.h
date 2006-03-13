//==============================================================================
//										
//	Copyright (c) 2002-2006, Dave Parker
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

//------------------------------------------------------------------------------

// constants - these need to match the definitions in prism/Prism.java

const int EXPORT_PLAIN = 1;
const int EXPORT_MATLAB = 2;
const int EXPORT_DOT = 3;
const int EXPORT_MRMC = 4;

const int LIN_EQ_METHOD_POWER = 1;
const int LIN_EQ_METHOD_JACOBI = 2;
const int LIN_EQ_METHOD_GAUSSSEIDEL = 3;
const int LIN_EQ_METHOD_BGAUSSSEIDEL = 4;
const int LIN_EQ_METHOD_PGAUSSSEIDEL = 5;
const int LIN_EQ_METHOD_BPGAUSSSEIDEL = 6;
const int LIN_EQ_METHOD_JOR = 7;
const int LIN_EQ_METHOD_SOR = 8;
const int LIN_EQ_METHOD_BSOR = 9;
const int LIN_EQ_METHOD_PSOR = 10;
const int LIN_EQ_METHOD_BPSOR = 11;

const int TERM_CRIT_ABSOLUTE = 1;
const int TERM_CRIT_RELATIVE = 2;

//------------------------------------------------------------------------------

// externs to global variables

// cudd manager
extern DdManager *ddman;

// numerical method stuff
extern int lin_eq_method;
extern double lin_eq_method_param;
extern int term_crit;
extern double term_crit_param;
extern int max_iters;

// export stuff
extern int export_type;
extern FILE *export_file;
extern JNIEnv *export_env;

//------------------------------------------------------------------------------

// function prototypes

void PM_PrintToMainLog(JNIEnv *env, char *str, ...);
void PM_PrintToTechLog(JNIEnv *env, char *str, ...);
void PM_SetErrorMessage(char *str, ...);
char *PM_GetErrorMessage();
int store_export_info(int type, jstring fn, JNIEnv *env);
void export_string(char *str, ...);

//------------------------------------------------------------------------------
