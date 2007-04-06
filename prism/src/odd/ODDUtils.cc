//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

#include "odd.h"
#include "ODDUtils.h"

//------------------------------------------------------------------------------

// cudd manager
DdManager *ddman;

//------------------------------------------------------------------------------
// cudd manager
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_odd_ODDUtils_ODD_1SetCUDDManager(JNIEnv *env, jclass cls, jint ddm)
{
	ddman = (DdManager *)ddm;
}

//------------------------------------------------------------------------------
// build odd
//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_odd_ODDUtils_ODD_1BuildODD
(
JNIEnv *env,
jclass cls,
jint dd,		// trans matrix
jint vars,		// row vars
jint num_vars
)
{
	ODDNode *odd;
	
	// build odd
	odd = build_odd(ddman, (DdNode *)dd, (DdNode **)vars, num_vars);
	
	return (int)odd;
}

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_odd_ODDUtils_ODD_1GetNumODDNodes
(
JNIEnv *env,
jclass cls
)
{
	return get_num_odd_nodes();
}

//------------------------------------------------------------------------------
// ODDNode methods
//------------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_odd_ODDUtils_ODD_1GetTOff(JNIEnv *env, jclass cls, jint odd)
{
	return ((ODDNode *)odd)->toff;
}

//------------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_odd_ODDUtils_ODD_1GetEOff(JNIEnv *env, jclass cls, jint odd)
{
	return ((ODDNode *)odd)->eoff;
}

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_odd_ODDUtils_ODD_1GetThen(JNIEnv *env, jclass cls, jint odd)
{
	return (int)(((ODDNode *)odd)->t);
}

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_odd_ODDUtils_ODD_1GetElse(JNIEnv *env, jclass cls, jint odd)
{
	return (int)(((ODDNode *)odd)->e);
}

//------------------------------------------------------------------------------
