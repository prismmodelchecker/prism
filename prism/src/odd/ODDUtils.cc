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

#include "odd.h"
#include "ODDUtils.h"
#include "jnipointer.h"

//------------------------------------------------------------------------------

// cudd manager
DdManager *ddman;

//------------------------------------------------------------------------------
// cudd manager
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_odd_ODDUtils_ODD_1SetCUDDManager(JNIEnv *env, jclass cls, jlong __jlongpointer ddm)
{
	ddman = jlong_to_DdManager(ddm);
}

//------------------------------------------------------------------------------
// build odd
//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_odd_ODDUtils_ODD_1BuildODD
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer dd,	// trans matrix
jlong __jlongpointer vars,	// row vars
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
// clear odd
//------------------------------------------------------------------------------
JNIEXPORT void JNICALL Java_odd_ODDUtils_ODD_1ClearODD
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer odd)
{
	clear_odd(jlong_to_ODDNode(odd));
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

JNIEXPORT jint JNICALL Java_odd_ODDUtils_ODD_1GetIndexOfFirstFromDD
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer dd,
jlong __jlongpointer odd,
jlong __jlongpointer vars,
jint num_vars
)
{
	return get_index_of_first_from_bdd(
		ddman,
		jlong_to_DdNode(dd),
		jlong_to_DdNode_array(vars), num_vars,
		jlong_to_ODDNode(odd)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer  JNICALL Java_odd_ODDUtils_ODD_1SingleIndexToDD
(
JNIEnv *env,
jclass cls,
jint i,
jlong __jlongpointer odd,
jlong __jlongpointer vars,
jint num_vars
)
{
	return ptr_to_jlong(
		single_index_to_bdd(
			ddman,
			i,
			jlong_to_DdNode_array(vars), num_vars,
			jlong_to_ODDNode(odd)
		)
	);
}

//------------------------------------------------------------------------------
// ODDNode methods
//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_odd_ODDUtils_ODD_1GetTOff(JNIEnv *env, jclass cls, jlong __jlongpointer odd)
{
	return (jlong_to_ODDNode(odd))->toff;
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_odd_ODDUtils_ODD_1GetEOff(JNIEnv *env, jclass cls, jlong __jlongpointer odd)
{
	return (jlong_to_ODDNode(odd))->eoff;
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_odd_ODDUtils_ODD_1GetThen(JNIEnv *env, jclass cls, jlong __jlongpointer odd)
{
	return ptr_to_jlong((jlong_to_ODDNode(odd))->t);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_odd_ODDUtils_ODD_1GetElse(JNIEnv *env, jclass cls, jlong __jlongpointer odd)
{
	return ptr_to_jlong((jlong_to_ODDNode(odd))->e);
}

//------------------------------------------------------------------------------
