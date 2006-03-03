//==============================================================================
//	
//	Copyright (c) 2004-2005, Andrew Hinton
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
//=============================================================================

#include <jni.h>
#include <iostream>
#include "simstate.h"
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

/*
* Class:     simulator_SimulatorEngine
* Method:    createIntegerVar
* Signature: (I)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createIntegerVar
(JNIEnv * env, jclass cls, jint varIndex)
{
	int * location = &(state_variables[(int)varIndex]);
	CExpression * var = new CIntegerVar(location, (int)varIndex);
	return To_JInt(var);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createBooleanVar
* Signature: (I)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createBooleanVar
(JNIEnv * env, jclass cls, jint varIndex)
{
	int * location = &(state_variables[(int)varIndex]);
	CExpression * var = new CBooleanVar(location, (int)varIndex);
	return To_JInt(var);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createDouble
* Signature: (D)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createDouble
(JNIEnv * env, jclass cls, jdouble value)
{
	CExpression * val = new CDouble((double)value);
	return To_JInt(val);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createInteger
* Signature: (I)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createInteger
(JNIEnv * env, jclass cls, jint value)
{
	CExpression * val = new CInteger((int)value);
	return To_JInt(val);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createBoolean
* Signature: (Z)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createBoolean
(JNIEnv * env, jclass cls, jboolean value)
{
	CExpression * val = new CBoolean((bool)value);
	return To_JInt(val);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createCeil
* Signature: (J)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createCeil
(JNIEnv * env, jclass cls, jint exprPointer)
{
	CExpression * expr = To_Expr(exprPointer);
	CExpression * ceil = new CCeil(expr);
	return To_JInt(ceil);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createFloor
* Signature: (J)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createFloor
(JNIEnv * env, jclass cls, jint exprPointer)
{
	CExpression * expr = To_Expr(exprPointer);
	CExpression * floor = new CFloor(expr);
	return To_JInt(floor);
}

/*
 * Class:     simulator_SimulatorEngine
 * Method:    createNormalPow
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createNormalPow
  (JNIEnv * env, jclass cls, jint baseExprPointer, jint expExprPointer)
{
	CNormalExpression * base = dynamic_cast<CNormalExpression*>(To_Expr(baseExprPointer));
	CNormalExpression * exp = dynamic_cast<CNormalExpression*>(To_Expr(expExprPointer));
	CExpression * pow = new CNormalPow(base, exp);

	return To_JInt(pow);
}

/*
 * Class:     simulator_SimulatorEngine
 * Method:    createRealPow
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createRealPow
  (JNIEnv *env, jclass cls, jint baseExprPointer, jint expExprPointer)
{
	CExpression * base = To_Expr(baseExprPointer);
	CExpression * exp = To_Expr(expExprPointer);
	CExpression * pow = new CRealPow(base, exp);

	return To_JInt(pow);
}

/*
 * Class:     simulator_SimulatorEngine
 * Method:    createMod
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createMod
  (JNIEnv *env, jclass cls, jint leftExprPointer, jint rightExprPointer)
{
	CNormalExpression * left = dynamic_cast<CNormalExpression*>(To_Expr(leftExprPointer));
	CNormalExpression * right = dynamic_cast<CNormalExpression*>(To_Expr(rightExprPointer));
	CExpression * modExpr = new CMod(left, right);

	return To_JInt(modExpr);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createNot
* Signature: (J)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createNot
(JNIEnv * env, jclass cls, jint exprPointer)
{
	CNormalExpression * expr = dynamic_cast<CNormalExpression*>(To_Expr(exprPointer));
	CExpression * notrr = new CNot(expr);
	return To_JInt(notrr);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createAnd
* Signature: ([J)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createAnd
(JNIEnv * env, jclass cls, jintArray exprPointers)
{
	//std::cout << "createAnd called" << endl;
	jsize length = env->GetArrayLength(exprPointers);

	//std::cout << "createAnd before longinit" << endl;
	jint buf[length];
	jint i, sum = 0;
	env->GetIntArrayRegion(exprPointers, 0, length, buf);
	//std::cout << "createAnd after longinit" << endl;
	CNormalExpression ** exprs = new CNormalExpression*[(int)length];
	//std::cout << "createAnd before forloop" << endl;
	for (i = 0; i < length; i++) 
	{
		exprs[i] = dynamic_cast<CNormalExpression *>(To_Expr(buf[i]));
	}
	//std::cout << "createAnd before creating And" << endl;
	CExpression * expr = new CAnd(exprs, (int)length);
	//std::cout << "createAnd before returning value" << endl;
	return To_JInt(expr);

}

/*
* Class:     simulator_SimulatorEngine
* Method:    createOr
* Signature: ([J)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createOr
(JNIEnv * env, jclass cls, jintArray exprPointers)
{
	jsize length = env->GetArrayLength(exprPointers);

	
	jint buf[length];
	jint i, sum = 0;
	env->GetIntArrayRegion(exprPointers, 0, length, buf);

	CNormalExpression ** exprs = new CNormalExpression*[(int)length];

	for (i = 0; i < length; i++) 
	{
		exprs[i] = dynamic_cast<CNormalExpression *>(To_Expr(buf[i]));
	}

	CExpression * expr = new COr(exprs, (int)length);

	return To_JInt(expr);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createNormalMax
* Signature: ([J)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createNormalMax
(JNIEnv * env, jclass cls, jintArray exprPointers)
{
	jsize length = env->GetArrayLength(exprPointers);

	
	jint buf[length];
	jint i, sum = 0;
	env->GetIntArrayRegion(exprPointers, 0, length, buf);

	CNormalExpression ** exprs = new CNormalExpression*[(int)length];

	for (i = 0; i < length; i++) 
	{
		exprs[i] = dynamic_cast<CNormalExpression *>(To_Expr(buf[i]));
	}

	CExpression * expr = new CNormalMax(exprs, (int)length);

	return To_JInt(expr);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createNormalMin
* Signature: ([J)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createNormalMin
(JNIEnv * env, jclass cls, jintArray exprPointers)
{
	jsize length = env->GetArrayLength(exprPointers);

	
	jint buf[length];
	jint i, sum = 0;
	env->GetIntArrayRegion(exprPointers, 0, length, buf);

	CNormalExpression ** exprs = new CNormalExpression*[(int)length];

	for (i = 0; i < length; i++) 
	{
		exprs[i] = dynamic_cast<CNormalExpression *>(To_Expr(buf[i]));
	}

	CExpression * expr = new CNormalMin(exprs, (int)length);

	return To_JInt(expr);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createRealMax
* Signature: ([J)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createRealMax
(JNIEnv * env, jclass cls, jintArray exprPointers)
{
	//std::cout << "Am i (createRealMax) even called" << endl;
	jsize length = env->GetArrayLength(exprPointers);

	
	jint buf[length];
	jint i, sum = 0;
	env->GetIntArrayRegion(exprPointers, 0, length, buf);

	CExpression ** exprs = new CExpression*[(int)length];

	for (i = 0; i < length; i++) 
	{
		exprs[i] = (To_Expr(buf[i]));
	}

	CExpression * expr = new CRealMax(exprs, (int)length);

	return To_JInt(expr);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createRealMin
* Signature: ([J)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createRealMin
(JNIEnv * env, jclass cls, jintArray exprPointers)
{
	jsize length = env->GetArrayLength(exprPointers);

	
	jint buf[length];
	jint i, sum = 0;
	env->GetIntArrayRegion(exprPointers, 0, length, buf);

	CExpression ** exprs = new CExpression*[(int)length];

	for (i = 0; i < length; i++) 
	{
		exprs[i] = (To_Expr(buf[i]));
	}

	CExpression * expr = new CRealMin(exprs, (int)length);

	return To_JInt(expr);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createNormalTimes
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createNormalTimes
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(To_Expr(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(To_Expr(rexprPointer));
	CExpression * binary = new CNormalTimes(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createNormalPlus
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createNormalPlus
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(To_Expr(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(To_Expr(rexprPointer));
	CExpression * binary = new CNormalPlus(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createNormalMinus
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createNormalMinus
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(To_Expr(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(To_Expr(rexprPointer));
	CExpression * binary = new CNormalMinus(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createRealTimes
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createRealTimes
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CExpression * lexpr = (To_Expr(lexprPointer));
	CExpression * rexpr = (To_Expr(rexprPointer));
	CExpression * binary = new CRealTimes(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createRealDivide
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createDivide
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CExpression * lexpr = (To_Expr(lexprPointer));
	CExpression * rexpr = (To_Expr(rexprPointer));
	CExpression * binary = new CDivide(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createRealPlus
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createRealPlus
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CExpression * lexpr = (To_Expr(lexprPointer));
	CExpression * rexpr = (To_Expr(rexprPointer));
	CExpression * binary = new CRealPlus(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createRealMinus
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createRealMinus
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CExpression * lexpr = (To_Expr(lexprPointer));
	CExpression * rexpr = (To_Expr(rexprPointer));
	CExpression * binary = new CRealMinus(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createIte
* Signature: (JJJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createRealIte
(JNIEnv * env, jclass cls , jint conditionPointer, jint truePointer, jint falsePointer)
{
	CNormalExpression * condition = dynamic_cast<CNormalExpression*>(To_Expr(conditionPointer));
	CExpression * trueExpr = (To_Expr(truePointer));
	CExpression * falseExpr = (To_Expr(falsePointer));
	CExpression * ite = new CRealIte(condition, trueExpr, falseExpr);
	return To_JInt(ite);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createIte
* Signature: (JJJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createIte
(JNIEnv * env, jclass cls , jint conditionPointer, jint truePointer, jint falsePointer)
{
	CNormalExpression * condition = dynamic_cast<CNormalExpression*>(To_Expr(conditionPointer));
	CExpression * trueExpr = (To_Expr(truePointer));
	CExpression * falseExpr = (To_Expr(falsePointer));
	CExpression * ite = new CIte(condition, trueExpr, falseExpr);
	return To_JInt(ite);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createNormalEquals
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createNormalEquals
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(To_Expr(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(To_Expr(rexprPointer));
	CExpression * binary = new CNormalEquals(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createRealEquals
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createRealEquals
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CExpression * lexpr = (To_Expr(lexprPointer));
	CExpression * rexpr = (To_Expr(rexprPointer));
	CExpression * binary = new CRealEquals(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createNormalNotEquals
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createNormalNotEquals
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(To_Expr(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(To_Expr(rexprPointer));
	CExpression * binary = new CNormalNotEquals(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createRealNotEquals
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createRealNotEquals
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CExpression * lexpr = (To_Expr(lexprPointer));
	CExpression * rexpr = (To_Expr(rexprPointer));
	CExpression * binary = new CRealNotEquals(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createNormalLessThan
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createNormalLessThan
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(To_Expr(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(To_Expr(rexprPointer));
	CExpression * binary = new CNormalLessThan(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createRealLessThan
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createRealLessThan
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CExpression * lexpr = (To_Expr(lexprPointer));
	CExpression * rexpr = (To_Expr(rexprPointer));
	CExpression * binary = new CRealLessThan(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createNormalGreaterThan
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createNormalGreaterThan
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(To_Expr(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(To_Expr(rexprPointer));
	CExpression * binary = new CNormalGreaterThan(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createRealGreaterThan
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createRealGreaterThan
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CExpression * lexpr = (To_Expr(lexprPointer));
	CExpression * rexpr = (To_Expr(rexprPointer));
	CExpression * binary = new CRealGreaterThan(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createNormalLessThanEqual
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createNormalLessThanEqual
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(To_Expr(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(To_Expr(rexprPointer));
	CExpression * binary = new CNormalLessThanEqual(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createRealLessThanEqual
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createRealLessThanEqual
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CExpression * lexpr = (To_Expr(lexprPointer));
	CExpression * rexpr = (To_Expr(rexprPointer));
	CExpression * binary = new CRealLessThanEqual(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createNormalGreaterThanEqual
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createNormalGreaterThanEqual
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CNormalExpression * lexpr = dynamic_cast<CNormalExpression*>(To_Expr(lexprPointer));
	CNormalExpression * rexpr = dynamic_cast<CNormalExpression*>(To_Expr(rexprPointer));
	CExpression * binary = new CNormalGreaterThanEqual(lexpr, rexpr);
	return To_JInt(binary);
}

/*
* Class:     simulator_SimulatorEngine
* Method:    createRealGreaterThanEqual
* Signature: (JJ)J
*/
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createRealGreaterThanEqual
(JNIEnv * env, jclass cls, jint lexprPointer, jint rexprPointer)
{
	CExpression * lexpr = (To_Expr(lexprPointer));
	CExpression * rexpr = (To_Expr(rexprPointer));
	CExpression * binary = new CRealGreaterThanEqual(lexpr, rexpr);
	return To_JInt(binary);
}

/*
 * Class:     simulator_SimulatorEngine
 * Method:    printExpression
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_simulator_SimulatorEngine_printExpression
  (JNIEnv * rnv, jclass clss, jint exprPointer)
{
	//std::cout << "trying to print expression" << endl;
	CExpression * expr = (To_Expr(exprPointer));
	std::string str = expr->To_String();

	//std::cout << str;
}

/*
 * Class:     simulator_SimulatorEngine
 * Method:    expressionToString
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_simulator_SimulatorEngine_expressionToString
  (JNIEnv * env, jclass cls, jint exprPointer)
{
	CExpression * expr = (To_Expr(exprPointer));
	std::string str = expr->To_String();

	return env->NewStringUTF(str.c_str());
	
}

/*
 * Class:     simulator_SimulatorEngine
 * Method:    deleteExpression
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_simulator_SimulatorEngine_deleteExpression
  (JNIEnv * env, jclass cls, jint exprPointer)
{
	CExpression * expr = (To_Expr(exprPointer));
	//std::cout << "should be deleting"<<endl;
	if(expr != NULL)
		delete expr;
}
