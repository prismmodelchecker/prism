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
//==============================================================================

#include <stdio.h>
#include "SimulatorEngine.h"
#include "simmodel.h"
#include "simexpression.h"


//Helper functions

inline CExpression * To_Expr(jint pointer) 
{
	return (CExpression*)(pointer);
}

inline jint To_JInt(CStateReward * sr) 
{
	return (jint)(sr);
}

inline jint To_JInt(CTransitionReward * tr)
{
	return (jint)(tr);
}


/*
 * Class:     simulator_SimulatorEngine
 * Method:    createStateReward
 * Signature: (JJ)J
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createStateReward
  (JNIEnv *env, jclass cls, jint guardPointer, jint rewardPointer)
{
	CExpression* guard = To_Expr(guardPointer);
	CExpression* reward = To_Expr(rewardPointer);

	CStateReward* state_reward = new CStateReward(guard, reward);

	return To_JInt(state_reward);
}

/*
 * Class:     simulator_SimulatorEngine
 * Method:    createTransitionReward
 * Signature: (IJJ)J
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createTransitionReward
  (JNIEnv *env, jclass cls, jint actionIndex, jint guardPointer, jint rewardPointer)
{
	CExpression* guard = To_Expr(guardPointer);
	CExpression* reward = To_Expr(rewardPointer);

	CTransitionReward* trans_reward = new CTransitionReward((int)actionIndex, guard, reward);

	return To_JInt(trans_reward);
}
