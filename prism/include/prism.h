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

#ifndef PRISM_H
#define PRISM_H

//------------------------------------------------------------------------------
#include <jni.h>

// Flags for building Windows DLLs
#ifdef __MINGW32__
	#define EXPORT __declspec(dllexport)
#else
	#define EXPORT
#endif

// Fox-Glynn wieghts struct
typedef struct FoxGlynnWeights
{
	long left;
	long right;
	double total_weight;
	double *weights;
} FoxGlynnWeights;

// Function prototypes
EXPORT long get_real_time(JNIEnv *env);
EXPORT void get_string_array_from_java(JNIEnv *env, jobject strings_list, jstring *&strings_jstrings, const char **&strings, int &size);
EXPORT void release_string_array_from_java(JNIEnv *env, jstring *strings_jstrings, const char **strings, jint size);
EXPORT FoxGlynnWeights fox_glynn(double q_tmax, double underflow, double overflow, double accuracy);

// Global constants
// Delay between occasional updates for slow processes, e.g. numerical solution (milliseconds)
const int UPDATE_DELAY = 5000;

//------------------------------------------------------------------------------

#endif

//------------------------------------------------------------------------------
