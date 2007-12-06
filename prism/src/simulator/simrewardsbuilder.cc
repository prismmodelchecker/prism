//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

#include <stdio.h>
#include "SimulatorEngine.h"
#include "simmodel.h"
#include "simexpression.h"
#include "jnipointer.h"

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createStateReward
  (JNIEnv *env, jclass cls, jlong __pointer guardPointer, jlong __pointer rewardPointer)
{
	CExpression* guard = jlong_to_CExpression(guardPointer);
	CExpression* reward = jlong_to_CExpression(rewardPointer);
	CStateReward* state_reward = new CStateReward(guard, reward);
	return ptr_to_jlong(state_reward);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createTransitionReward
  (JNIEnv *env, jclass cls, jint actionIndex, jlong __pointer guardPointer, jlong __pointer rewardPointer)
{
	CExpression* guard = jlong_to_CExpression(guardPointer);
	CExpression* reward = jlong_to_CExpression(rewardPointer);
	CTransitionReward* trans_reward = new CTransitionReward((int)actionIndex, guard, reward);
	return ptr_to_jlong(trans_reward);
}
