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

#ifndef _Included_simulator_Logheader
#define _Included_simulator_Logheader

#include <stdarg.h>
#include <jni.h>

//=============================================================================
//	Functions
//=============================================================================

void Sim_Set_Main_Log(JNIEnv *env, jobject log);
void Sim_Enable_Main_Log_For_Current_Thread(JNIEnv *env);
void Sim_Disable_Main_Log();
void Sim_Print_To_Main_Log(char *str, ...);
void Sim_Flush_Main_Log();

#endif
