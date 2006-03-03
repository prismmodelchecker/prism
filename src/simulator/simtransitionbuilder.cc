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
#include "simexpression.h"
#include "simmodel.h"
#include "simstate.h"

//Helper functions

inline jint To_JInt(CExpression * expr) 
{
	return (jint)(expr);
}

inline CExpression * To_Expr(jint pointer) 
{
	return (CExpression*)(pointer);
}

inline CRealExpression * To_Real_Expr(jint pointer)
{
	return (CRealExpression*)(pointer);
}

inline jint Command_To_JInt(CCommand * comm)
{
	return (jint)(comm);
}

inline CCommand * To_Command(jint pointer)
{
	return (CCommand*)(pointer);
}

inline jint Update_To_JInt(CUpdate * up)
{
	return (jint)(up);
}

inline CUpdate * To_Update(jint pointer)
{
	return (CUpdate*)(pointer);
}

inline jint Assignment_To_JInt(CAssignment * assign)
{
	return (jint)(assign);
}


/*
 * Class:     simulator_SimulatorEngine
 * Method:    createCommand
 * Signature: (JII)J
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_createCommand
  (JNIEnv * env, jclass cls, jint guardPointer, jint actionIndex, jint moduleIndex, jint numUpdates)
{
	CNormalExpression * guard = dynamic_cast<CNormalExpression*>(To_Expr(guardPointer));

	CCommand * command = new CCommand(guard, (int)actionIndex, (int)moduleIndex, (int) numUpdates);

	return Command_To_JInt(command);
}

/*
 * Class:     simulator_SimulatorEngine
 * Method:    addUpdate
 * Signature: (JJI)J
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_addUpdate
  (JNIEnv *env, jclass cls, jint commandPointer, jint probPointer, jint numAssignments)
{
	//cout << "probPointer = " <<probPointer << endl;
	CRealExpression * prob = (To_Real_Expr(probPointer));
	//cout << "probPointerToStirng = " << prob->To_String() << endl;
	//if(prob == NULL) //cout << "When creating update, prob was null" <<endl;
	CCommand * command = (To_Command(commandPointer));

	CUpdate * update = new CUpdate(prob, (int) numAssignments);
	//cout << "probPointerToStirng after constructor= " << prob->To_String() << endl;
	command->Add_Update(update);
	
	return Update_To_JInt(update);
}

/*
 * Class:     simulator_SimulatorEngine
 * Method:    addAssignment
 * Signature: (JIJ)J
 */
JNIEXPORT jint JNICALL Java_simulator_SimulatorEngine_addAssignment
  (JNIEnv *env, jclass cls, jint updatePointer, jint varIndex, jint rhsPointer)
{
	CNormalExpression * rhs = dynamic_cast<CNormalExpression*>(To_Expr(rhsPointer));
	CUpdate * update = (To_Update(updatePointer));

	CAssignment* assign = new CAssignment(varIndex, rhs);
	//CAssignment * assign = new CAssignment(&state_variables[(int)varIndex],varIndex, rhs);

	update->Add_Assignment(assign);

	return Assignment_To_JInt(assign);
}
