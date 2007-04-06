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
#include "simpctl.h"
#include "simstate.h"
#include "simsampling.h"
#include "simmodel.h"
#include "simexpression.h"
#include "SimulatorEngine.h"


//Helper functions

inline jint To_JInt(CExpression * expr) 
{
	return (jint)(expr);
}

inline CExpression * To_Expr(jint pointer) 
{
	return (CExpression*)(pointer);
}

inline jint Formula_To_JInt(CPathFormula * form)
{
	return (jint)(form);
}

inline CPathFormula* To_Path_Formula(jint pointer)
{
	return (CPathFormula*)(pointer);
}

inline CRewardFormula* To_Reward_Formula(jint pointer)
{
	return (CRewardFormula*)(pointer);
}


//PCTL Expression building methods

/*
 * Class:     simulator_SimulatorEngine
 * Method:    loadPctlBoundedUntil
 * Signature: (JJDD)J
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_loadPctlBoundedUntil
  (JNIEnv *env, jclass cls, jint exprPointer1, jint exprPointer2, jdouble lowerBound, jdouble upperBound)
{
	CExpression* expr1 = To_Expr(exprPointer1);
	CExpression* expr2 = To_Expr(exprPointer2);
	
	CBoundedUntil* bu = new CBoundedUntil(expr1, expr2, (double)lowerBound, (double)upperBound);

	Register_Path_Formula(bu);

	return Formula_To_JInt(bu);
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_loadPctlBoundedUntilNegated
  (JNIEnv *env, jclass cls, jint exprPointer1, jint exprPointer2, jdouble lowerBound, jdouble upperBound, jboolean)
{
	CExpression* expr1 = To_Expr(exprPointer1);
	CExpression* expr2 = To_Expr(exprPointer2);
	
	CBoundedUntil* bu = new CBoundedUntil(expr1, expr2, (double)lowerBound, (double)upperBound);
	bu->Set_Negate(true);

	Register_Path_Formula(bu);

	return Formula_To_JInt(bu);
}

/*
 * Class:     simulator_SimulatorEngine
 * Method:    loadPctlUntil
 * Signature: (JJ)J
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_loadPctlUntil
  (JNIEnv *env, jclass cls, jint exprPointer1, jint exprPointer2)
{
	CExpression* expr1 = To_Expr(exprPointer1);
	CExpression* expr2 = To_Expr(exprPointer2);
	
	CUntil* bu = new CUntil(expr1, expr2);

	Register_Path_Formula(bu);

	return Formula_To_JInt(bu);
}

JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_loadPctlUntilNegated
  (JNIEnv *env, jclass cls, jint exprPointer1, jint exprPointer2)
{
	CExpression* expr1 = To_Expr(exprPointer1);
	CExpression* expr2 = To_Expr(exprPointer2);
	
	CUntil* bu = new CUntil(expr1, expr2);
	bu->Set_Negate(true);

	Register_Path_Formula(bu);

	return Formula_To_JInt(bu);
}

/*
 * Class:     simulator_SimulatorEngine
 * Method:    loadPctlNext
 * Signature: (J)J
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_loadPctlNext
  (JNIEnv *env, jclass cls, jint exprPointer)
{
	CExpression* expr = To_Expr(exprPointer);
	
	CNext* bu = new CNext(expr);

	Register_Path_Formula(bu);

	return Formula_To_JInt(bu);
}

/*
 * Class:     simulator_SimulatorEngine
 * Method:    loadPctlReachability
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_loadPctlReachability
  (JNIEnv *env, jclass cls, jint rsi, jint exprPointer)
{
	CExpression* expr = To_Expr(exprPointer);

	CRewardReachability* reach = new CRewardReachability(rsi, expr);

	Register_Path_Formula(reach);

	return Formula_To_JInt(reach);
}

/*
 * Class:     simulator_SimulatorEngine
 * Method:    loadPctlCumulative
 * Signature: (D)I
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_loadPctlCumulative
  (JNIEnv *env, jclass cls, jint rsi, jdouble time)
{
	CRewardCumulative* cumul = new CRewardCumulative(rsi, (double)time);

	Register_Path_Formula(cumul);

	return Formula_To_JInt(cumul);
}

/*
 * Class:     simulator_SimulatorEngine
 * Method:    loadPctlInstantanious
 * Signature: (D)I
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_loadPctlInstantanious
  (JNIEnv *env, jclass cls, jint rsi, jdouble time)
{
	CRewardInstantanious* instant = new CRewardInstantanious(rsi, (double)time);

	Register_Path_Formula(instant);

	return Formula_To_JInt(instant);
}
/*
 * Class:     simulator_SimulatorEngine
 * Method:    loadProbQuestion
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_loadProbQuestion
  (JNIEnv *env, jclass cls, jint pathPointer)
{
	CPathFormula* formula = To_Path_Formula(pathPointer);
	CProbEqualsQuestion* sampler = new CProbEqualsQuestion(formula);
	return Register_Sample_Holder(sampler);
}


/*
 * Class:     simulator_SimulatorEngine
 * Method:    loadRewardQuestion
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_loadRewardQuestion
  (JNIEnv *env, jclass cls, jint rewardPointer)
{
	CRewardFormula* formula = To_Reward_Formula(rewardPointer);
	CRewardEqualsQuestion* sampler = new CRewardEqualsQuestion(formula);
	return Register_Sample_Holder(sampler);
}


//Utility method

/*
 * Class:     simulator_SimulatorEngine
 * Method:    printRegisteredPathFormulae
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_simulator_SimulatorEngine_printRegisteredPathFormulae
  (JNIEnv *env, jclass cls)
{
	Print_Formulae();
}
