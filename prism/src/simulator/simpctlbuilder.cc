//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
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

#include <jni.h>
#include <iostream>
#include "jnipointer.h"
#include "simpctl.h"
#include "simstate.h"
#include "simsampling.h"
#include "simmodel.h"
#include "simexpression.h"
#include "SimulatorEngine.h"

//PCTL Expression building methods

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_loadPctlBoundedUntil
  (JNIEnv *env, jclass cls, jlong __pointer exprPointer1, jlong __pointer exprPointer2, jdouble lowerBound, jdouble upperBound)
{
	CExpression* expr1 = jlong_to_CExpression(exprPointer1);
	CExpression* expr2 = jlong_to_CExpression(exprPointer2);
	CBoundedUntil* bu = new CBoundedUntil(expr1, expr2, (double)lowerBound, (double)upperBound);
	Register_Path_Formula(bu);
	return ptr_to_jlong(bu);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_loadPctlBoundedUntilNegated
  (JNIEnv *env, jclass cls, jlong __pointer exprPointer1, jlong __pointer exprPointer2, jdouble lowerBound, jdouble upperBound)
{
	CExpression* expr1 = jlong_to_CExpression(exprPointer1);
	CExpression* expr2 = jlong_to_CExpression(exprPointer2);
	CBoundedUntil* bu = new CBoundedUntil(expr1, expr2, (double)lowerBound, (double)upperBound);
	bu->Set_Negate(true);
	Register_Path_Formula(bu);
	return ptr_to_jlong(bu);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_loadPctlUntil
  (JNIEnv *env, jclass cls, jlong __pointer exprPointer1, jlong __pointer exprPointer2)
{
	CExpression* expr1 = jlong_to_CExpression(exprPointer1);
	CExpression* expr2 = jlong_to_CExpression(exprPointer2);
	CUntil* bu = new CUntil(expr1, expr2);
	Register_Path_Formula(bu);
	return ptr_to_jlong(bu);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_loadPctlUntilNegated
  (JNIEnv *env, jclass cls, jlong __pointer exprPointer1, jlong __pointer exprPointer2)
{
	CExpression* expr1 = jlong_to_CExpression(exprPointer1);
	CExpression* expr2 = jlong_to_CExpression(exprPointer2);
	CUntil* bu = new CUntil(expr1, expr2);
	bu->Set_Negate(true);
	Register_Path_Formula(bu);
	return ptr_to_jlong(bu);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_loadPctlNext
  (JNIEnv *env, jclass cls, jlong __pointer exprPointer)
{
	CExpression* expr = jlong_to_CExpression(exprPointer);
	CNext* bu = new CNext(expr);
	Register_Path_Formula(bu);
	return ptr_to_jlong(bu);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_loadPctlReachability
  (JNIEnv *env, jclass cls, jint rsi, jlong __pointer exprPointer)
{
	CExpression* expr = jlong_to_CExpression(exprPointer);
	CRewardReachability* reach = new CRewardReachability(rsi, expr);
	Register_Path_Formula(reach);
	return ptr_to_jlong(reach);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_loadPctlCumulative
  (JNIEnv *env, jclass cls, jint rsi, jdouble time)
{
	CRewardCumulative* cumul = new CRewardCumulative(rsi, (double)time);
	Register_Path_Formula(cumul);
	return ptr_to_jlong(cumul);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_loadPctlInstantanious
  (JNIEnv *env, jclass cls, jint rsi, jdouble time)
{
	CRewardInstantanious* instant = new CRewardInstantanious(rsi, (double)time);
	Register_Path_Formula(instant);
	return ptr_to_jlong(instant);
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_loadProbQuestion
  (JNIEnv *env, jclass cls, jlong __pointer pathPointer)
{
	CPathFormula* formula = jlong_to_CPathFormula(pathPointer);
	CProbEqualsQuestion* sampler = new CProbEqualsQuestion(formula);
	return Register_Sample_Holder(sampler);
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_loadRewardQuestion
  (JNIEnv *env, jclass cls, jlong __pointer rewardPointer)
{
	CRewardFormula* formula = jlong_to_CRewardFormula(rewardPointer);
	CRewardEqualsQuestion* sampler = new CRewardEqualsQuestion(formula);
	return Register_Sample_Holder(sampler);
}


//Utility method

JNIEXPORT void JNICALL Java_simulator_SimulatorEngine_printRegisteredPathFormulae
  (JNIEnv *env, jclass cls)
{
	Print_Formulae();
}
