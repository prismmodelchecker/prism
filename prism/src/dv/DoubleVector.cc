//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	Description: C++ code for double vector
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

#include "dv.h"
#include "DoubleVector.h"
#include "jnipointer.h"

#include <math.h>

//------------------------------------------------------------------------------

// cudd manager
DdManager *ddman;

//------------------------------------------------------------------------------
// cudd manager
//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_DoubleVector_DV_1SetCUDDManager
(
JNIEnv *env,
jclass cls,
jlong __pointer ddm
)
{
	ddman = jlong_to_DdManager(ddm);
}

//------------------------------------------------------------------------------
// DoubleVector methods
//------------------------------------------------------------------------------

JNIEXPORT jlong __pointer JNICALL Java_dv_DoubleVector_DV_1CreateZeroVector
(
JNIEnv *env,
jobject obj,
jint n
)
{
	double *vector = new double[n];
	int i;
	
	for (i = 0; i < n; i++) {
		vector[i] = 0;
	}
	
	return ptr_to_jlong(vector);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __pointer JNICALL Java_dv_DoubleVector_DV_1ConvertMTBDD
(
JNIEnv *env,
jobject obj,
jlong __pointer dd,
jlong __pointer vars,
jint num_vars,
jlong __pointer odd
)
{
	return ptr_to_jlong(
		mtbdd_to_double_vector(
			ddman,
			jlong_to_DdNode(dd),
			jlong_to_DdNode_array(vars), num_vars,
			jlong_to_ODDNode(odd)
		)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT jdouble JNICALL Java_dv_DoubleVector_DV_1GetElement
(
JNIEnv *env,
jobject obj,
jlong __pointer v,
jint n,
jint i
)
{
	double *vector = jlong_to_double(v);
	return (jdouble)vector[i];
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_DoubleVector_DV_1RoundOff
(
JNIEnv *env,
jobject obj,
jlong __pointer v,
jint n,
jint places
)
{
	double *vector = jlong_to_double(v);
	double trunc, d;
	int i, j;

	trunc = pow(10.0, places);
	for (i = 0; i < n; i++) {
		d = trunc * vector[i];
		j = (int)floor(d);
		if (d-j >= 0.5) j += 1;
		vector[i] = j / trunc;
	}
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_DoubleVector_DV_1SubtractFromOne
(
JNIEnv *env,
jobject obj,
jlong __pointer v,
jint n
)
{
	double *vector = jlong_to_double(v);
	int i;

	for (i = 0; i < n; i++) {
		vector[i] = 1.0 - vector[i];
	}
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_DoubleVector_DV_1Add
(
JNIEnv *env,
jobject obj,
jlong __pointer v,
jint n,
jlong __pointer v2
)
{
	double *vector = jlong_to_double(v);
	double *vector2 = jlong_to_double(v2);
	int i;

	for (i = 0; i < n; i++) {
		vector[i] += vector2[i];
	}
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_DoubleVector_DV_1TimesConstant
(
JNIEnv *env,
jobject obj,
jlong __pointer v,
jint n,
jdouble d
)
{
	double *vector = jlong_to_double(v);
	int i;

	for (i = 0; i < n; i++) {
		vector[i] *= d;
	}
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_DoubleVector_DV_1Filter
(
JNIEnv *env,
jobject obj,
jlong __pointer vector,
jlong __pointer filter,
jlong __pointer vars,
jint num_vars,
jlong __pointer odd
)
{
	filter_double_vector(
		ddman,
		jlong_to_double(vector),
		jlong_to_DdNode(filter),
		jlong_to_DdNode_array(vars), num_vars,
		jlong_to_ODDNode(odd)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_DoubleVector_DV_1Clear
(
JNIEnv *env,
jobject obj,
jlong __pointer vector
)
{
	// note we assume that this memory was created with new
	delete jlong_to_double(vector);
}

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_dv_DoubleVector_DV_1GetNNZ
(
JNIEnv *env,
jobject obj,
jlong __pointer v,
jint n
)
{
	double *vector = jlong_to_double(v);
	int i, count;
	
	count = 0;
	for (i = 0; i < n; i++) {
		if (vector[i] != 0) count++;
	}
	
	return count;
}

//------------------------------------------------------------------------------

JNIEXPORT jdouble JNICALL Java_dv_DoubleVector_DV_1FirstFromBDD
(
JNIEnv *env,
jobject obj,
jlong __pointer vector,
jlong __pointer filter,
jlong __pointer vars,
jint num_vars,
jlong __pointer odd
)
{
	return (jdouble)get_first_from_bdd(
		ddman,
		jlong_to_double(vector),
		jlong_to_DdNode(filter),
		jlong_to_DdNode_array(vars), num_vars,
		jlong_to_ODDNode(odd)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT jdouble JNICALL Java_dv_DoubleVector_DV_1MinOverBDD
(
JNIEnv *env,
jobject obj,
jlong __pointer vector,
jlong __pointer filter,
jlong __pointer vars,
jint num_vars,
jlong __pointer odd
)
{
	return (jdouble)min_double_vector_over_bdd(
		ddman,
		jlong_to_double(vector),
		jlong_to_DdNode(filter),
		jlong_to_DdNode_array(vars), num_vars,
		jlong_to_ODDNode(odd)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT jdouble JNICALL Java_dv_DoubleVector_DV_1MaxOverBDD
(
JNIEnv *env,
jobject obj,
jlong __pointer vector,
jlong __pointer filter,
jlong __pointer vars,
jint num_vars,
jlong __pointer odd
)
{
	return (jdouble)max_double_vector_over_bdd(
		ddman,
		jlong_to_double(vector),
		jlong_to_DdNode(filter),
		jlong_to_DdNode_array(vars), num_vars,
		jlong_to_ODDNode(odd)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT jdouble JNICALL Java_dv_DoubleVector_DV_1SumOverBDD
(
JNIEnv *env,
jobject obj,
jlong __pointer vector,
jlong __pointer filter,
jlong __pointer vars,
jint num_vars,
jlong __pointer odd
)
{
	return (jdouble)sum_double_vector_over_bdd(
		ddman,
		jlong_to_double(vector),
		jlong_to_DdNode(filter),
		jlong_to_DdNode_array(vars), num_vars,
		jlong_to_ODDNode(odd)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT jdouble JNICALL Java_dv_DoubleVector_DV_1SumOverMTBDD
(
JNIEnv *env,
jobject obj,
jlong __pointer vector,
jlong __pointer mult,
jlong __pointer vars,
jint num_vars,
jlong __pointer odd
)
{
	return (jdouble)sum_double_vector_over_mtbdd(
		ddman,
		jlong_to_double(vector),
		jlong_to_DdNode(mult),
		jlong_to_DdNode_array(vars), num_vars,
		jlong_to_ODDNode(odd)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __pointer JNICALL Java_dv_DoubleVector_DV_1BDDGreaterThanEquals
(
JNIEnv *env,
jobject obj,
jlong __pointer vector,
jdouble bound,
jlong __pointer vars,
jint num_vars,
jlong __pointer odd
)
{
	return ptr_to_jlong(
		double_vector_to_bdd(
			ddman,
			jlong_to_double(vector),
			DV_GREATER_THAN_EQUALS,
			bound,
			jlong_to_DdNode_array(vars), num_vars,
			jlong_to_ODDNode(odd)
		)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong JNICALL Java_dv_DoubleVector_DV_1BDDGreaterThan
(
JNIEnv *env,
jobject obj,
jlong __pointer vector,
jdouble bound,
jlong __pointer vars,
jint num_vars,
jlong __pointer odd
)
{
	return ptr_to_jlong(
		double_vector_to_bdd(
			ddman,
			jlong_to_double(vector),
			DV_GREATER_THAN,
			bound,
			jlong_to_DdNode_array(vars), num_vars,
			jlong_to_ODDNode(odd)
		)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __pointer JNICALL Java_dv_DoubleVector_DV_1BDDLessThanEquals
(
JNIEnv *env,
jobject obj,
jlong __pointer vector,
jdouble bound,
jlong __pointer vars,
jint num_vars,
jlong __pointer odd
)
{
	return ptr_to_jlong(
		double_vector_to_bdd(
			ddman,
			jlong_to_double(vector),
			DV_LESS_THAN_EQUALS,
			bound,
			jlong_to_DdNode_array(vars), num_vars,
			jlong_to_ODDNode(odd)
		)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __pointer JNICALL Java_dv_DoubleVector_DV_1BDDLessThan
(
JNIEnv *env,
jobject obj,
jlong __pointer vector,
jdouble bound,
jlong __pointer vars,
jint num_vars,
jlong __pointer odd
)
{
	return ptr_to_jlong(
		double_vector_to_bdd(
			ddman,
			jlong_to_double(vector),
			DV_LESS_THAN,
			bound,
			jlong_to_DdNode_array(vars), num_vars,
			jlong_to_ODDNode(odd)
		)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __pointer JNICALL Java_dv_DoubleVector_DV_1BDDInterval
(
JNIEnv *env,
jobject obj,
jlong __pointer vector,
jdouble lo,
jdouble hi,
jlong __pointer vars,
jint num_vars,
jlong __pointer odd
)
{
	return ptr_to_jlong(
		double_vector_to_bdd(
			ddman,
			jlong_to_double(vector),
			DV_INTERVAL,
			lo, hi,
			jlong_to_DdNode_array(vars), num_vars,
			jlong_to_ODDNode(odd)
		)
	);
}

//------------------------------------------------------------------------------
