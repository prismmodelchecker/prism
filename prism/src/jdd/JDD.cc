//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker
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
#include <stdio.h>
#include <util.h>
#include <cudd.h>
#include <dd.h>

//------------------------------------------------------------------------------


static DdManager *ddman;

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_GetCUDDManager(JNIEnv *env, jclass cls)
{
	return (int)ddman;
}

//==============================================================================
//
//	Wrapper functions for dd
//	
//==============================================================================

JNIEXPORT void JNICALL Java_jdd_JDD_DD_1SetOutputStream(JNIEnv *env, jclass cls, jint fp)
{
	DD_SetOutputStream((FILE *)fp);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1SetOutputStream(JNIEnv *env, jclass cls)
{
	return (jint)DD_GetOutputStream();
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


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1Ref(JNIEnv *env, jclass cls, jint dd)
{
	Cudd_Ref((DdNode *)dd);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1Deref(JNIEnv *env, jclass cls, jint dd)
{
	Cudd_RecursiveDeref(ddman, (DdNode *)dd);
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

JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Create(JNIEnv *env, jclass cls)
{
	return (int)DD_Create(ddman);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Constant(JNIEnv *env, jclass cls, jdouble value)
{
	return (int)DD_Constant(ddman, value);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1PlusInfinity(JNIEnv *env, jclass cls)
{
	return (int)DD_PlusInfinity(ddman);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1MinusInfinity(JNIEnv *env, jclass cls)
{
	return (int)DD_MinusInfinity(ddman);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Var(JNIEnv *env, jclass cls, jint i)
{
	return (int)DD_Var(ddman, i);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Not(JNIEnv *env, jclass cls, jint dd)
{
	return (int)DD_Not(ddman, (DdNode *)dd);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Or(JNIEnv *env, jclass cls, jint dd1, jint dd2)
{
	return (int)DD_Or(ddman, (DdNode *)dd1, (DdNode *)dd2);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1And(JNIEnv *env, jclass cls, jint dd1, jint dd2)
{
	return (int)DD_And(ddman, (DdNode *)dd1, (DdNode *)dd2);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Xor(JNIEnv *env, jclass cls, jint dd1, jint dd2)
{
	return (int)DD_Xor(ddman, (DdNode *)dd1, (DdNode *)dd2);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Implies(JNIEnv *env, jclass cls, jint dd1, jint dd2)
{
	return (int)DD_Implies(ddman, (DdNode *)dd1, (DdNode *)dd2);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Apply(JNIEnv *env, jclass cls, jint op, jint dd1, jint dd2)
{
	return (int)DD_Apply(ddman, op, (DdNode *)dd1, (DdNode *)dd2);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1MonadicApply(JNIEnv *env, jclass cls, jint op, jint dd)
{
	return (int)DD_MonadicApply(ddman, op, (DdNode *)dd);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Restrict(JNIEnv *env, jclass cls, jint dd, jint cube)
{
	return (int)DD_Restrict(ddman, (DdNode *)dd, (DdNode *)cube);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1ITE(JNIEnv *env, jclass cls, jint dd1, jint dd2, jint dd3)
{
	return (int)DD_ITE(ddman, (DdNode *)dd1, (DdNode *)dd2, (DdNode *)dd3);
}

//==============================================================================
//
//	Wrapper functions for dd_vars
//	
//==============================================================================

JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1PermuteVariables(JNIEnv *env, jclass cls, jint dd, jint old_vars, jint new_vars, jint num_vars)
{
	return (int)DD_PermuteVariables(ddman, (DdNode *)dd, (DdNode **)old_vars, (DdNode **)new_vars, num_vars);
}

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1SwapVariables(JNIEnv *env, jclass cls, jint dd, jint old_vars, jint new_vars, jint num_vars)
{
	return (int)DD_SwapVariables(ddman, (DdNode *)dd, (DdNode **)old_vars, (DdNode **)new_vars, num_vars);
}

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1VariablesGreaterThan(JNIEnv *env, jclass cls, jint x_vars, jint y_vars, jint num_vars)
{
	return (int)DD_VariablesGreaterThan(ddman, (DdNode **)x_vars, (DdNode **)y_vars, num_vars);
}

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1VariablesGreaterThanEquals(JNIEnv *env, jclass cls, jint x_vars, jint y_vars, jint num_vars)
{
	return (int)DD_VariablesGreaterThanEquals(ddman, (DdNode **)x_vars, (DdNode **)y_vars, num_vars);
}

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1VariablesLessThan(JNIEnv *env, jclass cls, jint x_vars, jint y_vars, jint num_vars)
{
	return (int)DD_VariablesLessThan(ddman, (DdNode **)x_vars, (DdNode **)y_vars, num_vars);
}

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1VariablesLessThanEquals(JNIEnv *env, jclass cls, jint x_vars, jint y_vars, jint num_vars)
{
	return (int)DD_VariablesLessThanEquals(ddman, (DdNode **)x_vars, (DdNode **)y_vars, num_vars);
}

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1VariablesEquals(JNIEnv *env, jclass cls, jint x_vars, jint y_vars, jint num_vars)
{
	return (int)DD_VariablesEquals(ddman, (DdNode **)x_vars, (DdNode **)y_vars, num_vars);
}

//==============================================================================
//
//	Wrapper functions for dd_abstr
//	
//==============================================================================

JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1ThereExists(JNIEnv *env, jclass cls, jint dd, jint vars, jint num_vars)
{
	return (int)DD_ThereExists(ddman, (DdNode *)dd, (DdNode **)vars, num_vars);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1ForAll(JNIEnv *env, jclass cls, jint dd, jint vars, jint num_vars)
{
	return (int)DD_ForAll(ddman, (DdNode *)dd, (DdNode **)vars, num_vars);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1SumAbstract(JNIEnv *env, jclass cls, jint dd, jint vars, jint num_vars)
{
	return (int)DD_SumAbstract(ddman, (DdNode *)dd, (DdNode **)vars, num_vars);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1ProductAbstract(JNIEnv *env, jclass cls, jint dd, jint vars, jint num_vars)
{
	return (int)DD_ProductAbstract(ddman, (DdNode *)dd, (DdNode **)vars, num_vars);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1MinAbstract(JNIEnv *env, jclass cls, jint dd, jint vars, jint num_vars)
{
	return (int)DD_MinAbstract(ddman, (DdNode *)dd, (DdNode **)vars, num_vars);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1MaxAbstract(JNIEnv *env, jclass cls, jint dd, jint vars, jint num_vars)
{
	return (int)DD_MaxAbstract(ddman, (DdNode *)dd, (DdNode **)vars, num_vars);
}

//==============================================================================
//
//	Wrapper functions for dd_term
//	
//==============================================================================

JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1GreaterThan(JNIEnv *env, jclass cls, jint dd, jdouble threshold)
{
	return (int)DD_GreaterThan(ddman, (DdNode *)dd, threshold);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1GreaterThanEquals(JNIEnv *env, jclass cls, jint dd, jdouble threshold)
{
	return (int)DD_GreaterThanEquals(ddman, (DdNode *)dd, threshold);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1LessThan(JNIEnv *env, jclass cls, jint dd, jdouble threshold)
{
	return (int)DD_LessThan(ddman, (DdNode *)dd, threshold);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1LessThanEquals(JNIEnv *env, jclass cls, jint dd, jdouble threshold)
{
	return (int)DD_LessThanEquals(ddman, (DdNode *)dd, threshold);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Equals(JNIEnv *env, jclass cls, jint dd, jdouble value)
{
	return (int)DD_Equals(ddman, (DdNode *)dd, value);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Interval(JNIEnv *env, jclass cls, jint dd, jdouble lower, jdouble upper)
{
	return (int)DD_Interval(ddman, (DdNode *)dd, lower, upper);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1RoundOff(JNIEnv *env, jclass cls, jint dd, jint places)
{
	return (int)DD_RoundOff(ddman, (DdNode *)dd, places);
}

//------------------------------------------------------------------------------


JNIEXPORT jboolean JNICALL Java_jdd_JDD_DD_1EqualSupNorm(JNIEnv *env, jclass cls, jint dd1, jint dd2, jdouble epsilon)
{
	return DD_EqualSupNorm(ddman, (DdNode *)dd1, (DdNode *)dd2, epsilon);
}

//------------------------------------------------------------------------------


JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1FindMin(JNIEnv *env, jclass cls, jint dd)
{
	return DD_FindMin(ddman, (DdNode *)dd);
}

//------------------------------------------------------------------------------


JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1FindMax(JNIEnv *env, jclass cls, jint dd)
{
	return DD_FindMax(ddman, (DdNode *)dd);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1RestrictToFirst(JNIEnv *env, jclass cls, jint dd, jint vars, jint num_vars)
{
	return (int)DD_RestrictToFirst(ddman, (DdNode *)dd, (DdNode **)vars, num_vars);
}

//==============================================================================
//
//	Wrapper functions for dd_info
//	
//==============================================================================

JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1GetNumNodes(JNIEnv *env, jclass cls, jint dd)
{
	return DD_GetNumNodes(ddman, (DdNode *)dd);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1GetNumTerminals(JNIEnv *env, jclass cls, jint dd)
{
	return DD_GetNumTerminals(ddman, (DdNode *)dd);
}

//------------------------------------------------------------------------------


JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1GetNumMinterms(JNIEnv *env, jclass cls, jint dd, jint num_vars)
{
	return DD_GetNumMinterms(ddman, (DdNode *)dd, num_vars);
}

//------------------------------------------------------------------------------


JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1GetNumPaths(JNIEnv *env, jclass cls, jint dd)
{
	return DD_GetNumPaths(ddman, (DdNode *)dd);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintInfo(JNIEnv *env, jclass cls, jint dd, jint num_vars)
{
	DD_PrintInfo(ddman, (DdNode *)dd, num_vars);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintInfoBrief(JNIEnv *env, jclass cls, jint dd, jint num_vars)
{
	DD_PrintInfoBrief(ddman, (DdNode *)dd, num_vars);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintSupport(JNIEnv *env, jclass cls, jint dd)
{
	DD_PrintSupport(ddman, (DdNode *)dd);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1GetSupport(JNIEnv *env, jclass cls, jint dd)
{
	return (int)DD_GetSupport(ddman, (DdNode *)dd);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintTerminals(JNIEnv *env, jclass cls, jint dd)
{
	DD_PrintTerminals(ddman, (DdNode *)dd);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintTerminalsAndNumbers(JNIEnv *env, jclass cls, jint dd, jint num_vars)
{
	DD_PrintTerminalsAndNumbers(ddman, (DdNode *)dd, num_vars);
}

//==============================================================================
//
//	Wrapper functions for dd_matrix
//	
//==============================================================================

JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1SetVectorElement(JNIEnv *env, jclass cls, jint dd, jint vars, jint num_vars, jlong index, jdouble value)
{
	return (int)DD_SetVectorElement(ddman, (DdNode *)dd, (DdNode **)vars, num_vars, index, value);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1SetMatrixElement(JNIEnv *env, jclass cls, jint dd, jint rvars, jint num_rvars, jint cvars, jint num_cvars, jlong rindex, jlong cindex, jdouble value)
{
	return (int)DD_SetMatrixElement(ddman, (DdNode *)dd, (DdNode **)rvars, num_rvars, (DdNode **)cvars, num_cvars, rindex, cindex, value);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Set3DMatrixElement(JNIEnv *env, jclass cls, jint dd, jint rvars, jint num_rvars, jint cvars, jint num_cvars, jint lvars, jint num_lvars, jlong rindex, jlong cindex, jlong lindex, jdouble value)
{
	return (int)DD_Set3DMatrixElement(ddman, (DdNode *)dd, (DdNode **)rvars, num_rvars, (DdNode **)cvars, num_cvars, (DdNode **)lvars, num_lvars, rindex, cindex, lindex, value);
}

//------------------------------------------------------------------------------


JNIEXPORT jdouble JNICALL Java_jdd_JDD_DD_1GetVectorElement(JNIEnv *env, jclass cls, jint dd, jint vars, jint num_vars, jlong index)
{
	return DD_GetVectorElement(ddman, (DdNode *)dd, (DdNode **)vars, num_vars, index);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Identity(JNIEnv *env, jclass cls, jint rvars, jint cvars, jint num_vars)
{
	return (int)DD_Identity(ddman, (DdNode **)rvars, (DdNode **)cvars, num_vars);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1Transpose(JNIEnv *env, jclass cls, jint dd, jint rvars, jint cvars, jint num_vars, jint lvars, jint num_lvars)
{
	return (int)DD_Transpose(ddman, (DdNode *)dd, (DdNode **)rvars, (DdNode **)cvars, num_vars);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDD_DD_1MatrixMultiply(JNIEnv *env, jclass cls, jint dd1, jint dd2, jint vars, jint num_vars, jint method)
{
	return (int)DD_MatrixMultiply(ddman, (DdNode *)dd1, (DdNode *)dd2, (DdNode **)vars, num_vars, method);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintVector(JNIEnv *env, jclass cls, jint dd, jint vars, jint num_vars, jint acc)
{
	DD_PrintVector(ddman, (DdNode *)dd, (DdNode **)vars, num_vars, acc);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintMatrix(JNIEnv *env, jclass cls, jint dd, jint rvars, jint num_rvars, jint cvars, jint num_cvars, jint acc)
{
	DD_PrintMatrix(ddman, (DdNode *)dd, (DdNode **)rvars, num_rvars, (DdNode **)cvars, num_cvars, acc);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1PrintVectorFiltered(JNIEnv *env, jclass cls, jint dd, jint filter, jint vars, jint num_vars, jint acc)
{
	DD_PrintVectorFiltered(ddman, (DdNode *)dd, (DdNode *)filter, (DdNode **)vars, num_vars, acc);
}

//==============================================================================
//
//	Wrapper functions for dd_export
//	
//==============================================================================

JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportDDToDotFile(JNIEnv *env, jclass cls, jint dd, jstring filename)
{
	const char *str = env->GetStringUTFChars(filename, 0);
	DD_ExportDDToDotFile(ddman, (DdNode *)dd, (char *)str);
	env->ReleaseStringUTFChars(filename, str);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportDDToDotFileLabelled(JNIEnv *env, jclass cls, jint dd, jstring filename, jobject var_names)
{
	int i, j;
	jint size;
	jclass vn_cls;
	jmethodID vn_mid;
	jstring jstr;
	const char **names;
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
        vn_mid = env->GetMethodID(vn_cls, "elementAt", "(I)Ljava/lang/Object;");
        if (vn_mid == 0) {
            return;
        }
	for (i = 0; i < size; i++) {
		jstr = (jstring)env->CallObjectMethod(var_names, vn_mid, i);
		names[i] = env->GetStringUTFChars(jstr, 0);
	}

	// get filename string
	filenamestr = env->GetStringUTFChars(filename, 0);

	// call dd_export... function
	DD_ExportDDToDotFileLabelled(ddman, (DdNode *)dd, (char *)filenamestr, (char **)names);	

	// release memory
	for (i = 0; i < size; i++) {
		env->ReleaseStringUTFChars(jstr, names[i]);
	}
	env->ReleaseStringUTFChars(filename, filenamestr);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportMatrixToPPFile(JNIEnv *env, jclass cls, jint dd, jint rvars, jint num_rvars, jint cvars, jint num_cvars, jstring filename)
{
	const char *str = env->GetStringUTFChars(filename, 0);
	DD_ExportMatrixToPPFile(ddman, (DdNode *)dd, (DdNode **)rvars, num_rvars, (DdNode **)cvars, num_cvars, (char *)str);
	env->ReleaseStringUTFChars(filename, str);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportMatrixToMatlabFile(JNIEnv *env, jclass cls, jint dd, jint rvars, jint num_rvars, jint cvars, jint num_cvars, jstring name, jstring filename)
{
	const char *str1 = env->GetStringUTFChars(name, 0);
	const char *str2 = env->GetStringUTFChars(filename, 0);
	DD_ExportMatrixToMatlabFile(ddman, (DdNode *)dd, (DdNode **)rvars, num_rvars, (DdNode **)cvars, num_cvars, (char *)str1, (char *)str2);
	env->ReleaseStringUTFChars(name, str1);
	env->ReleaseStringUTFChars(filename, str2);
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDD_DD_1ExportMatrixToSpyFile(JNIEnv *env, jclass cls, jint dd, jint rvars, jint num_rvars, jint cvars, jint num_cvars, jint depth, jstring filename)
{
	const char *str = env->GetStringUTFChars(filename, 0);
	DD_ExportMatrixToSpyFile(ddman, (DdNode *)dd, (DdNode **)rvars, num_rvars, (DdNode **)cvars, num_cvars, depth, (char *)str);
	env->ReleaseStringUTFChars(filename, str);
}

//==============================================================================
//
//	Functions for JDDNode class
//	
//==============================================================================

JNIEXPORT jboolean JNICALL Java_jdd_JDDNode_DDN_1IsConstant(JNIEnv *env, jobject obj, jint dd)
{
	return Cudd_IsConstant((DdNode *)dd);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDDNode_DDN_1GetIndex(JNIEnv *env, jobject obj, jint dd)
{
	return ((DdNode *)dd)->index;
}

//------------------------------------------------------------------------------


JNIEXPORT jdouble JNICALL Java_jdd_JDDNode_DDN_1GetValue(JNIEnv *env, jobject obj, jint dd)
{
	return Cudd_V((DdNode *)dd);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDDNode_DDN_1GetThen(JNIEnv *env, jobject obj, jint dd)
{
	return (int)Cudd_T((DdNode *)dd);
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDDNode_DDN_1GetElse(JNIEnv *env, jobject obj, jint dd)
{
	return (int)Cudd_E((DdNode *)dd);
}

//==============================================================================
//
//	Functions for JDDVars class
//	
//==============================================================================

JNIEXPORT jint JNICALL Java_jdd_JDDVars_DDV_1BuildArray(JNIEnv *env, jobject obj)
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
	mid = env->GetMethodID(cls, "getVarPtr", "(I)I");
	if (mid == 0) {
		delete arr;
		return 0;
	}
	for (i = 0; i < n; i++) {
		arr[i] = (DdNode *)env->CallIntMethod(obj, mid, i);
	}
	
	return (int)arr;
}

//------------------------------------------------------------------------------


JNIEXPORT void JNICALL Java_jdd_JDDVars_DDV_1FreeArray(JNIEnv *env, jobject obj, jint arr)
{
	delete (DdNode **)arr;
}

//------------------------------------------------------------------------------


JNIEXPORT jint JNICALL Java_jdd_JDDVars_DDV_1GetIndex(JNIEnv *env, jobject obj, jint dd)
{
	return ((DdNode *)dd)->index;
}

//------------------------------------------------------------------------------

