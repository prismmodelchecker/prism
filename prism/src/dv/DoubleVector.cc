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
#include "DoubleVectorGlob.h"
#include "jnipointer.h"

#include <cmath>
#include <new>

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
jlong __jlongpointer ddm
)
{
	ddman = jlong_to_DdManager(ddm);
}

//------------------------------------------------------------------------------
// DoubleVector methods
//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_dv_DoubleVector_DV_1CreateZeroVector
(
JNIEnv *env,
jobject obj,
jint n
)
{
	double *vector;
	int i;
	try {
		vector = new double[n];
	} catch (std::bad_alloc e) {
		return 0;
	}
	for (i = 0; i < n; i++) {
		vector[i] = 0;
	}
	
	return ptr_to_jlong(vector);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_dv_DoubleVector_DV_1ConvertMTBDD
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
jlong __jlongpointer v,
jint n,
jint i
)
{
	double *vector = jlong_to_double(v);
	return (jdouble)vector[i];
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_DoubleVector_DV_1SetElement
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer v,
jint n,
jint i,
jdouble d
)
{
	double *vector = jlong_to_double(v);
	vector[i] = d;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_DoubleVector_DV_1SetAllElements
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer v,
jint n,
jdouble d
)
{
	double *vector = jlong_to_double(v);
	for(int i = 0; i < n; i++)
		vector[i] = d;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_DoubleVector_DV_1RoundOff
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer v,
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
jlong __jlongpointer v,
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
jlong __jlongpointer v,
jint n,
jlong __jlongpointer v2
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
jlong __jlongpointer v,
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

JNIEXPORT jdouble JNICALL Java_dv_DoubleVector_DV_1DotProduct
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer v,
jint n,
jlong __jlongpointer v2
)
{
	double *vector = jlong_to_double(v);
	double *vector2 = jlong_to_double(v2);
	int i;
	double d = 0.0;

	for (i = 0; i < n; i++) {
		d += vector[i] * vector2[i];
	}
	
	return d;
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_DoubleVector_DV_1Filter
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer vector,
jlong __jlongpointer filter,
jdouble d,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
)
{
	filter_double_vector(
		ddman,
		jlong_to_double(vector),
		jlong_to_DdNode(filter),
		d,
		jlong_to_DdNode_array(vars), num_vars,
		jlong_to_ODDNode(odd)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_DoubleVector_DV_1MaxMTBDD
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer vector,
jlong __jlongpointer vector2,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
)
{
	max_double_vector_mtbdd(
		ddman,
		jlong_to_double(vector),
		jlong_to_DdNode(vector2),
		jlong_to_DdNode_array(vars), num_vars,
		jlong_to_ODDNode(odd)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_dv_DoubleVector_DV_1Clear
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer vector
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
jlong __jlongpointer v,
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
jlong __jlongpointer vector,
jlong __jlongpointer filter,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
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
jlong __jlongpointer vector,
jlong __jlongpointer filter,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
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
jlong __jlongpointer vector,
jlong __jlongpointer filter,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
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

JNIEXPORT jdouble JNICALL Java_dv_DoubleVector_DV_1MaxFiniteOverBDD
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer vector,
jlong __jlongpointer filter,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
)
{
	return (jdouble)max_finite_double_vector_over_bdd(
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
jlong __jlongpointer vector,
jlong __jlongpointer filter,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
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
jlong __jlongpointer vector,
jlong __jlongpointer mult,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
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

JNIEXPORT void JNICALL Java_dv_DoubleVector_DV_1SumOverDDVars
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer vector,
jlong __jlongpointer vector2,
jlong __jlongpointer vars,
jint num_vars,
jint first_var,
jint last_var,
jlong __jlongpointer odd,
jlong __jlongpointer odd2
)
{
	sum_double_vector_over_dd_vars(
		ddman,
		jlong_to_double(vector),
		jlong_to_double(vector2),
		jlong_to_DdNode_array(vars), num_vars,
		first_var, last_var,
		jlong_to_ODDNode(odd),
		jlong_to_ODDNode(odd2)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_dv_DoubleVector_DV_1BDDGreaterThanEquals
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer vector,
jdouble bound,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
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
jlong __jlongpointer vector,
jdouble bound,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
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

JNIEXPORT jlong __jlongpointer JNICALL Java_dv_DoubleVector_DV_1BDDLessThanEquals
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer vector,
jdouble bound,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
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

JNIEXPORT jlong __jlongpointer JNICALL Java_dv_DoubleVector_DV_1BDDLessThan
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer vector,
jdouble bound,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
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

JNIEXPORT jlong __jlongpointer JNICALL Java_dv_DoubleVector_DV_1BDDInterval
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer vector,
jdouble lo,
jdouble hi,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
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

JNIEXPORT jlong __jlongpointer JNICALL Java_dv_DoubleVector_DV_1BDDCloseValueAbs
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer vector,
jdouble value,
jdouble epsilon,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
)
{
	return ptr_to_jlong(
		double_vector_to_bdd(
			ddman,
			jlong_to_double(vector),
			DV_CLOSE_ABS,
			value, epsilon,
			jlong_to_DdNode_array(vars), num_vars,
			jlong_to_ODDNode(odd)
		)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_dv_DoubleVector_DV_1BDDCloseValueRel
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer vector,
jdouble value,
jdouble epsilon,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
)
{
	return ptr_to_jlong(
		double_vector_to_bdd(
			ddman,
			jlong_to_double(vector),
			DV_CLOSE_REL,
			value, epsilon,
			jlong_to_DdNode_array(vars), num_vars,
			jlong_to_ODDNode(odd)
		)
	);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_dv_DoubleVector_DV_1ConvertToMTBDD
(
JNIEnv *env,
jobject obj,
jlong __jlongpointer vector,
jlong __jlongpointer vars,
jint num_vars,
jlong __jlongpointer odd
)
{
	return ptr_to_jlong(
		double_vector_to_mtbdd(
			ddman,
			jlong_to_double(vector),
			jlong_to_DdNode_array(vars), num_vars,
			jlong_to_ODDNode(odd)
		)
	);
}

//------------------------------------------------------------------------------
