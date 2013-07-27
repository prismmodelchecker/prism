//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	Description: C++ code for integer vector
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

#include "iv.h"
#include "DoubleVectorGlob.h"
#include "IntegerVector.h"
#include "jnipointer.h"

#include <math.h>
#include <new>

//------------------------------------------------------------------------------
// IntegerVector methods
//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_dv_IntegerVector_IV_1CreateZeroVector
(
JNIEnv *env,
jobject obj,
jint n
)
{
	int *vector;
	int i;
	try {
		vector = new int[n];
	} catch (std::bad_alloc e) {
		return 0;
	}
	for (i = 0; i < n; i++) {
		vector[i] = 0;
	}
	
	return ptr_to_jlong(vector);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_dv_IntegerVector_IV_1ConvertMTBDD
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer dd,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
)
{
	return ptr_to_jlong(
		mtbdd_to_integer_vector(
			ddman,
			jlong_to_DdNode(dd),
			jlong_to_DdNode_array(vars), num_vars,
			jlong_to_ODDNode(odd)
		)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_dv_IntegerVector_IV_1GetElement
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer v,
jint n,
jint i
)
{
	int *vector = (int *) jlong_to_ptr(v);
	return (jint)vector[i];
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_IntegerVector_IV_1SetElement
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer v,
jint n,
jint i,
jint j
)
{
	int *vector = (int *) jlong_to_ptr(v);
	vector[i] = j;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_IntegerVector_IV_1SetAllElements
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer v,
jint n,
jint j
)
{
	int *vector = (int *) jlong_to_ptr(v);
	for(int i = 0; i < n; i++)
		vector[i] = j;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_IntegerVector_IV_1Clear
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer vector
)
{
	// note we assume that this memory was created with new
	if (vector) delete (int *) jlong_to_ptr(vector);
}

//------------------------------------------------------------------------------
