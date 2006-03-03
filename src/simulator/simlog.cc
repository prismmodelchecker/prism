//==============================================================================
//	
//	Copyright (c) 2004-2005, Dave Parker
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

#include "simlog.h"

#define MAX_LOG_STRING_LEN 1024

//==============================================================================
//	Local Data
//==============================================================================

// global ref to log class
static jclass main_log_cls = NULL;
// global ref to log object
static jobject main_log_obj = NULL;
// method ids for print/flush methods in log
static jmethodID main_log_mid_p = NULL;
static jmethodID main_log_mid_f = NULL;

// ref to jni env (only valid for current thread)
static JNIEnv *main_log_env = NULL;

//==============================================================================
//	Functions
//==============================================================================

void Sim_Set_Main_Log(JNIEnv *env, jobject log)
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
	// get the method ids for the print/flush method
	main_log_mid_p = env->GetMethodID(main_log_cls, "print", "(Ljava/lang/String;)V");
	main_log_mid_f = env->GetMethodID(main_log_cls, "flush", "()V");
}

//------------------------------------------------------------------------------

// cache jni pointer
// (only valid for current thread)
// (should set back to null afterwards using method below)

void Sim_Enable_Main_Log_For_Current_Thread(JNIEnv *env)
{
	main_log_env = env;
}

//------------------------------------------------------------------------------

void Sim_Disable_Main_Log()
{
	main_log_env = NULL;
}

//------------------------------------------------------------------------------

void Sim_Print_To_Main_Log(char *str, ...)
{
	va_list argptr;
	char full_string[MAX_LOG_STRING_LEN];
	
	va_start(argptr, str);
	vsnprintf(full_string, MAX_LOG_STRING_LEN, str, argptr);
	va_end(argptr);
	
	if (main_log_env)
		main_log_env->CallVoidMethod(main_log_obj, main_log_mid_p, main_log_env->NewStringUTF(full_string));
	else
		printf("%s", full_string);
}

//------------------------------------------------------------------------------

void Sim_Flush_Main_Log()
{
	if (main_log_env)
		main_log_env->CallVoidMethod(main_log_obj, main_log_mid_f);
	else
		fflush(stdout);
}

//------------------------------------------------------------------------------
