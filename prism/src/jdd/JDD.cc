//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

#include "JDD.h"
#include "JDDNode.h"
#include "JDDVars.h"
#include "jnipointer.h"

#include <cstdio>
#include <util.h>
#include <cudd.h>
#include <dd.h>

//------------------------------------------------------------------------------

static DdManager *ddman;

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_GetCUDDManager(JNIEnv *env, jclass cls)
{
	return ptr_to_jlong(ddman);
}

//==============================================================================
//
//	Wrapper functions for dd
//	
//==============================================================================

JNIEXPORT void JNICALL Java_jdd_JDD_DD_1SetOutputStream(JNIEnv *env, jclass cls, jlong __jlongpointer fp)
{
	DD_SetOutputStream(jlong_to_FILE(fp));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1GetOutputStream(JNIEnv *env, jclass cls)
{
	return ptr_to_jlong(DD_GetOutputStream());
}

//==============================================================================
//
//	Wrapper functions for dd_cudd
//	
//==============================================================================

JNIEXPORT void JNICALL Java_jdd_JDD_DD_1InitialiseCUDD__(JNIEnv *env, jclass cls)
{
	ddman = DD_InitialiseCUDD();
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_jdd_JDD_DD_1InitialiseCUDD__JD(JNIEnv *env, jclass cls, jlong max_mem, jdouble epsilon)
{
	ddman = DD_InitialiseCUDD(max_mem, epsilon);
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_jdd_JDD_DD_1SetCUDDMaxMem(JNIEnv *env, jclass cls, jlong max_mem)
{
	DD_SetCUDDMaxMem(ddman, max_mem);
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_jdd_JDD_DD_1SetCUDDEpsilon(JNIEnv *env, jclass cls, jdouble epsilon)
{
	DD_SetCUDDEpsilon(ddman, epsilon);
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_jdd_JDD_DD_1CloseDownCUDD(JNIEnv *env, jclass cls, jboolean check)
{
	DD_CloseDownCUDD(ddman, check);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1Ref(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	Cudd_Ref(jlong_to_DdNode(dd));
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1Deref(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	Cudd_RecursiveDeref(ddman, jlong_to_DdNode(dd));
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintCacheInfo(JNIEnv *env, jclass cls)
{
	DD_PrintCacheInfo(ddman);
}

//==============================================================================
//
//	Wrapper functions for dd_basics
//	
//==============================================================================

JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1Create(JNIEnv *env, jclass cls)
{
	return ptr_to_jlong(DD_Create(ddman));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1Constant(JNIEnv *env, jclass cls, jdouble value)
{
	return ptr_to_jlong(DD_Constant(ddman, value));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1PlusInfinity(JNIEnv *env, jclass cls)
{
	return ptr_to_jlong(DD_PlusInfinity(ddman));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1MinusInfinity(JNIEnv *env, jclass cls)
{
	return ptr_to_jlong(DD_MinusInfinity(ddman));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1Var(JNIEnv *env, jclass cls, jint i)
{
	return ptr_to_jlong(DD_Var(ddman, i));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1Not(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	return ptr_to_jlong(DD_Not(ddman, jlong_to_DdNode(dd)));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1Or(JNIEnv *env, jclass cls, jlong __jlongpointer dd1, jlong __jlongpointer dd2)
{
	return ptr_to_jlong(DD_Or(ddman, jlong_to_DdNode(dd1), jlong_to_DdNode(dd2)));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1And(JNIEnv *env, jclass cls, jlong __jlongpointer dd1, jlong __jlongpointer dd2)
{
	return ptr_to_jlong(DD_And(ddman, jlong_to_DdNode(dd1), jlong_to_DdNode(dd2)));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1Xor(JNIEnv *env, jclass cls, jlong __jlongpointer dd1, jlong __jlongpointer dd2)
{
	return ptr_to_jlong(DD_Xor(ddman, jlong_to_DdNode(dd1), jlong_to_DdNode(dd2)));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1Implies(JNIEnv *env, jclass cls, jlong __jlongpointer dd1, jlong __jlongpointer dd2)
{
	return ptr_to_jlong(DD_Implies(ddman, jlong_to_DdNode(dd1), jlong_to_DdNode(dd2)));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1Apply(JNIEnv *env, jclass cls, jint op, jlong __jlongpointer dd1, jlong __jlongpointer dd2)
{
	return ptr_to_jlong(DD_Apply(ddman, op, jlong_to_DdNode(dd1), jlong_to_DdNode(dd2)));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1MonadicApply(JNIEnv *env, jclass cls, jint op, jlong __jlongpointer dd)
{
	return ptr_to_jlong(DD_MonadicApply(ddman, op, jlong_to_DdNode(dd)));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1Restrict(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer cube)
{
	return ptr_to_jlong(DD_Restrict(ddman, jlong_to_DdNode(dd), jlong_to_DdNode(cube)));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1ITE(JNIEnv *env, jclass cls, jlong __jlongpointer dd1, jlong __jlongpointer dd2, jlong __jlongpointer dd3)
{
	return ptr_to_jlong(DD_ITE(ddman, jlong_to_DdNode(dd1), jlong_to_DdNode(dd2), jlong_to_DdNode(dd3)));
}

//==============================================================================
//
//	Wrapper functions for dd_vars
//	
//==============================================================================

JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1PermuteVariables(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer old_vars, jlong __jlongpointer new_vars, jint num_vars)
{
	return ptr_to_jlong(DD_PermuteVariables(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(old_vars), jlong_to_DdNode_array(new_vars), num_vars));
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1SwapVariables(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer old_vars, jlong __jlongpointer new_vars, jint num_vars)
{
	return ptr_to_jlong(DD_SwapVariables(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(old_vars), jlong_to_DdNode_array(new_vars), num_vars));
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1VariablesGreaterThan(JNIEnv *env, jclass cls, jlong __jlongpointer x_vars, jlong __jlongpointer y_vars, jint num_vars)
{
	return ptr_to_jlong(DD_VariablesGreaterThan(ddman, jlong_to_DdNode_array(x_vars), jlong_to_DdNode_array(y_vars), num_vars));
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1VariablesGreaterThanEquals(JNIEnv *env, jclass cls, jlong __jlongpointer x_vars, jlong __jlongpointer y_vars, jint num_vars)
{
	return ptr_to_jlong(DD_VariablesGreaterThanEquals(ddman, jlong_to_DdNode_array(x_vars), jlong_to_DdNode_array(y_vars), num_vars));
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1VariablesLessThan(JNIEnv *env, jclass cls, jlong __jlongpointer x_vars, jlong __jlongpointer y_vars, jint num_vars)
{
	return ptr_to_jlong(DD_VariablesLessThan(ddman, jlong_to_DdNode_array(x_vars), jlong_to_DdNode_array(y_vars), num_vars));
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1VariablesLessThanEquals(JNIEnv *env, jclass cls, jlong __jlongpointer x_vars, jlong __jlongpointer y_vars, jint num_vars)
{
	return ptr_to_jlong(DD_VariablesLessThanEquals(ddman, jlong_to_DdNode_array(x_vars), jlong_to_DdNode_array(y_vars), num_vars));
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1VariablesEquals(JNIEnv *env, jclass cls, jlong __jlongpointer x_vars, jlong __jlongpointer y_vars, jint num_vars)
{
	return ptr_to_jlong(DD_VariablesEquals(ddman, jlong_to_DdNode_array(x_vars), jlong_to_DdNode_array(y_vars), num_vars));
}

//==============================================================================
//
//	Wrapper functions for dd_abstr
//	
//==============================================================================

JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1ThereExists(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer vars, jint num_vars)
{
	return ptr_to_jlong(DD_ThereExists(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(vars), num_vars));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1ForAll(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer vars, jint num_vars)
{
	return ptr_to_jlong(DD_ForAll(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(vars), num_vars));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1SumAbstract(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer vars, jint num_vars)
{
	return ptr_to_jlong(DD_SumAbstract(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(vars), num_vars));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1ProductAbstract(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer vars, jint num_vars)
{
	return ptr_to_jlong(DD_ProductAbstract(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(vars), num_vars));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1MinAbstract(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer vars, jint num_vars)
{
	return ptr_to_jlong(DD_MinAbstract(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(vars), num_vars));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1MaxAbstract(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer vars, jint num_vars)
{
	return ptr_to_jlong(DD_MaxAbstract(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(vars), num_vars));
}

//==============================================================================
//
//	Wrapper functions for dd_term
//	
//==============================================================================

JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1GreaterThan(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jdouble threshold)
{
	return ptr_to_jlong(DD_GreaterThan(ddman, jlong_to_DdNode(dd), threshold));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1GreaterThanEquals(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jdouble threshold)
{
	return ptr_to_jlong(DD_GreaterThanEquals(ddman, jlong_to_DdNode(dd), threshold));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1LessThan(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jdouble threshold)
{
	return ptr_to_jlong(DD_LessThan(ddman, jlong_to_DdNode(dd), threshold));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1LessThanEquals(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jdouble threshold)
{
	return ptr_to_jlong(DD_LessThanEquals(ddman, jlong_to_DdNode(dd), threshold));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1Equals(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jdouble value)
{
	return ptr_to_jlong(DD_Equals(ddman, jlong_to_DdNode(dd), value));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1Interval(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jdouble lower, jdouble upper)
{
	return ptr_to_jlong(DD_Interval(ddman, jlong_to_DdNode(dd), lower, upper));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1RoundOff(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jint places)
{
	return ptr_to_jlong(DD_RoundOff(ddman, jlong_to_DdNode(dd), places));
}

//------------------------------------------------------------------------------


JNIEXPORT jboolean JNICALL Java_jdd_JDD_DD_1EqualSupNorm(JNIEnv *env, jclass cls, jlong __jlongpointer dd1, jlong __jlongpointer dd2, jdouble epsilon)
{
	return DD_EqualSupNorm(ddman, jlong_to_DdNode(dd1), jlong_to_DdNode(dd2), epsilon);
}

//------------------------------------------------------------------------------


JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1FindMin(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	return DD_FindMin(ddman, jlong_to_DdNode(dd));
}

//------------------------------------------------------------------------------


JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1FindMinPositive(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	return DD_FindMinPositive(ddman, jlong_to_DdNode(dd));
}

//------------------------------------------------------------------------------


JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1FindMax(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	return DD_FindMax(ddman, jlong_to_DdNode(dd));
}

//------------------------------------------------------------------------------

JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1FindMaxFinite(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	return DD_FindMaxFinite(ddman, jlong_to_DdNode(dd));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1RestrictToFirst(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer vars, jint num_vars)
{
	return ptr_to_jlong(DD_RestrictToFirst(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(vars), num_vars));
}

//------------------------------------------------------------------------------


JNIEXPORT jboolean JNICALL Java_jdd_JDD_DD_1IsZeroOneMTBDD(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	return DD_IsZeroOneMTBDD(ddman, jlong_to_DdNode(dd));
}


//==============================================================================
//
//	Wrapper functions for dd_info
//	
//==============================================================================

JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1GetNumNodes(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	return DD_GetNumNodes(ddman, jlong_to_DdNode(dd));
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1GetNumTerminals(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	return DD_GetNumTerminals(ddman, jlong_to_DdNode(dd));
}

//------------------------------------------------------------------------------


JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1GetNumMinterms(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jint num_vars)
{
	return DD_GetNumMinterms(ddman, jlong_to_DdNode(dd), num_vars);
}

//------------------------------------------------------------------------------


JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1GetNumPaths(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	return DD_GetNumPaths(ddman, jlong_to_DdNode(dd));
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintInfo(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jint num_vars)
{
	DD_PrintInfo(ddman, jlong_to_DdNode(dd), num_vars);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintInfoBrief(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jint num_vars)
{
	DD_PrintInfoBrief(ddman, jlong_to_DdNode(dd), num_vars);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintSupport(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	DD_PrintSupport(ddman, jlong_to_DdNode(dd));
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintSupportNames(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jobject var_names)
{
	// If no var names passed in, don't use them
	if (!var_names) {
		DD_PrintSupport(ddman, jlong_to_DdNode(dd));
	}
	// Otherwise, need to convert Java array to C array first
	else {
		int i;
		jint size;
		jclass vn_cls;
		jmethodID vn_mid;
		const char **names;
		jstring *names_jstrings;
		// get size of vector of names
		vn_cls = env->GetObjectClass(var_names);
		vn_mid = env->GetMethodID(vn_cls, "size", "()I");
		if (vn_mid == 0) {
			return;
		}
		size = env->CallIntMethod(var_names,vn_mid);
		// put names from vector into array
		names = new const char*[size];
		names_jstrings = new jstring[size];
		vn_mid = env->GetMethodID(vn_cls, "get", "(I)Ljava/lang/Object;");
		if (vn_mid == 0) {
			return;
		}
		for (i = 0; i < size; i++) {
			names_jstrings[i] = (jstring)env->CallObjectMethod(var_names, vn_mid, i);
			names[i] = env->GetStringUTFChars(names_jstrings[i], 0);
		}
		// call the function
		DD_PrintSupportNames(ddman, jlong_to_DdNode(dd), (char **)names);
		// release memory
		for (i = 0; i < size; i++) {
			env->ReleaseStringUTFChars(names_jstrings[i], names[i]);
		}
		delete[] names;
		delete[] names_jstrings;
	}
}

//------------------------------------------------------------------------------


JNIEXPORT jlong JNICALL Java_jdd_JDD_DD_1GetSupport(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	return ptr_to_jlong(DD_GetSupport(ddman, jlong_to_DdNode(dd)));
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintTerminals(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	DD_PrintTerminals(ddman, jlong_to_DdNode(dd));
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintTerminalsAndNumbers(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jint num_vars)
{
	DD_PrintTerminalsAndNumbers(ddman, jlong_to_DdNode(dd), num_vars);
}

//==============================================================================
//
//	Wrapper functions for dd_matrix
//	
//==============================================================================

JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1SetVectorElement(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer vars, jint num_vars, jlong index, jdouble value)
{
	return ptr_to_jlong(DD_SetVectorElement(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(vars), num_vars, index, value));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1SetMatrixElement(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer rvars, jint num_rvars, jlong __jlongpointer cvars, jint num_cvars, jlong rindex, jlong cindex, jdouble value)
{
	return ptr_to_jlong(DD_SetMatrixElement(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(rvars), num_rvars, jlong_to_DdNode_array(cvars), num_cvars, rindex, cindex, value));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1Set3DMatrixElement(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer rvars, jint num_rvars, jlong __jlongpointer cvars, jint num_cvars, jlong __jlongpointer lvars, jint num_lvars, jlong rindex, jlong cindex, jlong lindex, jdouble value)
{
	return ptr_to_jlong(DD_Set3DMatrixElement(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(rvars), num_rvars, jlong_to_DdNode_array(cvars), num_cvars, jlong_to_DdNode_array(lvars), num_lvars, rindex, cindex, lindex, value));
}

//------------------------------------------------------------------------------


JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1GetVectorElement(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer vars, jint num_vars, jlong index)
{
	return DD_GetVectorElement(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(vars), num_vars, index);
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1Identity(JNIEnv *env, jclass cls, jlong __jlongpointer rvars, jlong __jlongpointer cvars, jint num_vars)
{
	return ptr_to_jlong(DD_Identity(ddman, jlong_to_DdNode_array(rvars), jlong_to_DdNode_array(cvars), num_vars));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1Transpose(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer rvars, jlong __jlongpointer cvars, jint num_vars, jint lvars, jint num_lvars)
{
	return ptr_to_jlong(DD_Transpose(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(rvars), jlong_to_DdNode_array(cvars), num_vars));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDD_DD_1MatrixMultiply(JNIEnv *env, jclass cls, jlong __jlongpointer dd1, jlong __jlongpointer dd2, jlong __jlongpointer vars, jint num_vars, jint method)
{
	return ptr_to_jlong(DD_MatrixMultiply(ddman, jlong_to_DdNode(dd1), jlong_to_DdNode(dd2), jlong_to_DdNode_array(vars), num_vars, method));
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintVector(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer vars, jint num_vars, jint acc)
{
	DD_PrintVector(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(vars), num_vars, acc);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintMatrix(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer rvars, jint num_rvars, jlong __jlongpointer cvars, jint num_cvars, jint acc)
{
	DD_PrintMatrix(ddman, jlong_to_DdNode(dd), jlong_to_DdNode_array(rvars), num_rvars, jlong_to_DdNode_array(cvars), num_cvars, acc);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintVectorFiltered(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer filter, jlong __jlongpointer vars, jint num_vars, jint acc)
{
	DD_PrintVectorFiltered(ddman, jlong_to_DdNode(dd), jlong_to_DdNode(filter), jlong_to_DdNode_array(vars), num_vars, acc);
}

//==============================================================================
//
//	Wrapper functions for dd_export
//	
//==============================================================================

JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportDDToDotFile(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jstring filename)
{
	const char *str = env->GetStringUTFChars(filename, 0);
	DD_ExportDDToDotFile(ddman, jlong_to_DdNode(dd), (char *)str);
	env->ReleaseStringUTFChars(filename, str);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportDDToDotFileLabelled(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jstring filename, jobject var_names)
{
	int i;
	jint size;
	jclass vn_cls;
	jmethodID vn_mid;
	const char **names;
	jstring *names_strings;
	const char *filenamestr;

	// get size of vector of names
	vn_cls = env->GetObjectClass(var_names);
	vn_mid = env->GetMethodID(vn_cls, "size", "()I");
	if (vn_mid == 0) {
		return;
	}
	size = env->CallIntMethod(var_names,vn_mid);
	// put names from vector into array
	names = new const char*[size];
	names_strings = new jstring[size];
	vn_mid = env->GetMethodID(vn_cls, "elementAt", "(I)Ljava/lang/Object;");
	if (vn_mid == 0) {
		return;
	}
	for (i = 0; i < size; i++) {
		names_strings[i] = (jstring)env->CallObjectMethod(var_names, vn_mid, i);
		names[i] = env->GetStringUTFChars(names_strings[i], 0);
	}

	// get filename string
	filenamestr = env->GetStringUTFChars(filename, 0);

	// call dd_export... function
	DD_ExportDDToDotFileLabelled(ddman, jlong_to_DdNode(dd), (char *)filenamestr, (char **)names);

	// release memory
	for (i = 0; i < size; i++) {
		env->ReleaseStringUTFChars(names_strings[i], names[i]);
	}
	env->ReleaseStringUTFChars(filename, filenamestr);
	delete[] names;
	delete[] names_strings;
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportMatrixToPPFile(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer rvars, jint num_rvars, jlong __jlongpointer cvars, jint num_cvars, jstring filename)
{
	const char *str = env->GetStringUTFChars(filename, 0);
	DD_ExportMatrixToPPFile(
		ddman, jlong_to_DdNode(dd),
		jlong_to_DdNode_array(rvars), num_rvars,
		jlong_to_DdNode_array(cvars), num_cvars,
		(char *)str
	);
	env->ReleaseStringUTFChars(filename, str);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1Export3dMatrixToPPFile(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer rvars, jint num_rvars, jlong __jlongpointer cvars, jint num_cvars, jlong __jlongpointer nvars, jint num_nvars, jstring filename)
{
	const char *str = env->GetStringUTFChars(filename, 0);
	DD_Export3dMatrixToPPFile(
		ddman, jlong_to_DdNode(dd),
		jlong_to_DdNode_array(rvars), num_rvars,
		jlong_to_DdNode_array(cvars), num_cvars,
		jlong_to_DdNode_array(nvars), num_nvars,
		(char *)str
	);
	env->ReleaseStringUTFChars(filename, str);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportMatrixToMatlabFile(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer rvars, jint num_rvars, jlong __jlongpointer cvars, jint num_cvars, jstring name, jstring filename)
{
	const char *str1 = env->GetStringUTFChars(name, 0);
	const char *str2 = env->GetStringUTFChars(filename, 0);
	DD_ExportMatrixToMatlabFile(
		ddman, jlong_to_DdNode(dd),
		jlong_to_DdNode_array(rvars), num_rvars,
		jlong_to_DdNode_array(cvars), num_cvars,
		(char *)str1, (char *)str2
	);
	env->ReleaseStringUTFChars(name, str1);
	env->ReleaseStringUTFChars(filename, str2);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportMatrixToSpyFile(JNIEnv *env, jclass cls, jlong __jlongpointer dd, jlong __jlongpointer rvars, jint num_rvars, jlong __jlongpointer cvars, jint num_cvars, jint depth, jstring filename)
{
	const char *str = env->GetStringUTFChars(filename, 0);
	DD_ExportMatrixToSpyFile(
		ddman, jlong_to_DdNode(dd),
		jlong_to_DdNode_array(rvars), num_rvars,
		jlong_to_DdNode_array(cvars), num_cvars,
		depth, (char *)str
	);
	env->ReleaseStringUTFChars(filename, str);
}

//==============================================================================
//
//	Functions for JDDNode class
//	
//==============================================================================

JNIEXPORT jboolean JNICALL Java_jdd_JDDNode_DDN_1IsConstant(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	return Cudd_IsConstant(jlong_to_DdNode(dd));
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDDNode_DDN_1GetIndex(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	return (jlong_to_DdNode(dd))->index;
}

//------------------------------------------------------------------------------


JNIEXPORT jdouble JNICALL Java_jdd_JDDNode_DDN_1GetValue(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	return Cudd_V(jlong_to_DdNode(dd));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDDNode_DDN_1GetThen(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	DdNode *node = jlong_to_DdNode(dd);
	if (Cudd_IsConstant(node)) return ptr_to_jlong(NULL);
	return ptr_to_jlong(Cudd_T(node));
}

//------------------------------------------------------------------------------


JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDDNode_DDN_1GetElse(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	DdNode *node = jlong_to_DdNode(dd);
	if (Cudd_IsConstant(node)) return ptr_to_jlong(NULL);
	return ptr_to_jlong(Cudd_E(node));
}

//==============================================================================
//
//	Functions for JDDVars class
//	
//==============================================================================

JNIEXPORT jlong __jlongpointer JNICALL Java_jdd_JDDVars_DDV_1BuildArray(JNIEnv *env, jobject obj)
{
	jclass cls;
	jmethodID mid;
	DdNode **arr;
	int i, n;
	
	cls = env->GetObjectClass(obj);
	mid = env->GetMethodID(cls, "getNumVars", "()I");
	if (mid == 0) {
		return 0;
	}
	n = env->CallIntMethod(obj, mid);
	arr = new DdNode*[n];
	mid = env->GetMethodID(cls, "getVarPtr", "(I)J");
	if (mid == 0) {
		delete[] arr;
		return 0;
	}
	for (i = 0; i < n; i++) {
		arr[i] = jlong_to_DdNode(env->CallLongMethod(obj, mid, i));
	}
	
	return ptr_to_jlong(arr);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDDVars_DDV_1FreeArray(JNIEnv *env, jobject obj, jlong __jlongpointer arr)
{
	delete[] jlong_to_DdNode_array(arr);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDDVars_DDV_1GetIndex(JNIEnv *env, jobject obj, jlong __jlongpointer dd)
{
	return (jlong_to_DdNode(dd))->index;
}

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_jdd_JDD_DebugJDD_1GetRefCount(JNIEnv *env, jclass cls, jlong __jlongpointer dd)
{
	return (jlong_to_DdNode(dd))->ref;
}

//------------------------------------------------------------------------------

JNIEXPORT jlongArray JNICALL Java_jdd_JDD_DebugJDD_1GetExternalRefCounts(JNIEnv *env, jclass cls)
{
	// get external reference counts and return as a long[] Java array
	// the entries of the array will be alternating ptr / count values
	std::map<DdNode*, int> external_refs;
	DD_GetExternalRefCounts(ddman, external_refs);
	std::size_t v_size = 2 * external_refs.size();

	jlong* v = new jlong[v_size];
	std::size_t i = 0;
	for (std::map<DdNode*,int>::iterator it = external_refs.begin();
	     it != external_refs.end();
	     ++it) {
		DdNode *node = it->first;
		int refs = it->second;

		v[i++] = ptr_to_jlong(node);
		v[i++] = refs;
	}

	// printf("v_size = %lu\n", v_size);
	jlongArray result = env->NewLongArray(v_size);
	env->SetLongArrayRegion(result, 0, v_size, v);
	delete[] v;

	return result;
}
//------------------------------------------------------------------------------

JNIEXPORT jboolean JNICALL Java_jdd_JDD_DD_1GetErrorFlag(JNIEnv *env, jclass cls)
{
	return DD_GetErrorFlag(ddman);
}
