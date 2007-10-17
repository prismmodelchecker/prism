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
#include "simstate.h"
#include "simexpression.h"
#include "SimulatorEngine.h"
#include "jnipointer.h"

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createIntegerVar
(JNIEnv * env, jclass cls, jint varIndex)
{
	int * location = &(state_variables[(int)varIndex]);
	CExpression * var = new CIntegerVar(location, (int)varIndex);
	return ptr_to_jlong(var);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createBooleanVar
(JNIEnv * env, jclass cls, jint varIndex)
{
	int * location = &(state_variables[(int)varIndex]);
	CExpression * var = new CBooleanVar(location, (int)varIndex);
	return ptr_to_jlong(var);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createDouble
(JNIEnv * env, jclass cls, jdouble value)
{
	CExpression * val = new CDouble((double)value);
	return ptr_to_jlong(val);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createInteger
(JNIEnv * env, jclass cls, jint value)
{
	CExpression * val = new CInteger((int)value);
	return ptr_to_jlong(val);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createBoolean
(JNIEnv * env, jclass cls, jboolean value)
{
	CExpression * val = new CBoolean((bool)value);
	return ptr_to_jlong(val);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createCeil
(JNIEnv * env, jclass cls, jlong __pointer exprPointer)
{
	CExpression * expr = jlong_to_CExpression(exprPointer);
	CExpression * ceil = new CCeil(expr);
	return ptr_to_jlong(ceil);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createFloor
(JNIEnv * env, jclass cls, jlong __pointer exprPointer)
{
	CExpression * expr = jlong_to_CExpression(exprPointer);
	CExpression * floor = new CFloor(expr);
	return ptr_to_jlong(floor);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createNormalPow
  (JNIEnv * env, jclass cls, jlong __pointer baseExprPointer, jlong __pointer expExprPointer)
{
	CNormalExpression * base = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(baseExprPointer));
	CNormalExpression * exp = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(expExprPointer));
	CExpression * pow = new CNormalPow(base, exp);
	return ptr_to_jlong(pow);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createRealPow
  (JNIEnv *env, jclass cls, jlong __pointer baseExprPointer, jlong __pointer expExprPointer)
{
	CExpression * base = jlong_to_CExpression(baseExprPointer);
	CExpression * exp = jlong_to_CExpression(expExprPointer);
	CExpression * pow = new CRealPow(base, exp);
	return ptr_to_jlong(pow);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createMod
  (JNIEnv *env, jclass cls, jlong __pointer leftExprPointer, jlong __pointer rightExprPointer)
{
	CNormalExpression * left = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(leftExprPointer));
	CNormalExpression * right = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(rightExprPointer));
	CExpression * modExpr = new CMod(left, right);
	return ptr_to_jlong(modExpr);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createNot
(JNIEnv * env, jclass cls, jlong __pointer exprPointer)
{
	CNormalExpression * expr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(exprPointer));
	CExpression * notrr = new CNot(expr);
	return ptr_to_jlong(notrr);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createAnd
(JNIEnv * env, jclass cls, jlongArray __pointer exprPointers)
{
	jsize length = env->GetArrayLength(exprPointers);
	jlong *buf = new jlong[length];
	jint i, sum = 0;
	env->GetLongArrayRegion(exprPointers, 0, length, buf);

	CNormalExpression ** exprs = new CNormalExpression*[(int)length];

	for (i = 0; i < length; i++) 
	{
		exprs[i] = dynamic_cast<CNormalExpression *>(jlong_to_CExpression(buf[i]));
	}

	CExpression * expr = new CAnd(exprs, (int)length);
	
	delete buf;

	return ptr_to_jlong(expr);

}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createOr
(JNIEnv * env, jclass cls, jlongArray __pointer exprPointers)
{
	jsize length = env->GetArrayLength(exprPointers);

	jlong *buf = new jlong[length];
	jint i, sum = 0;
	env->GetLongArrayRegion(exprPointers, 0, length, buf);

	CNormalExpression ** exprs = new CNormalExpression*[(int)length];

	for (i = 0; i < length; i++) 
	{
		exprs[i] = dynamic_cast<CNormalExpression *>(jlong_to_CExpression(buf[i]));
	}

	CExpression * expr = new COr(exprs, (int)length);

	delete buf;

	return ptr_to_jlong(expr);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createNormalMax
(JNIEnv * env, jclass cls, jlongArray __pointer exprPointers)
{
	jsize length = env->GetArrayLength(exprPointers);

	jlong *buf = new jlong[length];
	jint i, sum = 0;
	env->GetLongArrayRegion(exprPointers, 0, length, buf);

	CNormalExpression ** exprs = new CNormalExpression*[(int)length];

	for (i = 0; i < length; i++) 
	{
		exprs[i] = dynamic_cast<CNormalExpression *>(jlong_to_CExpression(buf[i]));
	}

	CExpression * expr = new CNormalMax(exprs, (int)length);

	delete buf;

	return ptr_to_jlong(expr);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createNormalMin
(JNIEnv * env, jclass cls, jlongArray __pointer exprPointers)
{
	jsize length = env->GetArrayLength(exprPointers);

	jlong *buf = new jlong[length];
	jint i, sum = 0;
	env->GetLongArrayRegion(exprPointers, 0, length, buf);

	CNormalExpression ** exprs = new CNormalExpression*[(int)length];

	for (i = 0; i < length; i++) 
	{
		exprs[i] = dynamic_cast<CNormalExpression *>(jlong_to_CExpression(buf[i]));
	}

	CExpression * expr = new CNormalMin(exprs, (int)length);

	delete buf;

	return ptr_to_jlong(expr);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createRealMax
(JNIEnv * env, jclass cls, jlongArray __pointer exprPointers)
{
	jsize length = env->GetArrayLength(exprPointers);

	jlong *buf = new jlong[length];
	jint i, sum = 0;
	env->GetLongArrayRegion(exprPointers, 0, length, buf);

	CExpression ** exprs = new CExpression*[(int)length];

	for (i = 0; i < length; i++) 
	{
		exprs[i] = (jlong_to_CExpression(buf[i]));
	}

	CExpression * expr = new CRealMax(exprs, (int)length);

	delete buf;

	return ptr_to_jlong(expr);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createRealMin
(JNIEnv * env, jclass cls, jlongArray __pointer exprPointers)
{
	jsize length = env->GetArrayLength(exprPointers);

	jlong *buf = new jlong[length];
	jint i, sum = 0;
	env->GetLongArrayRegion(exprPointers, 0, length, buf);

	CExpression ** exprs = new CExpression*[(int)length];

	for (i = 0; i < length; i++) 
	{
		exprs[i] = (jlong_to_CExpression(buf[i]));
	}

	CExpression * expr = new CRealMin(exprs, (int)length);

	delete buf;

	return ptr_to_jlong(expr);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createNormalTimes
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CNormalTimes(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createNormalPlus
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CNormalPlus(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createNormalMinus
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CNormalMinus(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createRealTimes
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CExpression * lexpr = (jlong_to_CExpression(lexprPointer));
	CExpression * rexpr = (jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CRealTimes(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createDivide
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CExpression * lexpr = (jlong_to_CExpression(lexprPointer));
	CExpression * rexpr = (jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CDivide(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createRealPlus
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CExpression * lexpr = (jlong_to_CExpression(lexprPointer));
	CExpression * rexpr = (jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CRealPlus(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createRealMinus
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CExpression * lexpr = (jlong_to_CExpression(lexprPointer));
	CExpression * rexpr = (jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CRealMinus(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createRealIte
(JNIEnv * env, jclass cls , jlong __pointer conditionPointer, jlong __pointer truePointer, jlong __pointer falsePointer)
{
	CNormalExpression * condition = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(conditionPointer));
	CExpression * trueExpr = (jlong_to_CExpression(truePointer));
	CExpression * falseExpr = (jlong_to_CExpression(falsePointer));
	CExpression * ite = new CRealIte(condition, trueExpr, falseExpr);
	return ptr_to_jlong(ite);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createIte
(JNIEnv * env, jclass cls , jlong __pointer conditionPointer, jlong __pointer truePointer, jlong __pointer falsePointer)
{
	CNormalExpression * condition = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(conditionPointer));
	CExpression * trueExpr = (jlong_to_CExpression(truePointer));
	CExpression * falseExpr = (jlong_to_CExpression(falsePointer));
	CExpression * ite = new CIte(condition, trueExpr, falseExpr);
	return ptr_to_jlong(ite);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createNormalEquals
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CNormalEquals(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createRealEquals
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CExpression * lexpr = (jlong_to_CExpression(lexprPointer));
	CExpression * rexpr = (jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CRealEquals(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createNormalNotEquals
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CNormalNotEquals(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createRealNotEquals
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CExpression * lexpr = (jlong_to_CExpression(lexprPointer));
	CExpression * rexpr = (jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CRealNotEquals(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createNormalLessThan
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CNormalLessThan(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createRealLessThan
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CExpression * lexpr = (jlong_to_CExpression(lexprPointer));
	CExpression * rexpr = (jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CRealLessThan(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createNormalGreaterThan
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CNormalGreaterThan(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createRealGreaterThan
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CExpression * lexpr = (jlong_to_CExpression(lexprPointer));
	CExpression * rexpr = (jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CRealGreaterThan(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createNormalLessThanEqual
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CNormalLessThanEqual(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createRealLessThanEqual
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CExpression * lexpr = (jlong_to_CExpression(lexprPointer));
	CExpression * rexpr = (jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CRealLessThanEqual(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createNormalGreaterThanEqual
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CNormalGreaterThanEqual(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createRealGreaterThanEqual
(JNIEnv * env, jclass cls, jlong __pointer lexprPointer, jlong __pointer rexprPointer)
{
	CExpression * lexpr = (jlong_to_CExpression(lexprPointer));
	CExpression * rexpr = (jlong_to_CExpression(rexprPointer));
	CExpression * binary = new CRealGreaterThanEqual(lexpr, rexpr);
	return ptr_to_jlong(binary);
}

JNIEXPORT void JNICALL Java_simulator_SimulatorEngine_printExpression
  (JNIEnv * rnv, jclass clss, jlong exprPointer)
{
	CExpression * expr = (jlong_to_CExpression(exprPointer));
	std::string str = expr->To_String();
}

JNIEXPORT jstring JNICALL Java_simulator_SimulatorEngine_expressionToString
  (JNIEnv * env, jclass cls, jlong exprPointer)
{
	CExpression * expr = (jlong_to_CExpression(exprPointer));
	std::string str = expr->To_String();
	return env->NewStringUTF(str.c_str());
}

JNIEXPORT void JNICALL Java_simulator_SimulatorEngine_deleteExpression
  (JNIEnv * env, jclass cls, jlong exprPointer)
{
	CExpression * expr = (jlong_to_CExpression(exprPointer));
	if(expr != NULL)
		delete expr;
}
