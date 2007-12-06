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

#include "SimulatorEngine.h"
#include "simexpression.h"
#include "simmodel.h"
#include "simstate.h"
#include "jnipointer.h"

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_createCommand
  (JNIEnv * env, jclass cls, jlong __pointer guardPointer, jint actionIndex, jint moduleIndex, jint numUpdates)
{
	CNormalExpression * guard = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(guardPointer));
	CCommand * command = new CCommand(guard, (int)actionIndex, (int)moduleIndex, (int)numUpdates);
	return ptr_to_jlong(command);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_addUpdate
  (JNIEnv *env, jclass cls, jlong __pointer commandPointer, jlong __pointer probPointer, jint numAssignments)
{
	CRealExpression * prob = jlong_to_CRealExpression(probPointer);
	CCommand * command = jlong_to_CCommand(commandPointer);
	CUpdate * update = new CUpdate(prob, (int)numAssignments);
	command->Add_Update(update);
	return ptr_to_jlong(update);
}

JNIEXPORT jlong __pointer JNICALL Java_simulator_SimulatorEngine_addAssignment
  (JNIEnv *env, jclass cls, jlong __pointer updatePointer, jint varIndex, jlong __pointer rhsPointer)
{
	CNormalExpression * rhs = dynamic_cast<CNormalExpression*>(jlong_to_CExpression(rhsPointer));
	CUpdate * update = jlong_to_CUpdate(updatePointer);
	CAssignment* assign = new CAssignment(varIndex, rhs);
	update->Add_Assignment(assign);
	return ptr_to_jlong(assign);
}
