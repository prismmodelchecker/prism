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
#include "jnipointer.h"

//------------------------------------------------------------------------------

// cudd manager
DdManager *ddman;

//------------------------------------------------------------------------------
// cudd manager
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_odd_ODDUtils_ODD_1SetCUDDManager(JNIEnv *env, jclass cls, jlong __pointer ddm)
{
	ddman = jlong_to_DdManager(ddm);
}

//------------------------------------------------------------------------------
// build odd
//------------------------------------------------------------------------------

JNIEXPORT jlong __pointer JNICALL Java_odd_ODDUtils_ODD_1BuildODD
(
JNIEnv *env,
jclass cls,
jlong __pointer dd,	// trans matrix
jlong __pointer vars,	// row vars
jint num_vars
)
{
	return ptr_to_jlong(
		build_odd(
			ddman,
			jlong_to_DdNode(dd),
			jlong_to_DdNode_array(vars), num_vars
		)
	);
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

JNIEXPORT jlong __pointer JNICALL Java_odd_ODDUtils_ODD_1GetTOff(JNIEnv *env, jclass cls, jlong __pointer odd)
{
	return (jlong_to_ODDNode(odd))->toff;
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __pointer JNICALL Java_odd_ODDUtils_ODD_1GetEOff(JNIEnv *env, jclass cls, jlong __pointer odd)
{
	return (jlong_to_ODDNode(odd))->eoff;
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __pointer JNICALL Java_odd_ODDUtils_ODD_1GetThen(JNIEnv *env, jclass cls, jlong __pointer odd)
{
	return ptr_to_jlong((jlong_to_ODDNode(odd))->t);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __pointer JNICALL Java_odd_ODDUtils_ODD_1GetElse(JNIEnv *env, jclass cls, jlong __pointer odd)
{
	return ptr_to_jlong((jlong_to_ODDNode(odd))->e);
}

//------------------------------------------------------------------------------
